package org.walletconnect.walletconnectv2.pubsub.proposal

data class SessionSignal(
    val method: String = "pairing",
    val params: SessionSignalParams
)
