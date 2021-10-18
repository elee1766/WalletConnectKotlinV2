package org.walletconnect.walletconnectv2.client

import org.walletconnect.walletconnectv2.clientcomm.session.Session

sealed interface WalletConnectClientListeners {
    fun interface Pairing : WalletConnectClientListeners {
        fun onSessionProposal(proposal: Session.Proposal)
    }
}