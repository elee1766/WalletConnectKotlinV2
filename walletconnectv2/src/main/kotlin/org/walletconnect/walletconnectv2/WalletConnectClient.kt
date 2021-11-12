package org.walletconnect.walletconnectv2

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.walletconnect.walletconnectv2.client.ClientTypes
import org.walletconnect.walletconnectv2.client.SettledSession
import org.walletconnect.walletconnectv2.client.WalletConnectClientListener
import org.walletconnect.walletconnectv2.client.WalletConnectClientListeners
import org.walletconnect.walletconnectv2.engine.EngineInteractor
import org.walletconnect.walletconnectv2.engine.sequence.*
import timber.log.Timber

object WalletConnectClient {
    private val engineInteractor = EngineInteractor()
    private var walletConnectListener: WalletConnectClientListener? = null
    private var pairingListener: WalletConnectClientListeners.Pairing? = null
    private var sessionApproveListener: WalletConnectClientListeners.SessionApprove? = null
    private var sessionRejectListener: WalletConnectClientListeners.SessionReject? = null
    private var sessionDeleteListener: WalletConnectClientListeners.SessionDelete? = null

    init {
        Timber.plant(Timber.DebugTree())

        scope.launch {
            engineInteractor.sequenceEvent.collect { event ->
                when (event) {
                    is OnSessionProposal -> walletConnectListener?.onSessionProposal(event.proposal)
                    is OnSessionRequest -> walletConnectListener?.onSessionRequest(event.request)
                    is OnSessionDeleted -> walletConnectListener?.onSessionDelete(event.topic, event.reason)
                    else -> Unsupported
                }
            }
        }
    }

    fun initialize(initialParams: ClientTypes.InitialParams) = with(initialParams) {
        // TODO: pass properties to DI framework
        val engineFactory = EngineInteractor.EngineFactory(useTls, hostName, apiKey, isController, application, metadata)
        engineInteractor.initialize(engineFactory)
    }

    fun setWalletConnectListener(walletConnectListener: WalletConnectClientListener) {
        this.walletConnectListener = walletConnectListener
    }

    fun pair(
        pairingParams: ClientTypes.PairParams,
        listener: WalletConnectClientListeners.Pairing
    ) {
        pairingListener = listener
        engineInteractor.pair(pairingParams.uri) { result ->
            result.fold(
                onSuccess = { topic -> listener.onSuccess(topic as String) },
                onFailure = { error -> listener.onError(error) }
            )
        }
    }

    fun approve(
        approveParams: ClientTypes.ApproveParams,
        listener: WalletConnectClientListeners.SessionApprove
    ) = with(approveParams) {
        sessionApproveListener = listener
        engineInteractor.approve(proposal, accounts) { result ->
            result.fold(
                onSuccess = { settledSession -> listener.onSuccess(settledSession as SettledSession) },
                onFailure = { error -> listener.onError(error) }
            )
        }
    }

    fun reject(
        rejectParams: ClientTypes.RejectParams,
        listener: WalletConnectClientListeners.SessionReject
    ) = with(rejectParams) {
        sessionRejectListener = listener
        engineInteractor.reject(rejectionReason, proposalTopic) { result ->
            result.fold(
                onSuccess = { topic -> listener.onSuccess(topic as String) },
                onFailure = { error -> listener.onError(error) }
            )
        }
    }

    fun disconnect(
        disconnectParams: ClientTypes.DisconnectParams,
        listener: WalletConnectClientListeners.SessionDelete
    ) = with(disconnectParams) {
        sessionDeleteListener = listener
        engineInteractor.disconnect(topic, reason) { result ->
            result.fold(
                onSuccess = { topic -> listener.onSuccess(topic as String) },
                onFailure = { error -> listener.onError(error) }
            )
        }
    }
}