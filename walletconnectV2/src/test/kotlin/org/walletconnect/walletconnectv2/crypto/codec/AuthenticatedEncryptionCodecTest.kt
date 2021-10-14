package org.walletconnect.walletconnectv2.crypto.codec

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tinder.scarlet.utils.getRawType
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.walletconnect.walletconnectv2.clientsync.session.PreSettlementSession
import org.walletconnect.walletconnectv2.common.Expiry
import org.walletconnect.walletconnectv2.common.SubscriptionId
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.common.Ttl
import org.walletconnect.walletconnectv2.common.network.adapters.*
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import org.walletconnect.walletconnectv2.pubsub.RelayProtocolOptions
import org.walletconnect.walletconnectv2.pubsub.Session
import org.walletconnect.walletconnectv2.pubsub.success.SessionParticipant
import org.walletconnect.walletconnectv2.pubsub.success.SessionState
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthenticatedEncryptionCodecTest {

    private val codec: AuthenticatedEncryptionCodec = AuthenticatedEncryptionCodec()

    @Test
    fun `Codec AES_256_CBC and Hmac_SHA256 authentication test`() {
        val sharedKey = "94BA14D48AAF8E0D3FA13E94A73C8745136EB7C3D7BA6232E6512A78D6624A04"
        val message = "WalletConnect"
        val encryptedPayload = codec.encrypt(message, sharedKey, PublicKey("12"))
        assertEquals(encryptedPayload.publicKey, "12")

        val text = codec.decrypt(encryptedPayload, sharedKey, PublicKey("12"))
        assertEquals(text, message)
    }

    @Test
    fun `Codec AES_256_CBC and Hmac_SHA256 invalid HMAC test`() {
        val sharedKey1 = "94BA14D48AAF8E0D3FA13E94A73C8745136EB7C3D7BA6232E6512A78D6624A04"
        val sharedKey2 = "95BA14D48AAF8E0D3FA13E94A73C8745136EB7C3D7BA6232E6512A78D6624A04"
        val message = "WalletConnect"

        val encryptedPayload = codec.encrypt(message, sharedKey1, PublicKey("12"))
        assertEquals(encryptedPayload.publicKey, "12")

        try {
            codec.decrypt(encryptedPayload, sharedKey2, PublicKey("12"))
        } catch (e: Exception) {
            assertEquals("Invalid Hmac", e.message)
        }
    }
}