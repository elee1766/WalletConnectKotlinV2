package org.walletconnect.walletconnectv2

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.walletconnect.walletconnectv2.engine.EngineInteractor
import org.walletconnect.walletconnectv2.outofband.client.ClientTypes

object WalletConnectClient {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    private lateinit var engineInteractor: EngineInteractor

    fun initialize(initialParams: ClientTypes.InitialParams) {
        // TODO: pass properties to DI framework
        engineInteractor = EngineInteractor(hostName = initialParams.hostName)
    }

    fun pair(
        pairingParams: ClientTypes.PairParams,
        clientListeners: WalletConnectClientListeners.Session
    ) {
        require(this::engineInteractor.isInitialized) {
            "Initialize must be called prior to pairing"
        }

        scope.launch {
            engineInteractor.pair(pairingParams.uri)
        }
    }

    fun approve() {
        //todo add logic for approving session proposal
    }
}