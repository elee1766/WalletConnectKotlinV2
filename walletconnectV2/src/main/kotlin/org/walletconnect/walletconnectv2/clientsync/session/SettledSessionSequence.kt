package org.walletconnect.walletconnectv2.clientsync.session

import org.json.JSONObject
import org.walletconnect.walletconnectv2.common.Expiry
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import org.walletconnect.walletconnectv2.pubsub.proposal.SessionProposedPermissions
import org.walletconnect.walletconnectv2.pubsub.success.SessionState

data class SettledSessionSequence(
    val settledTopic: Topic,
    val relay: JSONObject,
    val sharedKey : String,
    val selfPublicKey: PublicKey,
    val peerPublicKey: PublicKey,
    val sequencePermissions: SessionProposedPermissions,
    val expiry: Expiry,
    val state: SessionState
)
