package org.walletconnect.walletconnectv2.pubsub

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.json.JSONObject
import org.walletconnect.walletconnectv2.common.Expiry
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.common.network.adapters.ExpiryAdapter
import org.walletconnect.walletconnectv2.common.network.adapters.JSONObjectAdapter
import org.walletconnect.walletconnectv2.common.network.adapters.TopicAdapter
import org.walletconnect.walletconnectv2.pubsub.success.SessionParticipant
import org.walletconnect.walletconnectv2.pubsub.success.SessionState

sealed class Session {

//    data class SessionProposal() : Session()

    @JsonClass(generateAdapter = true)
    data class Success(
        @Json(name = "topic")
        @TopicAdapter.Qualifier
        val settledTopic: Topic,
        @Json(name = "relay")
        @JSONObjectAdapter.Qualifier
        val relay: JSONObject,
        @Json(name = "responder")
        val responder: SessionParticipant,
        @Json(name = "expiry")
        @ExpiryAdapter.Qualifier
        val expiry: Expiry,
        @Json(name = "state")
        val state: SessionState
    ) : Session()

    class Failure(val reason: String) : Session()
}
