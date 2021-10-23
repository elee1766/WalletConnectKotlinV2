package org.walletconnect.walletconnectv2.client

sealed class ClientTypes {

    data class InitialParams(
        val useTls: Boolean = true,
        val hostName: String = "relay.walletconnect.com",
        val apiKey: String = "",
        val isController: Boolean = true
    ) : ClientTypes()

    data class PairParams(val uri: String) : ClientTypes()
}
