package org.walletconnect.walletconnectv2.storage

import android.content.SharedPreferences
import io.mockk.*
import org.junit.jupiter.api.Test
import org.walletconnect.walletconnectv2.crypto.data.PrivateKey
import org.walletconnect.walletconnectv2.crypto.data.PublicKey

class KeyChainTest {

    private val sharedPreferences = mockk<SharedPreferences> {
        every { edit().putString(any(), any()).apply() } just Runs
        every { edit().remove(any()).apply() } just Runs
        every { getString(any(), any()) } returns ""
    }
    private val keyChain: KeyChain = KeyChain(sharedPreferences)

    @Test
    fun `set keys test`() {
        keyChain.setKey("tag", PublicKey("key1"), PrivateKey("key2"))
        verify {
            sharedPreferences.edit().putString(any(), any()).apply()
        }
    }

    @Test
    fun `get keys test`() {
        keyChain.getKeys("tag")
        verify {
            sharedPreferences.getString(any(), any())
        }
    }

    @Test
    fun `delete keys test`() {
        keyChain.deleteKeys("tag")
        verify {
            sharedPreferences.edit().remove(any()).apply()
        }
    }
}