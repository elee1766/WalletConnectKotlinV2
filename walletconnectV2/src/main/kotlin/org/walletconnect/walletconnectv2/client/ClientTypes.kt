package org.walletconnect.walletconnectv2.client

import android.app.Application

sealed class ClientTypes {

    data class InitialParams(
        val useTls: Boolean = true,
        val hostName: String = "relay.walletconnect.com",
        val apiKey: String = "",
        val isController: Boolean = true,
        val application: Application
    ) : ClientTypes()

    data class PairParams(val uri: String) : ClientTypes()
}
