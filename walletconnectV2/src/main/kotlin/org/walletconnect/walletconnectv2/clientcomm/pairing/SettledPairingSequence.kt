package org.walletconnect.walletconnectv2.clientSync.pairing

import org.json.JSONObject
import org.walletconnect.walletconnectv2.clientSync.pairing.proposal.PairingProposedPermissions
import org.walletconnect.walletconnectv2.common.Expiry
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.data.PublicKey

data class SettledPairingSequence(
    val settledTopic: Topic,
    val relay: JSONObject,
    val sharedKey : String,
    val selfPublicKey: PublicKey,
    val peerPublicKey: PublicKey,
    val sequencePermissions: Pair<PairingProposedPermissions?, PublicKey>,
    val expiry: Expiry
)
