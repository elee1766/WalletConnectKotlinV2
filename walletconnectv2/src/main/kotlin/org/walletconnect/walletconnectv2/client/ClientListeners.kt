package org.walletconnect.walletconnectv2.client

//Note: should be as a global listener because when resubscribing it should be ready just from the initalization, without any public method call
interface WalletConnectClientListener {
    fun onSessionProposal(proposal: SessionProposal)
    fun onSessionRequest(request: SessionRequest)
    fun onSessionDelete(topic: String, reason: String)
}

sealed interface WalletConnectClientListeners {

    fun onError(error: Throwable)

    interface Pairing : WalletConnectClientListeners {
        fun onSuccess(topic: String)
    }

    interface SessionApprove : WalletConnectClientListeners {
        fun onSuccess(session: SettledSession)
    }

    interface SessionReject : WalletConnectClientListeners {
        fun onSuccess(topic: String)
    }

    interface SessionDelete : WalletConnectClientListeners {
        fun onSuccess(topic: String, reason: String)
    }
}