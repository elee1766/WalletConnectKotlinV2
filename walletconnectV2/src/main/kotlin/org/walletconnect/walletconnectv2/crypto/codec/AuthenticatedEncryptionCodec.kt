package org.walletconnect.walletconnectv2.crypto.codec

import org.walletconnect.walletconnectv2.crypto.Codec
import org.walletconnect.walletconnectv2.crypto.data.EncryptionPayload
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import org.walletconnect.walletconnectv2.util.Utils.bytesToHex
import org.walletconnect.walletconnectv2.util.Utils.hexToBytes
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AuthenticatedEncryptionCodec : Codec {

    override fun encrypt(
        message: String,
        sharedKey: String,
        selfPublicKey: PublicKey
    ): EncryptionPayload {
        val (encryptionKey, authenticationKey) = getKeys(sharedKey)

        val data = message.toByteArray(Charsets.UTF_8)
        val iv: ByteArray = randomBytes(16)

        val cipher: Cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(encryptionKey, AES_ALGORITHM),
            IvParameterSpec(iv)
        )
        val cipherText: ByteArray = cipher.doFinal(data)

        val computedMac: String =
            computeHmac(cipherText, iv, authenticationKey, selfPublicKey.keyAsHex.hexToBytes())

        return EncryptionPayload(
            iv = iv.bytesToHex(),
            publicKey = selfPublicKey.keyAsHex,
            mac = computedMac,
            cipherText = cipherText.bytesToHex()
        )
    }

    override fun decrypt(
        payload: EncryptionPayload,
        sharedKey: String
    ): String {

        val (encryptionKey, authenticationKey) = getKeys(sharedKey)

        val data = payload.cipherText.hexToBytes()
        val iv = payload.iv.hexToBytes()

        println("PUBLIC: ${payload.publicKey}")

        val computedHmac =
            computeHmac(data, iv, authenticationKey, payload.publicKey.hexToBytes())

        println("computed mac: $computedHmac")
        println("payload mac: ${payload.mac.lowercase(Locale.getDefault())}")

        if (computedHmac != payload.mac.lowercase(Locale.getDefault())) {
            throw Exception("Invalid Hmac")
        }

        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(encryptionKey, AES_ALGORITHM),
            IvParameterSpec(iv)
        )

        return String(cipher.doFinal(data), Charsets.UTF_8)
    }

    private fun getKeys(sharedKey: String): Pair<ByteArray, ByteArray> {
        val hexKey = sharedKey.hexToBytes()
        val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-512")
        val hashedKey: ByteArray = messageDigest.digest(hexKey)

        val aesKey: ByteArray = hashedKey.sliceArray(0..31)
        val hmacKey: ByteArray = hashedKey.sliceArray(31..63)

        return Pair(aesKey, hmacKey)
    }

    private fun computeHmac(
        data: ByteArray,
        iv: ByteArray,
        key: ByteArray,
        selfPublicKey: ByteArray
    ): String {
        val mac = Mac.getInstance(MAC_ALGORITHM)
        val payload = iv + selfPublicKey + data
        mac.init(SecretKeySpec(key, MAC_ALGORITHM))
        return mac.doFinal(payload).bytesToHex()
    }

    private fun randomBytes(size: Int): ByteArray {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    companion object {
        private const val CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"
        private const val MAC_ALGORITHM = "HmacSHA256"
        private const val HASH_ALGORITHM = "SHA-512"
        private const val AES_ALGORITHM = "AES"
    }
}