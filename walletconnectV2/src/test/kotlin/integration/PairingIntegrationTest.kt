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
        "wc:5f238dc528276c841dff3540ebb6867a6e906229222865a8f01f506b576c5bdd@2?controller=false&publicKey=731812baf13cb27319e0d34184dab4ab1ce2fbcafc20bcd452ab726cdeb5ef41&relay=%7B%22protocol%22%3A%22waku%22%7D"

    scope.launch {

        engine.pair(uri)

        try {
            withTimeout(Duration.ofMinutes(2).toMillis()) {
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