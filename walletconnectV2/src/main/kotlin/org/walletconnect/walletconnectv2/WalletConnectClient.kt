package org.walletconnect.walletconnectv2

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.walletconnect.walletconnectv2.client.ClientTypes
import org.walletconnect.walletconnectv2.client.SessionProposal
import org.walletconnect.walletconnectv2.client.WalletConnectClientListeners
import org.walletconnect.walletconnectv2.clientcomm.session.Session
import org.walletconnect.walletconnectv2.engine.EngineInteractor
import java.net.URI

object WalletConnectClient {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    private lateinit var engineInteractor: EngineInteractor
    val publishAcknowledgement by lazy { engineInteractor.publishAcknowledgement }
    val subscribeAcknowledgement by lazy { engineInteractor.subscribeAcknowledgement }
    val sessionProposalFlow by lazy { engineInteractor.sessionProposal }
    val subscriptionRequest by lazy { engineInteractor.subscriptionRequest }

    // Listeners
    internal var pairingListener: WalletConnectClientListeners.Pairing? = null

    fun initialize(initialParams: ClientTypes.InitialParams) {
        // TODO: pass properties to DI framework
        engineInteractor = EngineInteractor(hostName = initialParams.hostName, scope = scope)
    }

    fun pair(
        pairingParams: ClientTypes.PairParams,
        clientListeners: WalletConnectClientListeners.Pairing
    ) {
        require(this::engineInteractor.isInitialized) {
            "Initialize must be called prior to pairing"
        }

        pairingListener = clientListeners

        engineInteractor.pair(pairingParams.uri)

        scope.launch {
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