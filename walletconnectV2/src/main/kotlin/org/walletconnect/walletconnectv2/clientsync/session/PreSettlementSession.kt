package org.walletconnect.walletconnectv2.clientsync.session

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.walletconnect.walletconnectv2.pubsub.Session

sealed class PreSettlementSession {

    abstract val id: Int
    abstract val jsonrpc: String
    abstract val method: String
    abstract val params: Session

//    data class SessionPropose() : PreSettlementSession()

    @JsonClass(generateAdapter = true)
    data class Approve(
        @Json(name = "id")
        override val id: Int,
        @Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @Json(name = "method")
        override val method: String = "wc_sessionApprove",
        @Json(name = "params")
        override val params: Session.Success
    ): PreSettlementSession()

    data class Reject(
        override val id: Int,
        override val jsonrpc: String = "2.0",
        override val method: String = "wc_sessionReject",
        override val params: Session.Failure
    ): PreSettlementSession()

}