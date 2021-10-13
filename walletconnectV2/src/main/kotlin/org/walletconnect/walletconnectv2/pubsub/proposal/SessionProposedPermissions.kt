package org.walletconnect.walletconnectv2.pubsub.proposal

import org.walletconnect.walletconnectv2.outofband.pairing.proposal.JsonRPC

data class SessionProposedPermissions(
    val blockchain: Blockchain,
    val jsonRPC: JsonRPC,
    val notifications: Notifications
)
