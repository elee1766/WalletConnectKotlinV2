package integration

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.walletconnect.walletconnectv2.engine.EngineInteractor
import java.time.Duration

fun main() {
    val job = SupervisorJob()
    val scope = CoroutineScope(job + Dispatchers.IO)
    val engine = EngineInteractor("staging.walletconnect.org?apiKey=c4f79cc821944d9680842e34466bfbd")
    val uri =
        "wc:f06ab27e32969e22d8d2ee912e983b17d8f2f5bff8a31ca2dd301d3c370fd046@2?controller=false&publicKey=577b7214079fb7253973e48a59285103ff22e7916c2a6b80fc7abc417bb65401&relay=%7B%22protocol%22%3A%22waku%22%7D"

    scope.launch {
        engine.pair(uri)

        try {
            withTimeout(Duration.ofSeconds(180).toMillis()) {
                engine.pairingAcknowledgement.collect {
                    println("Publish Acknowledgement: $it")
                    require(it.result) {
                        "Acknowledgement from Relay returned false"
                    }
                }

                engine.subscribeAcknowledgement.collect {
                    println("Subscribe Acknowledgement $it")
                    require(it.result.id.isNotBlank()) {
                        "Acknowledgement from Relay returned false"
                    }
                }
            }
        } catch (timeoutException: TimeoutCancellationException) {
            println("timed out")
        }
    }
}