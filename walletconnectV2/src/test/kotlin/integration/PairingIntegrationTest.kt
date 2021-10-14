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
        "wc:ffdab82f201d6c02be6c8b79184878154811351b6af0544cc7f57d61b1a85868@2?controller=false&publicKey=d1a7dfa074f8516a88d3e4f2280a4b150066c0ee37ff10689b16827e00f90304&relay=%7B%22protocol%22%3A%22waku%22%7D"

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

//                val subscribeDeferred = async(Dispatchers.IO) {
//                    engine.subscribeAcknowledgement.collect {
//                        println("Subscribe Acknowledgement $it")
//                        require(it.result.id.isNotBlank()) {
//                            "Acknowledgement from Relay returned false"
//                        }
//                    }
//                }
//
//                val subscriptionDeferred = async(Dispatchers.IO) {
//                    engine.subscriptionRequest.collect {
//                        println("Subscription Request $it")
//                    }
//                }

                listOf(pairDeferred, /*subscribeDeferred, subscriptionDeferred*/).awaitAll()
            }
        } catch (timeoutException: TimeoutCancellationException) {
            println("timed out")
            exitProcess(0)
        }
    }
}