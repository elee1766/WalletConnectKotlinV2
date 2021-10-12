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
        "wc:035fe1a85e3dac41539b8ef2b2916c9da0b8abf78cb318890071b1e2ae0b0e75@2?controller=false&publicKey=b618235db1adf42990bf3917ae6326fb5ab255f335e670610a78456f00fa225e&relay=%7B%22protocol%22%3A%22waku%22%7D"

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

                val subDeferred = async(Dispatchers.IO) {
                    engine.subscribeAcknowledgement.collect {
                        println("Subscribe Acknowledgement $it")
                        require(it.result.id.isNotBlank()) {
                            "Acknowledgement from Relay returned false"
                        }

//                        exitProcess(0)
                    }
                }

                listOf(pairDeferred, subDeferred).awaitAll()
            }
        } catch (timeoutException: TimeoutCancellationException) {
            println("timed out")
            exitProcess(0)
        }
    }
}