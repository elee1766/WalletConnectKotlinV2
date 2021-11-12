package org.walletconnect.walletconnectv2.engine.serailising

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi

class JsonRpcSerializer(val moshi: Moshi) : JsonRpcSerialising {

    override fun <T> trySerialize(typeClass: Class<T>, type: T): String =
        moshi.adapter(typeClass).toJson(type)

    override fun <T> tryDeserialize(type: Class<T>, json: String): T? =
        try {
            org.walletconnect.walletconnectv2.moshi.adapter(type).fromJson(json)
        } catch (error: JsonDataException) {
            null
        }
}