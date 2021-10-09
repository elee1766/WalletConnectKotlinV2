package org.walletconnect.walletconnectv2

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.walletconnect.walletconnectv2.engine.EngineInteractor
import org.walletconnect.walletconnectv2.outofband.client.ClientTypes

object WalletConnectClient {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    private lateinit var pairingEngine: EngineInteractor

    val publishResponse = pairingEngine.pairingResponse.shareIn(scope, SharingStarted.Lazily)

    fun initialize(initialParams: ClientTypes.InitialParams) {
        // TODO: pass properties to DI framework
        pairingEngine = EngineInteractor(initialParams)
    }

    fun pair(
        pairingParams: ClientTypes.PairParams,
        clientListeners: WalletConnectClientListeners.Session
    ) {

        require(this::pairingEngine.isInitialized) {
            "Initialize must be called prior to pairing"
        }

        scope.launch {
            pairingEngine.pair(pairingParams.uri)
        }
    }

    fun approve() {
        //todo add logic for approving session proposal
    }
}