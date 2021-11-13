package org.walletconnect.walletconnectv2.client

//Note: we should keep a global listener because when resubscribing the communication on topics should be ready just from the initialization, without any public method call
interface WalletConnectClientListener {
    fun onSessionProposal(proposal: WalletConnectClientData.SessionProposal)
    fun onSessionRequest(request: WalletConnectClientData.SessionRequest)
    fun onSessionDelete(topic: String, reason: String)
}

sealed interface WalletConnectClientListeners {

    fun onError(error: Throwable)

    interface Pairing : WalletConnectClientListeners {
        fun onSuccess(topic: String)
    }

    interface SessionReject : WalletConnectClientListeners {
        fun onSuccess(topic: String)
    }

    interface SessionDelete : WalletConnectClientListeners {
        fun onSuccess(topic: String)
    }

    interface SessionApprove : WalletConnectClientListeners {
        fun onSuccess(session: WalletConnectClientData.SettledSession)
    }
}