package org.walletconnect.walletconnectv2.pubsub.proposal

import org.walletconnect.walletconnectv2.outofband.pairing.success.AppMetaData

data class SessionProposer(
    val publicKey: String,
    val controller: Boolean,
    val metadata: AppMetaData? = null
)
