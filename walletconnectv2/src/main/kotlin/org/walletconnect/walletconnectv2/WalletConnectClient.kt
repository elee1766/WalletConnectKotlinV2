package org.walletconnect.walletconnectv2

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.walletconnect.walletconnectv2.client.ClientTypes
import org.walletconnect.walletconnectv2.client.WalletConnectClientData
import org.walletconnect.walletconnectv2.client.WalletConnectClientListener
import org.walletconnect.walletconnectv2.client.WalletConnectClientListeners
import org.walletconnect.walletconnectv2.common.toClientSessionProposal
import org.walletconnect.walletconnectv2.common.toClientSessionRequest
import org.walletconnect.walletconnectv2.common.toClientSettledSession
import org.walletconnect.walletconnectv2.common.toEngineSessionProposal
import org.walletconnect.walletconnectv2.engine.EngineInteractor
import org.walletconnect.walletconnectv2.engine.model.EngineData
import org.walletconnect.walletconnectv2.engine.sequence.*
import timber.log.Timber

object WalletConnectClient {
    private val engineInteractor = EngineInteractor()
    private var walletConnectListener: WalletConnectClientListener? = null

    init {
        Timber.plant(Timber.DebugTree())

        scope.launch {
            engineInteractor.sequenceEvent.collect { event ->
                when (event) {
                    is SequenceLifecycleEvent.OnSessionProposal -> walletConnectListener?.onSessionProposal(event.proposal.toClientSessionProposal())
                    is SequenceLifecycleEvent.OnSessionRequest -> walletConnectListener?.onSessionRequest(event.request.toClientSessionRequest())
                    is SequenceLifecycleEvent.OnSessionDeleted -> walletConnectListener?.onSessionDelete(event.topic, event.reason)
                    else -> SequenceLifecycleEvent.Unsupported
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
        engineInteractor.pair(pairingParams.uri) { result ->
            result.fold(
                onSuccess = { topic -> listener.onSuccess(WalletConnectClientData.SettledPairing(topic)) },
                onFailure = { error -> listener.onError(error) }
            )
        }
    }

    fun approve(
        approveParams: ClientTypes.ApproveParams,
        listener: WalletConnectClientListeners.SessionApprove
    ) = with(approveParams) {
        engineInteractor.approve(proposal.toEngineSessionProposal(), accounts) { result ->
            result.fold(
                onSuccess = { settledSession -> listener.onSuccess(settledSession.toClientSettledSession()) },
                onFailure = { error -> listener.onError(error) }
            )
        }
    }

    fun reject(
        rejectParams: ClientTypes.RejectParams,
        listener: WalletConnectClientListeners.SessionReject
    ) = with(rejectParams) {
        engineInteractor.reject(rejectionReason, proposalTopic) { result ->
            result.fold(
                onSuccess = { topic -> listener.onSuccess(WalletConnectClientData.RejectedSession(topic)) },
                onFailure = { error -> listener.onError(error) }
            )
        }
    }

    fun disconnect(
        disconnectParams: ClientTypes.DisconnectParams,
        listener: WalletConnectClientListeners.SessionDelete
    ) = with(disconnectParams) {
        engineInteractor.disconnect(topic, reason) { result ->
            result.fold(
                onSuccess = { topic -> listener.onSuccess(WalletConnectClientData.DeletedSession(topic)) },
                onFailure = { error -> listener.onError(error) }
            )
        }
    }
}