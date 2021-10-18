package integration

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.walletconnect.walletconnectv2.engine.EngineInteractor
import java.time.Duration
import kotlin.system.exitProcess

fun main() {
    val job = SupervisorJob()
    val scope = CoroutineScope(job + Dispatchers.IO)
    val engine = EngineInteractor(true, "relay.walletconnect.org?apiKey=c4f79cc821944d9680842e34466bfbd")
    val uri =
        "wc:34a765f699c7b3afd0a7c556565639a78856aa3f77c6bb799fb4cbdb8db46f14@2?controller=false&publicKey=7ea3d505828072cab81523075a942787464ca7183a5069ad0fc1a6e2c941d662&relay=%7B%22protocol%22%3A%22waku%22%7D"

    scope.launch {
        engine.pair(uri)

        try {
            withTimeout(Duration.ofMinutes(20).toMillis()) {
                val pairDeferred = async(Dispatchers.IO) {
                    engine.pairingAcknowledgement.collect {
                        println("Publish Acknowledgement: $it")
                        require(it.result) {
                            "Acknowledgement from Relay returned false"
                        }
                    }
                }

                val subscribeDeferred = async(Dispatchers.IO) {
                    engine.subscribeAcknowledgement.collect {
                        println("Subscribe Acknowledgement $it")
                        require(it.result.id.isNotBlank()) {
                            "Acknowledgement from Relay returned false"
                        }
                    }
                }

                val subscriptionDeferred = async(Dispatchers.IO) {
                    engine.subscriptionRequest.collect {
                        println("Subscription Request $it")
                    }
                }

                listOf(pairDeferred, subscribeDeferred, subscriptionDeferred).awaitAll()
            }
        } catch (timeoutException: TimeoutCancellationException) {
            println("timed out")
            exitProcess(0)
        }
    }
}