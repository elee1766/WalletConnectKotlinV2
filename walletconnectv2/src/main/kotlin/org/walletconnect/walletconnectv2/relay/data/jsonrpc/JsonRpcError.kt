package org.walletconnect.walletconnectv2.relay.data.jsonrpc

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

class JsonRpcError(
    @Json(name = "jsonrpc")
    val jsonrpc: String = "2.0",
    @Json(name = "error")
    val error: Error,
    @Json(name = "id")
    val id: Long
)

data class Error(
    @Json(name = "code")
    val code: Long,
    @Json(name = "message")
    val message: String,
) {
    val errorMessage: String = "Error code: $code; Error message: $message"
}

sealed class PublishResponse {
    abstract val jsonrpc: String
    abstract val id: Long

    @JsonClass(generateAdapter = true)
    data class Response(
        @Json(name = "id")
        override val id: Long,
        @Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @Json(name = "result")
        val result: Boolean
    ) : PublishResponse()

    @JsonClass(generateAdapter = true)
    class JsonRpcError(
        @Json(name = "jsonrpc")
        override val jsonrpc: String = "2.0",
        @Json(name = "error")
        val error: Error,
        @Json(name = "id")
        override val id: Long
    ) : PublishResponse()
}