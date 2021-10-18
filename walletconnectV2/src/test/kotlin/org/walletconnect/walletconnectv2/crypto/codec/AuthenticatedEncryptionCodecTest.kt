package org.walletconnect.walletconnectv2.crypto.codec

import org.junit.jupiter.api.Test
import org.walletconnect.walletconnectv2.crypto.CryptoManager
import org.walletconnect.walletconnectv2.crypto.KeyChain
import org.walletconnect.walletconnectv2.crypto.data.EncryptionPayload
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import org.walletconnect.walletconnectv2.crypto.managers.Curve25519CryptoManager
import org.walletconnect.walletconnectv2.util.Utils.bytesToHex
import org.walletconnect.walletconnectv2.util.Utils.hexToBytes
import kotlin.test.assertEquals

class AuthenticatedEncryptionCodecTest {

    private val keyChain = object : KeyChain {
        val mapOfKeys = mutableMapOf<String, String>()

        override fun setKey(key: String, value: String) {
            mapOfKeys[key] = value
        }

        override fun getKey(key: String): String {
            return mapOfKeys[key]!!
        }
    }

    private val cipher: Curve25519CryptoManager = Curve25519CryptoManager(keyChain)
    private val codec: AuthenticatedEncryptionCodec = AuthenticatedEncryptionCodec()


    @Test
    fun `Codec AES_256_CBC and Hmac_SHA256 authentication test`() {
        val sharedKey = "94BA14D48AAF8E0D3FA13E94A73C8745136EB7C3D7BA6232E6512A78D6624A04"
        val message = "WalletConnect"

        val encryptedPayload = codec.encrypt(message, sharedKey, PublicKey("12"))

        println(encryptedPayload)

        assertEquals(encryptedPayload.publicKey, "12")

        val text = codec.decrypt(encryptedPayload, sharedKey)

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
            codec.decrypt(encryptedPayload, sharedKey2)
        } catch (e: Exception) {
            assertEquals("Invalid Hmac", e.message)
        }
    }

    @Test
    fun `deserialize encrypted message to encryption payload`() {
        val hex =
            "b055a28c9f86e9afca4d8da6d94f012b7ea3d505828072cab81523075a942787464ca7183a5069ad0fc1a6e2c941d6626cbfca0224c170ed24f3e810794cf5edee196c5ad4484b915649b7118d7f6d8e45b4ee7cbe7b3f832b6f5e10b51907755b0312dc2cd6368d448d90bb3557db6cc9f072a9da9f46de919f1abbfed7517dc7b176a80061d27cd7fc6cec04436e7fe029cd5b8be916456ae09abd6b7cab175b7de9d85b44a13109534083343b146a67c7d106870e7656d828073c45018632651f1e8ac807cab3d47549dbc70195477b2afb834a42ae83e32d6b5c0c5b2d568bff0b49fc812a43b27194689e9cc50d7b4e714b761c09a8433b432ebd622794aa94f8b96d0d87e66c1efeff1380c436a37e55beff36b50ef13ea1b6183e6d60ccac7b2ff3ac0ce84b9b73ca33eae26c42c26c73e8da6d138471c0adb0916aa51d00d195c9af1950f29b399fa163b9518f8be7a7c3dfbd9bba7f6efacfc023c4b4f7badebcf3d105a575995f2cc700d97dc72ebc428e533b9d973fd3efd3f862deb55d8a4733f9a1533390fa6ca2a8924536a6218a20e3ab4be2027df3e3f9ea10236fdd993216a811b06f138ab0d46949cafebbe0f79c357addf6d251633841d97f2eff9d2ca37b2578ae77a14bebe8a3d7188303c3de536c70cbc954c473d7e389391d8a4e62e1ddd8b257236ba2530b57def27f78aa8246b07e93349f161f237080a5a37c8631884dd2ce1499cfc8c7abff992aba43dcb6ddb11ec4c07e4629693609310443251c843bfec25295c4d83ef1eeb8bb0ca732f8b4aa7f571224f87e47ffbb20ce690bb0c13a50f028523cf95a1dedfd5f237b7cfae6e9e88bb66874b6aa9c1377b0b74856353af10caf3cf15f205bb3abbdbe4f03562998d8aa73a1e00cce3f8285b7d8b310c622f6debeac9b695408b4decc17c5843eaf548981745464e0d573476d492f559d84a2fd09adfd266d38597822cd4af9b5518ff53fb5f0be9e2e0ed972b20256bab817198a0b3ce62fa8d4abb1fbe714a90453a6e9104e109ad3aa3ec573f127816ad41fdc4d044c789a9efff98a67484d4184d5b2a655ec8c2ba5a8335477286401d6fc03f28d7d38e79f440407dc7774c73b08322f9f18177397d540569d73e07672de55651d8e24cbd47056fe51b8b7096a24f65b61de2c3e20bf9f7d3871257d0b60eace9eec4a8c86a73833bd9f313c367e829d89898b3e9854b43de0defce47c96222ceafe7a408c05283a8b8776e9883d52a9f42172e1ea5a8fe0d78b953bd2cd"

        val pubKeyStartIndex = EncryptionPayload.ivLength
        val macStartIndex = pubKeyStartIndex + EncryptionPayload.publicKeyLength
        val cipherTextStartIndex = macStartIndex + EncryptionPayload.macLength

        val iv = hex.substring(0, pubKeyStartIndex)
        val publicKey = hex.substring(pubKeyStartIndex, macStartIndex)
        val mac = hex.substring(macStartIndex, cipherTextStartIndex)
        val cipherText = hex.substring(cipherTextStartIndex, hex.length) //proposal payload

        val payload =
            EncryptionPayload(iv = iv, publicKey = publicKey, mac = mac, cipherText = cipherText)

        assertEquals(payload.iv.length, 32)
        assertEquals(payload.iv, "b055a28c9f86e9afca4d8da6d94f012b")

        assertEquals(payload.publicKey.length, 64)
        assertEquals(payload.publicKey, "7ea3d505828072cab81523075a942787464ca7183a5069ad0fc1a6e2c941d662")

        assertEquals(payload.mac.length, 64)
        assertEquals(payload.mac, "6cbfca0224c170ed24f3e810794cf5edee196c5ad4484b915649b7118d7f6d8e")

        println(payload)

        val sharedKey = "72852cd86c9c889d26b7b622050eca328943c228efc942b6e316dff213ed9b4e"

        val json = codec.decrypt(payload, sharedKey)

        println(json)
    }
}