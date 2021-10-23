package org.walletconnect.example.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.walletconnect.example.wallet.ui.Session
import org.walletconnect.example.wallet.ui.ShowSessionProposalDialog
import org.walletconnect.example.wallet.ui.UpdateActiveSessions
import org.walletconnect.example.wallet.ui.WalletUiEvent
import org.walletconnect.walletconnectv2.WalletConnectClient
import org.walletconnect.walletconnectv2.client.ClientTypes
import org.walletconnect.walletconnectv2.client.SessionProposal

class WalletViewModel : ViewModel() {

    private var _eventFlow = MutableSharedFlow<WalletUiEvent>()
    val eventFlow = _eventFlow.asLiveData()

    val activeSessions: MutableList<Session> = mutableListOf()
    lateinit var sessionProposal: SessionProposal

    init {
        val uri =
            "wc:79ddf2dd74e1e3cd0717bc9239e04ef6eb61ee60dc863c8feee99e5b6a195327@2?controller=false&publicKey=19bc598e3d972fb72cd61051d0ddd526746d998deb308f7139eefdf97922f570&relay=%7B%22protocol%22%3A%22waku%22%7D"

        viewModelScope.launch {
            WalletConnectClient.pair(ClientTypes.PairParams(uri)) { sessionProposal ->
                viewModelScope.launch {
                    this@WalletViewModel.sessionProposal = sessionProposal
                    _eventFlow.emit(ShowSessionProposalDialog(sessionProposal))
                }
            }
        }
    }

//    fun pair(uri: String) {
//        val pairParams = ClientTypes.PairParams(uri)
//        viewModelScope.launch {
//            WalletConnectClient.pair(pairParams) { sessionProposal ->
//                viewModelScope.launch {
//                    this@WalletViewModel.sessionProposal = sessionProposal
//                    _eventFlow.emit(ShowSessionProposalDialog(sessionProposal))
//                }
//            }
//        }
//    }

    fun approve() {
        val session = Session(
            name = sessionProposal.name,
            uri = sessionProposal.dappUrl,
            icon = sessionProposal.icon.first().toString()
        )

        activeSessions += session
        WalletConnectClient.approve(sessionProposal.copy(accounts = listOf("eip155:137:0x022c0c42a80bd19EA4cF0F94c4F9F96645759716")))
        viewModelScope.launch {
            _eventFlow.emit(UpdateActiveSessions(activeSessions))
        }
    }

    fun reject() {
        //call reject() session method from SDK
    }
}