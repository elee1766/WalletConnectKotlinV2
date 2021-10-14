package integration

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.json.JSONObject
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.common.Ttl
import org.walletconnect.walletconnectv2.engine.EngineInteractor
import org.walletconnect.walletconnectv2.outofband.pairing.proposal.JsonRPC
import org.walletconnect.walletconnectv2.pubsub.RelayProtocolOptions
import org.walletconnect.walletconnectv2.pubsub.Session
import org.walletconnect.walletconnectv2.pubsub.proposal.*
import java.time.Duration
import kotlin.system.exitProcess

fun main() {
//    pairTest()
    approveSessionTest()
}

fun approveSessionTest() {
    val job = SupervisorJob()
    val scope = CoroutineScope(job + Dispatchers.IO)
    val engine =
        EngineInteractor(true, "relay.walletconnect.org")//?apiKey=c4f79cc821944d9680842e34466bfbd")

    engine.approve(
        Session.SessionProposal(
            topic = Topic("cf62edd89acd1d08639dd5f094182a1f84b63d072ada79042c7c8016edb80db4"),
            relay = RelayProtocolOptions(protocol = "waku"),
            proposer = SessionProposer(
                publicKey = "30e0f1d9d3ea5f3b60ee7d654dafdafcc3254d9acc01b8714d87a4138bd57a3e",
                controller = false
            ),
            signal = SessionSignal(params = SessionSignalParams(topic = "df3e0cbcd8171102fd331b8c11cfe2c80d7db003800b6b01e58eab0b00155a81")),
            permissions = SessionProposedPermissions(
                blockchain = Blockchain(chains = listOf("eip155:42")),
                jsonRPC = JsonRPC(
                    methods = listOf(
                        "eth_sendTransaction",
                        "personal_sign",
                        "eth_signTypedData"
                    )
                ),
                notifications = Notifications(types = listOf())
            ),
            ttl = Ttl(604800)
        )
    )

    scope.launch {
        try {

            withTimeout(Duration.ofMinutes(1).toMillis()) {

                val pairDeferred = async(Dispatchers.IO) {
                    engine.publishAcknowledgement.collect {
                        println("Publish Acknowledgement: $it")
                        require(it.result) {
                            "Acknowledgement from Relay returned false"
                        }
                    }
                }

                pairDeferred.await()
            }
        } catch (timeoutException: TimeoutCancellationException) {
            println("timed out")
            exitProcess(0)
        }
    }
}

private fun pairTest() {
    val job = SupervisorJob()
    val scope = CoroutineScope(job + Dispatchers.IO)
    val engine =
        EngineInteractor(
            true,
            "relay.walletconnect.org"
        )//?apiKey=c4f79cc821944d9680842e34466bfbd")
    val uri =
        "wc:fa96be34a0e2f0076adb50cae5aa382d8a44f378f32b0438780efac09699dfe3@2?controller=false&publicKey=c3d726889e9c41e8cd9e32c633fe1ece34dda83c3077a5d59edef246bd5c4948&relay=%7B%22protocol%22%3A%22waku%22%7D"

    scope.launch {
        engine.pair(uri)

        try {
            withTimeout(Duration.ofMinutes(2).toMillis()) {

                val pairDeferred = async(Dispatchers.IO) {
                    engine.publishAcknowledgement.collect {
                        println("Publish Acknowledgement: $it")
                        require(it.result) {
                            "Acknowledgement from Relay returned false"
                        }
                    }
                }


                val subDeferred = async(Dispatchers.IO) {
                    engine.subscribeAcknowledgement.collect {
                        println("Subscribe Acknowledgement $it\n")
                        require(it.result.id.isNotBlank()) {
                            "Acknowledgement from Relay returned false"
                        }
                    }
                }

//                println("Session proposal")
//
//                engine.subscriptionRequest.collect {
//                    println("Session proposal response $it")
//
//                    require(it.params.data.message.isNotBlank()) {
//                        "Acknowledgement from Relay returned false"
//                    }
//                }

                listOf(pairDeferred, subDeferred).awaitAll()
            }
        } catch (timeoutException: TimeoutCancellationException) {
            println("timed out")
            exitProcess(0)
        }
    }
}