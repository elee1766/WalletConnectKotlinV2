package org.walletconnect.walletconnectv2.clientsync.pairing

import org.json.JSONObject
import org.walletconnect.walletconnectv2.common.Expiry
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import org.walletconnect.walletconnectv2.outofband.pairing.proposal.PairingProposedPermissions

data class SettledPairingSequence(
    val settledTopic: Topic,
    val relay: JSONObject,
    val sharedKey : String,
    val selfPublicKey: PublicKey,
    val peerPublicKey: PublicKey,
    val sequencePermissions: Pair<PairingProposedPermissions?, PublicKey>,
    val expiry: Expiry
)
