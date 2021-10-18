package org.walletconnect.walletconnectv2.crypto.codec

import org.junit.jupiter.api.Test
import org.walletconnect.walletconnectv2.crypto.KeyChain
import org.walletconnect.walletconnectv2.crypto.data.EncryptionPayload
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import kotlin.test.assertEquals

class AuthenticatedEncryptionCodecTest {

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
            "ee16ccdca42d0706a124ea909bb112c87ef187e0b0c7a242f0471d8704579681503df80040d22a1c4f7f678f5cbfbe39d1e95427bda511833540062f16e5eb86fff46518618dd10b7f70ff11d16bea2589a59968eecd764a1e9745c6e9eca64ad89ccc16d52a3a107494940ca3219a14170e74dcbd1d8c218a4b6e6c9dbe22e2a844519ade8e1f48dc106d42051c6674ba14cda1c35af85b18c58b78f3574c33e627f18e23ecebb98207086af8a9f3b384aa8cc0eec757b398f046a0815e91e7df25630e51efd76d202478f3c42a8fe8989a19827f2d8e732d5eaabd464b1293fda6f058cd6572ce80377f1e601609216fbe1f5670c84f61eaae9b8b11e8f578ab44af379c2b2e305a478c109ce841222af295033c4559c4659fad570d996549df14723e63c30bb3f8ee8ce47249301c5875f7e44e0c8888f17bc3e33f14a5aaf6b5354d159a80d19f70b7bfc5452a21588760f6521a7ed2f3df9fa99d990e856aa218adad4265507642136727be59db0c9890b5c213f91587e981cba51a561b9110f0a41378dfbed76b3014cb19f129f1a1b56a0534493f754655615223d1c7de93918d137e8da067719f8f14749fb47d234a5ccc51c850a16c36f203952a5114a7446fb57795bccf7064b9e9d19dbe5b8110db9af39bfef85fa64297119c39323192faf1ffd6876105a58608eea8a62c42a27f28d04a5debb4f73c2e7645aede615b01900e58b284b206baf0286c2cef0357845ed283beb75c97d6dc7966332a34ccda6cfdad9e8340fa11923abb2c0922b92b6735ba44ffdca0b1e22433fc7f8651029a0a6ed780ee21659977850b8da18929a313dcc46a11a1907345a12be0ccb654d1a82f64d8909121a5fdadbeac31a452bf15f25dd570e309de93a5bf311c4acfc42ab367c21e09e994ddefcd994b3dcf61c58eca81022ae1863ba48f5671ba2e96a3cbb539273386f8078e7a8a16999f353f8fd0e44353522dfe89a82b222c4bfaf9807a76f43e5e511a25b3baaf4adbf1fc1ea341c63b75b65ae4b906c383c04ce27eecb88dd256e0fbd6c0fb772dfea15880e61cbd3ade0a9059ff7585a31bbc793ed6624173a170586b7a3f9450e1b7676afafe434e10d425de28e91fe95abe64e388b32c4828c380d0b2f28d2eb435d00ef2f0b914e67a2857f000726e5249115e877bc8011fbcd4c3ceef23e79103ceb5d1b4109ff52dd70bcfbbca83864a9fb6bf5a03b177d879fd0241df7fde3577b240bd977e0d2f91569fe45c1aedeb2425184bb5122cf831bc35"

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
        assertEquals(payload.publicKey.length, 64)
        assertEquals(payload.mac.length, 64)

        println(payload)

        val sharedKey = "09c8154fa949b6bc8a56a7e44b6aa604f3c2617e7663bcc02faa9263411eff2b"

        val json = codec.decrypt(payload, sharedKey)

        println(json)
    }
}