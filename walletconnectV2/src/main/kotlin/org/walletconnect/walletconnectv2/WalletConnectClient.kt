package org.walletconnect.walletconnectv2

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.walletconnect.walletconnectv2.client.ClientTypes
import org.walletconnect.walletconnectv2.clientcomm.session.Session
import org.walletconnect.walletconnectv2.engine.EngineInteractor

object WalletConnectClient {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    private lateinit var engineInteractor: EngineInteractor

    fun initialize(initialParams: ClientTypes.InitialParams) {
        // TODO: pass properties to DI framework
        engineInteractor =
            EngineInteractor(useTLs = initialParams.useTls, hostName = initialParams.hostName)
    }

    fun pair(pairingParams: ClientTypes.PairParams) {

        require(this::engineInteractor.isInitialized) {
            "Initialize must be called prior to pairing"
        }

        println("KOBE PAIRING: ${pairingParams.uri}")

        scope.launch {
            engineInteractor.pair(pairingParams.uri)
        }
    }

    fun approve(proposal: Session.Proposal) {
//       engineInteractor.approve(proposal)
    }
}