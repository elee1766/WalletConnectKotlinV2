package org.walletconnect.example.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.walletconnect.example.R
import org.walletconnect.example.wallet.ui.*
import org.walletconnect.walletconnectv2.WalletConnectClient
import org.walletconnect.walletconnectv2.client.ClientTypes

class WalletViewModel : ViewModel() {

    init {
        val initParams = ClientTypes.InitialParams(useTls = true, hostName = "relay.walletconnect.org", apiKey = "", isController = true)
        WalletConnectClient.initialize(initParams)

        val pairingParams = ClientTypes.PairParams(uri = "wc:49ea28913d569e5dd9dde2559864b4f701de8504b1be087a255423a98a24a7a8@2?controller=false&publicKey=f716ebfa11655566bb53c64d9d73321515cb3da4f3a4b2f8eb3ecbf5d35cc715&relay=%7B%22protocol%22%3A%22waku%22%7D")
        WalletConnectClient.pair(pairingParams)
    }

    private var _eventFlow = MutableSharedFlow<WalletUiEvent>()
    val eventFlow = _eventFlow.asLiveData()

    val activeSessions: MutableList<Session> = mutableListOf()

    fun pair(uri: String) {
// Call pair method from SDK and setup callback for session proposal event. Once it's received show session proposal dialog
//        val sessionProposalListener = WalletConnectClientListeners.Session { sessionProposal ->
//
//        }
        val pairingParams = ClientTypes.PairParams(uri = uri)
         WalletConnectClient.pair(pairingParams)


        //mocked session proposal
        val sessionProposal = SessionProposal(
            name = "WalletConnect",
            icon = R.drawable.ic_walletconnect_circle_blue,
            uri = "app.walletconnect.org",
            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Ut eu accumsan nunc. Cras luctus, ipsum at tempor vulputate, metus purus mollis ex, ut maximus tellus lectus non nisl. Duis eu diam sollicitudin, bibendum enim ut, elementum erat.",
            chains = listOf("Ethereum Kovan", "BSC Mainnet", "Fantom Opera"),
            methods = listOf("personal_sign", "eth_sendTransaction", "eth_signedTypedData")
        )

        viewModelScope.launch {
            _eventFlow.emit(ShowSessionProposalDialog(sessionProposal))
        }
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