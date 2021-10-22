package org.walletconnect.example.wallet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.walletconnect.example.R
import org.walletconnect.example.wallet.ui.Session
import org.walletconnect.example.wallet.ui.ShowSessionProposalDialog
import org.walletconnect.example.wallet.ui.UpdateActiveSessions
import org.walletconnect.example.wallet.ui.WalletUiEvent
import org.walletconnect.walletconnectv2.WalletConnectClient
import org.walletconnect.walletconnectv2.client.ClientTypes

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private var _eventFlow = MutableSharedFlow<WalletUiEvent>()
    val eventFlow = _eventFlow.asLiveData()

    val activeSessions: MutableList<Session> = mutableListOf()

    init {
        val uri =
            "wc:9266894a11842ddba9912b540e6c90e2dfdc8f294bd2e322504e9de4961aaf4e@2?controller=false&publicKey=8039e2f65c80fc5a3525f80488d411efbff3b3280cd4187719e347efe7ea085a&relay=%7B%22protocol%22%3A%22waku%22%7D"

        viewModelScope.launch {
            WalletConnectClient.pair(ClientTypes.PairParams(uri)) { sessionProposal ->
                viewModelScope.launch {
                    _eventFlow.emit(ShowSessionProposalDialog(sessionProposal))
                }
            }
        }
    }

    fun pair(uri: String) {
//        // Call pair method from SDK and setup callback for session proposal event. Once it's received show session proposal dialog
//        viewModelScope.launch {
//            WalletConnectClient.pair(ClientTypes.PairParams(uri)) {
//                Log.e("SessionProposal", "Session Proposal: $it")
//            }
//        }
    }

    fun approve() {
        val session = Session(name = "WalletConnect", uri = "app.walletconnect.org", icon = R.drawable.ic_walletconnect_circle_blue)

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