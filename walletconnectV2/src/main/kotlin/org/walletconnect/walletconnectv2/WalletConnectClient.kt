package org.walletconnect.walletconnectv2

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.walletconnect.walletconnectv2.client.ClientTypes
import org.walletconnect.walletconnectv2.client.SessionProposal
import org.walletconnect.walletconnectv2.client.WalletConnectClientListeners
import org.walletconnect.walletconnectv2.clientcomm.session.Session
import org.walletconnect.walletconnectv2.engine.EngineInteractor
import java.net.URI

object WalletConnectClient {
    private val job = SupervisorJob()
    internal val scope = CoroutineScope(job + Dispatchers.IO)
    private val engineInteractor = EngineInteractor()

    // Listeners
    private var pairingListener: WalletConnectClientListeners.Pairing? = null

    init {
        scope.launch {
            engineInteractor.sessionProposal.collect {
                Log.e("TalhaWalletClient", "$it")
                it?.toSessionProposal()?.let { sessionProposal ->
                    pairingListener?.onSessionProposal(sessionProposal)
                }
            }
        }
    }

    fun initialize(initialParams: ClientTypes.InitialParams) {
        // TODO: pass properties to DI framework
        val engineFactory = EngineInteractor.EngineFactory(useTLs = initialParams.useTls, hostName = initialParams.hostName)
        engineInteractor.initialize(engineFactory)
    }

    fun pair(pairingParams: ClientTypes.PairParams, clientListeners: WalletConnectClientListeners.Pairing) {
        pairingListener = clientListeners

        scope.launch {
//            engineInteractor.pair("wc:c5885ce5596212bb58e9efc3f3e4aaa68df8d2016482f456823fdcb16bb06adf@2?controller=false&publicKey=84d4287be804381c9ac34713d93bb68123c913d7bca6a489d61e9a0d849c276b&relay=%7B%22protocol%22%3A%22waku%22%7D"/*pairingParams.uri*/)
            engineInteractor.pair(pairingParams.uri)
        }
    }

    fun approve(proposal: Session.Proposal) {
        engineInteractor.approve(proposal)
    }

    private fun Session.Proposal.toSessionProposal(): SessionProposal {
        return SessionProposal(
            name = this.proposer.metadata?.name!!,
            description = this.proposer.metadata.description,
            dappUrl = this.proposer.metadata.url,
            icon = this.proposer.metadata.icons.map { URI(it) },
            chains = this.permissions.blockchain.chains,
            methods = this.permissions.jsonRpc.methods
        )
    }

}