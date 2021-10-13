package org.walletconnect.walletconnectv2.crypto.codec

import org.walletconnect.walletconnectv2.crypto.Codec
import org.walletconnect.walletconnectv2.crypto.data.EncryptionPayload
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AuthenticatedEncryptionCodec : Codec {

    override fun encrypt(message: String, sharedKey: String): EncryptionPayload {
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

        val computedMac: String = computeHmac(cipherText, iv, authenticationKey)

        return EncryptionPayload(
            iv = iv.toHexString(),
            publicKey = PublicKey(sharedKey),
            mac = computedMac,
            cipherText = cipherText.toHexString()
        )
    }

    override fun decrypt(payload: EncryptionPayload, sharedKey: String): String {
        val (encryptionKey, authenticationKey) = getKeys(sharedKey)

        val data = payload.cipherText.toByteArray()
        val iv = payload.iv.toByteArray()
        val computedHmac = computeHmac(data, iv, authenticationKey)

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
        val hexKey = sharedKey.encodeToByteArray()
        val messageDigest: MessageDigest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hashedKey: ByteArray = messageDigest.digest(hexKey)

        val aesKey: ByteArray = hashedKey.sliceArray(0..31)
        val hmacKey: ByteArray = hashedKey.sliceArray(31..63)

        return Pair(aesKey, hmacKey)
    }

    private fun computeHmac(data: ByteArray, iv: ByteArray, key: ByteArray): String {
        val mac = Mac.getInstance(MAC_ALGORITHM)
        val payload = data + iv
        mac.init(SecretKeySpec(key, MAC_ALGORITHM))
        return mac.doFinal(payload).toHexString()
    }

    private fun ByteArray.toHexString(): String = toHexString(this, 0, this.size, false)

    private fun toHexString(
        input: ByteArray,
        offset: Int,
        length: Int,
        withPrefix: Boolean
    ): String {
        val stringBuilder = StringBuilder()
        if (withPrefix) {
            stringBuilder.append("0x")
        }
        for (i in offset until offset + length) {
            stringBuilder.append(String.format("%02x", input[i] and 0xFF))
        }
        return stringBuilder.toString()
    }

    private infix fun Byte.and(mask: Int): Int = toInt() and mask

    private fun String.toByteArray(): ByteArray = hexStringToByteArray(this)

    private fun hexStringToByteArray(input: String): ByteArray {
        val cleanInput: String = cleanHexPrefix(input)
        val len = cleanInput.length
        if (len == 0) {
            return byteArrayOf()
        }
        val data: ByteArray
        val startIdx: Int
        if (len % 2 != 0) {
            data = ByteArray(len / 2 + 1)
            data[0] = Character.digit(cleanInput[0], 16).toByte()
            startIdx = 1
        } else {
            data = ByteArray(len / 2)
            startIdx = 0
        }
        var i = startIdx
        while (i < len) {
            data[(i + 1) / 2] = ((Character.digit(cleanInput[i], 16) shl 4)
                    + Character.digit(cleanInput[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun cleanHexPrefix(input: String): String =
        if (containsHexPrefix(input)) {
            input.substring(2)
        } else {
            input
        }

    private fun containsHexPrefix(input: String): Boolean = input.startsWith("0x")

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