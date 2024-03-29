package org.walletconnect.walletconnectv2.common.network.adapters

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.walletconnect.walletconnectv2.common.Ttl

class TtlAdapterTest {
    private val moshi = Moshi.Builder()
        .add { _, _, _ ->
            TtlAdapter
        }
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun toJson() {
        val ttl = Ttl(100L)
        val expected = """"${ttl.seconds}""""

        val ttlJson = moshi.adapter(Ttl::class.java).toJson(ttl)

        Assertions.assertEquals(expected, """"$ttlJson"""")
    }
}