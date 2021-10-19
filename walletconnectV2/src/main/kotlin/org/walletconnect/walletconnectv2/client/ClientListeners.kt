package org.walletconnect.walletconnectv2.client

import org.walletconnect.walletconnectv2.clientcomm.session.Session
import java.net.URI

sealed interface WalletConnectClientListeners {
    fun interface Pairing : WalletConnectClientListeners {
        fun onSessionProposal(proposal: SessionProposal)
    }
}

data class SessionProposal(
    val name: String,
    val description: String,
    val dappUrl: String,
    val icon: List<URI>,
    val chains: List<String>,
    var methods: List<String>
)