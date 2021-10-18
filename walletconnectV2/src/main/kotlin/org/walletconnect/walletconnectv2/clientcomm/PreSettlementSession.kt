package org.walletconnect.walletconnectv2.clientcomm

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.walletconnect.walletconnectv2.clientcomm.session.Session

sealed class PreSettlementSession {
    abstract val id: Int
    abstract val jsonrpc: String
    abstract val method: String

    @JsonClass(generateAdapter = true)
    data class Proposal(
        @Json(name = "id")
        override val id: Int,
        @Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @Json(name = "method")
        override val method: String = "wc_sessionPropose",
        @Json(name = "params")
        val params: Session.Proposal
    ) : PreSettlementSession()
}