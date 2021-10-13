package org.walletconnect.walletconnectv2.crypto.codec

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AuthenticatedEncryptionCodecTest {

    private val codec: AuthenticatedEncryptionCodec = AuthenticatedEncryptionCodec()

    @Test
    fun `Codec AES_256_CBC and Hmac_SHA256 authentication test`() {
        val sharedKey = "94BA14D48AAF8E0D3FA13E94A73C8745136EB7C3D7BA6232E6512A78D6624A04"
        val message = "WalletConnect"

        val encryptedPayload = codec.encrypt(message, sharedKey)
        assertEquals(sharedKey, encryptedPayload.publicKey.keyAsHex)

        val text = codec.decrypt(encryptedPayload, sharedKey)
        assertEquals(message, text)
    }

    @Test
    fun `Codec AES_256_CBC and Hmac_SHA256 invalid HMAC test`() {
        val sharedKey1 = "94BA14D48AAF8E0D3FA13E94A73C8745136EB7C3D7BA6232E6512A78D6624A04"
        val sharedKey2 = "95BA14D48AAF8E0D3FA13E94A73C8745136EB7C3D7BA6232E6512A78D6624A04"
        val message = "WalletConnect"

        val encryptedPayload = codec.encrypt(message, sharedKey1)
        assertEquals(sharedKey1, encryptedPayload.publicKey.keyAsHex)

        try {
            codec.decrypt(encryptedPayload, sharedKey2)
        } catch (e: Exception){
            assertEquals("Invalid Hmac",e.message)
        }
    }
}