package org.walletconnect.walletconnectv2.outofband.client

sealed class ClientTypes {

    data class PairParams(val uri: String): ClientTypes()
}