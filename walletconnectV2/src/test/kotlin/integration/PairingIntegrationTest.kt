package integration

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.walletconnect.walletconnectv2.engine.EngineInteractor
import java.time.Duration
import kotlin.system.exitProcess

fun main() {
    val job = SupervisorJob()
    val scope = CoroutineScope(job + Dispatchers.IO)
    val engine = EngineInteractor(true, "relay.walletconnect.org")
    val uri =
        "wc:e34e484c50e52a7d89fceb35640961aec93fecaee7981e7cd884a5343a6ce6ac@2?controller=false&publicKey=4929b4e600efa3fe9b78fade9a86a2d2e50200e668dd3cb32a66605ffe071d3b&relay=%7B%22protocol%22%3A%22waku%22%7D"
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