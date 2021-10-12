package org.walletconnect.walletconnectv2.outofband.client

sealed class ClientTypes {
    data class InitialParams(
        val useTls: Boolean,
        val hostName: String,
        val apiKey: String,
        val isController: Boolean
    ) : ClientTypes()

    data class PairParams(val uri: String) : ClientTypes()
}
