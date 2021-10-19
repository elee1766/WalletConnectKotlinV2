package org.walletconnect.example.wallet

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.walletconnect.example.R
import org.walletconnect.example.wallet.ui.*
import org.walletconnect.walletconnectv2.WalletConnectClient
import org.walletconnect.walletconnectv2.client.ClientTypes
import org.walletconnect.walletconnectv2.client.SessionProposal
import org.walletconnect.walletconnectv2.client.WalletConnectClientListeners

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                WalletConnectClient.publishAcknowledgement.collect {
                    Log.e("Talha", "$it")
                }
                WalletConnectClient.subscribeAcknowledgement.collect {
                    Log.e("Talha", "$it")
                }
                WalletConnectClient.subscriptionRequest.collect {
                    Log.e("Talha", "$it")
                }
                WalletConnectClient.sessionProposalFlow.collect {
                    Log.e("Talha", "$it")
                }
            }
        }
    }

    private var _eventFlow = MutableSharedFlow<WalletUiEvent>()
    val eventFlow = _eventFlow.asLiveData()

    val activeSessions: MutableList<Session> = mutableListOf()

    fun pair(uri: String) {
// Call pair method from SDK and setup callback for session proposal event. Once it's received show session proposal dialog
        val sessionProposalListener = WalletConnectClientListeners.Pairing { sessionProposal ->
            viewModelScope.launch {
                _eventFlow.emit(ShowSessionProposalDialog(sessionProposal))
            }
        }
        val pairingParams = ClientTypes.PairParams(uri = uri)

        WalletConnectClient.pair(pairingParams, sessionProposalListener)
    }

    fun approve() {
        val session = Session(
            name = "WalletConnect",
            uri = "app.walletconnect.org",
            icon = R.drawable.ic_walletconnect_circle_blue
        )

        activeSessions += session

//        Call approve method from SDK to approve session proposal
//        WalletConnectClient.approve()
        viewModelScope.launch {
            _eventFlow.emit(UpdateActiveSessions(activeSessions))
        }
    }

    fun reject() {
        //call reject() session method from SDK
    }
}