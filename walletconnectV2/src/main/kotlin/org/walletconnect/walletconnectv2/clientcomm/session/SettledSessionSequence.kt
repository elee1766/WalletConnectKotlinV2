package org.walletconnect.walletconnectv2.clientSync.session

import org.walletconnect.walletconnectv2.clientSync.session.proposal.SessionProposedPermissions
import org.walletconnect.walletconnectv2.clientSync.session.success.RelayProtocolOptions
import org.walletconnect.walletconnectv2.common.Expiry
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import org.walletconnect.walletconnectv2.clientSync.session.success.SessionState

data class SettledSessionSequence(
    val settledTopic: Topic,
    val relay: RelayProtocolOptions,
    val sharedKey : String,
    val selfPublicKey: PublicKey,
    val peerPublicKey: PublicKey,
    val sequencePermissions: SessionProposedPermissions,
    val expiry: Expiry,
    val state: SessionState
)
