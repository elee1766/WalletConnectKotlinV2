package integration

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.walletconnect.walletconnectv2.clientcomm.session.RelayProtocolOptions
import org.walletconnect.walletconnectv2.clientcomm.session.Session
import org.walletconnect.walletconnectv2.clientcomm.session.proposal.SessionProposedPermissions
import org.walletconnect.walletconnectv2.clientcomm.session.proposal.SessionProposer
import org.walletconnect.walletconnectv2.clientcomm.session.proposal.SessionSignal
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.common.Ttl
import org.walletconnect.walletconnectv2.engine.EngineInteractor
import java.time.Duration
import kotlin.system.exitProcess

fun main() {
    pairTest()
//    approveSessionTest()
}

private fun pairTest() {
    val job = SupervisorJob()
    val scope = CoroutineScope(job + Dispatchers.IO)
    val engine = EngineInteractor(true, "relay.walletconnect.org")
    val uri =
        "wc:cec102a095a66facbfc9c154aa33794ab98d7cbfa6e1fcd4fe228ff94d11f851@2?controller=false&publicKey=619fc53f941ce95f3f20d361710530aaabd6e78d7999587401df90c902ac4107&relay=%7B%22protocol%22%3A%22waku%22%7D"
    scope.launch {
        engine.pair(uri)

        try {
            withTimeout(Duration.ofMinutes(20).toMillis()) {
                val pairDeferred = async(Dispatchers.IO) {
                    engine.publishAcknowledgement.collect {
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

fun approveSessionTest() {
    val job = SupervisorJob()
    val scope = CoroutineScope(job + Dispatchers.IO)
    val engine = EngineInteractor(true, "relay.walletconnect.org")

    engine.approve(
        Session.Proposal(
            topic = Topic("69bba8737e4c7d8715b0ea92fe044ba291a359c24a3cde3e240bc8ec81fa0757"),
            relay = RelayProtocolOptions(protocol = "waku"),
            proposer = SessionProposer(
                publicKey = "fa75874568a6f347229c5936f34ac7e2117f5233e13e3e418332687acd56382c",
                controller = false, null
            ),
            signal = SessionSignal(params = SessionSignal.Params(topic = Topic("15a66762d0a589a2330c73a627a8b83668f3b1eb7f172da1bedf045e09108aec"))),
            permissions = SessionProposedPermissions(
                blockchain = SessionProposedPermissions.Blockchain(chains = listOf("eip155:42")),
                jsonRpc = SessionProposedPermissions.JsonRpc(
                    methods = listOf(
                        "eth_sendTransaction",
                        "personal_sign",
                        "eth_signTypedData"
                    )
                ),
                notifications = SessionProposedPermissions.Notifications(types = listOf())
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