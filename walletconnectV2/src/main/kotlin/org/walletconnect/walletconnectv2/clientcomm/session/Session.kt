package org.walletconnect.walletconnectv2.clientcomm.session

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.json.JSONObject
import org.walletconnect.walletconnectv2.clientcomm.session.proposal.SessionProposedPermissions
import org.walletconnect.walletconnectv2.clientcomm.session.proposal.SessionProposer
import org.walletconnect.walletconnectv2.clientcomm.session.proposal.SessionSignal
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.common.Ttl
import org.walletconnect.walletconnectv2.common.network.adapters.JSONObjectAdapter
import org.walletconnect.walletconnectv2.common.network.adapters.TopicAdapter
import org.walletconnect.walletconnectv2.common.network.adapters.TtlAdapter

sealed class Session {

    @JsonClass(generateAdapter = true)
    data class Proposal(
        @Json(name = "topic")
        @field:TopicAdapter.Qualifier
        val topic: Topic,
        @Json(name = "relay")
        @field:JSONObjectAdapter.Qualifier
        val relay: JSONObject,
        @Json(name = "proposer")
        val proposer: SessionProposer,
        @Json(name = "signal")
        val signal: SessionSignal,
        @Json(name = "permissions")
        val permissions: SessionProposedPermissions,
        @Json(name = "ttl")
        @field:TtlAdapter.Qualifier
        val ttl: Ttl
    )
}