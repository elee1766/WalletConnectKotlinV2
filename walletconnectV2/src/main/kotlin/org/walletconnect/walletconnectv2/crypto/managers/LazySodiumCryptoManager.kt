package org.walletconnect.walletconnectv2.crypto.managers

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.goterl.lazysodium.utils.HexMessageEncoder
import com.goterl.lazysodium.utils.Key
import com.goterl.lazysodium.utils.KeyPair
import com.goterl.lazysodium.utils.LibraryLoader
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.data.Key as WCKey
import org.walletconnect.walletconnectv2.crypto.CryptoManager
import org.walletconnect.walletconnectv2.crypto.KeyChain
import org.walletconnect.walletconnectv2.crypto.data.PrivateKey
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class LazySodiumCryptoManager(private val keyChain: KeyChain): CryptoManager {
    private val lazySodium = LazySodiumJava(SodiumJava(LibraryLoader.Mode.PREFER_BUNDLED), StandardCharsets.UTF_8)

    override fun hasKeys(tag: String): Boolean {
        return keyChain.getKey(tag).isNotBlank()
    }

    override fun generateKeyPair(): PublicKey {
        val lsKeyPair = lazySodium.cryptoSignKeypair()
        val curve25519KeyPair = lazySodium.convertKeyPairEd25519ToCurve25519(lsKeyPair)
        val (publicKey, privateKey) = curve25519KeyPair.let { keyPair ->
            PublicKey(keyPair.publicKey.asHexString) to PrivateKey(keyPair.secretKey.asHexString)
        }

        setKeyPair(publicKey, privateKey)

        return publicKey
    }

    override fun generateSharedKey(self: PublicKey, peer: PublicKey, overrideTopic: String?): Topic {
        val (publicKey, privateKey) = getKeyPair(self)
        val sharedKey = lazySodium.cryptoScalarMult(privateKey.toKey(), peer.toKey())

        return setEncryptionKeys(sharedKey.asHexString.toLowerCase().also { println("Talha_SharedKey: $it") }, publicKey, overrideTopic)
    }

    internal fun setEncryptionKeys(sharedKey: String, selfPublicKey: PublicKey, overrideTopic: String?): Topic {
        val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-256")
        val hashedBytes: ByteArray = messageDigest.digest(sharedKey.hexToBytes())
        val topic = Topic(hashedBytes.bytesToHex())
        val sharedKeyObject = object: WCKey {
            override val keyAsHex: String = sharedKey
        }
        val keys = concatKeys(sharedKeyObject, selfPublicKey)

        keyChain.setKey(topic.topicValue, keys)

        return topic
    }

    fun String.hexToBytes(): ByteArray {
        val len = this.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(this[i], 16) shl 4)
                    + Character.digit(this[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun ByteArray.bytesToHex(): String {
        val hexString = StringBuilder(2 * this.size)
        for (i in this.indices) {
            val hex = Integer.toHexString(0xff and this[i].toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }

    internal fun setKeyPair(publicKey: PublicKey, privateKey: PrivateKey) {
        val keys = concatKeys(publicKey, privateKey)

        keyChain.setKey(publicKey.keyAsHex, keys)
    }

    internal fun getKeyPair(wcKey: WCKey): Pair<PublicKey, PrivateKey> {
        val storageKey: String = keyChain.getKey(wcKey.keyAsHex)

        return splitKeys(storageKey)
    }

    internal fun concatKeys(keyA: WCKey, keyB: WCKey): String {
        val encoder = HexMessageEncoder()
        return encoder.encode(encoder.decode(keyA.keyAsHex) + encoder.decode(keyB.keyAsHex))
    }

    internal fun splitKeys(concatKeys: String): Pair<PublicKey, PrivateKey> {
        val hexEncoder = HexMessageEncoder()
        val concatKeysByteArray = hexEncoder.decode(concatKeys)
        val privateKeyByteArray = concatKeysByteArray.sliceArray(0 until (concatKeysByteArray.size / 2))
        val publicKeyByteArray = concatKeysByteArray.sliceArray((concatKeysByteArray.size / 2) until concatKeysByteArray.size)

        return PublicKey(hexEncoder.encode(privateKeyByteArray)) to PrivateKey(hexEncoder.encode(publicKeyByteArray))
    }

    private fun WCKey.toKey(): Key {
        return Key.fromHexString(keyAsHex)
    }
}