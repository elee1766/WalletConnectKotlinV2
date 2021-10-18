package org.walletconnect.walletconnectv2
import org.walletconnect.walletconnectv2.pubsub.Session

sealed interface WalletConnectClientListeners {
    fun interface Pairing : WalletConnectClientListeners {
        fun onSessionProposal(proposal: Session.SessionProposal)
    }
}