package org.walletconnect.walletconnectv2.crypto.managers

import com.goterl.lazysodium.utils.HexMessageEncoder
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.CryptoManager
import org.walletconnect.walletconnectv2.crypto.KeyChain
import org.walletconnect.walletconnectv2.crypto.data.Key
import org.walletconnect.walletconnectv2.crypto.data.PrivateKey
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import org.whispersystems.curve25519.Curve25519
import java.security.MessageDigest

class Curve25519CryptoManager(private val keyChain: KeyChain) : CryptoManager {

    private val hexEncoder = HexMessageEncoder()
    private val cipher: Curve25519 = Curve25519.getInstance(Curve25519.BEST)

    override fun hasKeys(tag: String): Boolean {
        return keyChain.getKey(tag).isNotBlank()
    }

    override fun generateKeyPair(): PublicKey {
        val keyPair = cipher.generateKeyPair()

        val publicKey = PublicKey(hexEncoder.encode(keyPair.publicKey))
        val privateKey = PrivateKey(hexEncoder.encode(keyPair.privateKey))

        println("PublicKey: $publicKey\n")

        setKeyPair(publicKey, privateKey)
        return publicKey
    }

    override fun generateSharedKey(
        selfPublic: PublicKey,
        peerPublic: PublicKey,
        overrideTopic: String?
    ): Topic {
        val (publicKey, privateKey) = getKeyPair(selfPublic)

        val bytesPeerPublic = hexEncoder.decode(peerPublic.keyAsHex)
        val bytesPrivateKey = hexEncoder.decode(privateKey.keyAsHex)

        val sharedSecret: ByteArray = cipher.calculateAgreement(bytesPeerPublic, bytesPrivateKey)

        return setEncryptionKeys(sharedSecret, publicKey, overrideTopic)
    }

    private fun setEncryptionKeys(
        sharedKey: ByteArray,
        selfPublicKey: PublicKey,
        overrideTopic: String?
    ): Topic {

        val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-256")
        val hashedBytes: ByteArray = messageDigest.digest(sharedKey)

        val topic = Topic(overrideTopic ?: hashedBytes.bytesToHex())

        val sharedKeyObject = object : Key {
            override val keyAsHex: String = sharedKey.bytesToHex()
        }
        val keys = concatKeys(sharedKeyObject, selfPublicKey)
        keyChain.setKey(topic.topicValue, keys)

        println("Topic B: ${topic.topicValue}")

        return topic
    }

    internal fun setKeyPair(publicKey: PublicKey, privateKey: PrivateKey) {
        val keys = concatKeys(publicKey, privateKey)

        keyChain.setKey(publicKey.keyAsHex, keys)
    }

    internal fun getKeyPair(wcKey: Key): Pair<PublicKey, PrivateKey> {
        val storageKey: String = keyChain.getKey(wcKey.keyAsHex)

        return splitKeys(storageKey)
    }

    internal fun concatKeys(keyA: Key, keyB: Key): String {
        val encoder = HexMessageEncoder()
        return encoder.encode(encoder.decode(keyA.keyAsHex) + encoder.decode(keyB.keyAsHex))
    }

    internal fun splitKeys(concatKeys: String): Pair<PublicKey, PrivateKey> {
        val hexEncoder = HexMessageEncoder()
        val concatKeysByteArray = hexEncoder.decode(concatKeys)
        val privateKeyByteArray =
            concatKeysByteArray.sliceArray(0 until (concatKeysByteArray.size / 2))
        val publicKeyByteArray =
            concatKeysByteArray.sliceArray((concatKeysByteArray.size / 2) until concatKeysByteArray.size)

        return PublicKey(hexEncoder.encode(privateKeyByteArray)) to
                PrivateKey(hexEncoder.encode(publicKeyByteArray))
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
}