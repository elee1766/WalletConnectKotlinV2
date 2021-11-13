package org.walletconnect.example.wallet

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.walletconnect.example.wallet.ui.*
import org.walletconnect.walletconnectv2.WalletConnectClient
import org.walletconnect.walletconnectv2.client.*

class WalletViewModel : ViewModel(), WalletConnectClientListener {
    private var _eventFlow = MutableSharedFlow<WalletUiEvent>()
    val eventFlow = _eventFlow.asLiveData()

    val settledSessions: MutableList<WalletConnectClientData.SettledSession> = mutableListOf()
    lateinit var proposal: WalletConnectClientData.SessionProposal

    init {
        WalletConnectClient.setWalletConnectListener(this)
    }

    fun pair(uri: String) {
        val pairParams = ClientTypes.PairParams(uri.trim())
        WalletConnectClient.pair(pairParams, object : WalletConnectClientListeners.Pairing {
            override fun onSuccess(topic: String) {
                //Settled pairing
            }

            override fun onError(error: Throwable) {
                //Pairing approval error
            }
        })
    }

    fun approve() {
        val accounts = proposal.chains.map { chainId -> "$chainId:0x022c0c42a80bd19EA4cF0F94c4F9F96645759716" }
        val approveParams: ClientTypes.ApproveParams = ClientTypes.ApproveParams(proposal, accounts)

        WalletConnectClient.approve(approveParams, object : WalletConnectClientListeners.SessionApprove {
            override fun onSuccess(session: SettledSession) {
                settledSessions += session
                viewModelScope.launch { _eventFlow.emit(UpdateActiveSessions(settledSessions)) }
            }

            override fun onError(error: Throwable) {
                //Approve session error
            }
        })
    }

    fun reject() {
        val rejectionReason = "Reject Session"
        val proposalTopic: String = proposal.topic
        val rejectParams: ClientTypes.RejectParams = ClientTypes.RejectParams(rejectionReason, proposalTopic)
        WalletConnectClient.reject(rejectParams, object : WalletConnectClientListeners.SessionReject {
            override fun onSuccess(topic: String) {
                viewModelScope.launch { _eventFlow.emit(RejectSession) }
            }

            override fun onError(error: Throwable) {
                //Proposal rejection error
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun disconnect(topic: String, reason: String = "Reason") {
        val disconnectParams = ClientTypes.DisconnectParams(topic, reason)
        WalletConnectClient.disconnect(disconnectParams, object : WalletConnectClientListeners.SessionDelete {
            override fun onSuccess(topic: String) {
                settledSessions.removeIf { session -> session.topic == topic }
                viewModelScope.launch { _eventFlow.emit(UpdateActiveSessions(settledSessions)) }
            }

            override fun onError(error: Throwable) {
                //Session disconnect error
            }
        })
    }

    override fun onSessionProposal(proposal: SessionProposal) {
        viewModelScope.launch {
            this@WalletViewModel.proposal = proposal
            _eventFlow.emit(ShowSessionProposalDialog(this@WalletViewModel.proposal))
        }
    }

    override fun onSessionRequest(request: SessionRequest) {
        //TODO handle session request generic object
    }


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onSessionDelete(topic: String, reason: String) {
        settledSessions.removeIf { session -> session.topic == topic }
        viewModelScope.launch { _eventFlow.emit(UpdateActiveSessions(settledSessions)) }
    }

    private fun removeSession(topic: String) {
        settledSessions.find { session -> session.topic == topic }?.also { session -> settledSessions.remove(session) }
    }
}