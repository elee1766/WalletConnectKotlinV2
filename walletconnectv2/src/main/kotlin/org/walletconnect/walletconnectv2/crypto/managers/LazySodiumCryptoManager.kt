package org.walletconnect.walletconnectv2.crypto.managers

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.utils.HexMessageEncoder
import com.goterl.lazysodium.utils.Key
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.CryptoManager
import org.walletconnect.walletconnectv2.storage.KeyStore
import org.walletconnect.walletconnectv2.crypto.data.PrivateKey
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import org.walletconnect.walletconnectv2.crypto.data.SharedKey
import org.walletconnect.walletconnectv2.storage.KeyChain
import org.walletconnect.walletconnectv2.util.bytesToHex
import org.walletconnect.walletconnectv2.util.hexToBytes
import java.security.MessageDigest
import org.walletconnect.walletconnectv2.crypto.data.Key as WCKey

class LazySodiumCryptoManager(private val keyChain: KeyStore = KeyChain()) : CryptoManager {
    private val lazySodium = LazySodiumAndroid(SodiumAndroid())

    override fun hasKeys(tag: String): Boolean {
        return keyChain.getKey(tag).keyAsHex.isNotBlank()
    }

    override fun generateKeyPair(): PublicKey {
        val lsKeyPair = lazySodium.cryptoSignKeypair()
        val curve25519KeyPair = lazySodium.convertKeyPairEd25519ToCurve25519(lsKeyPair)
        val (publicKey, privateKey) = curve25519KeyPair.let { keyPair ->
            PublicKey(keyPair.publicKey.asHexString.lowercase()) to PrivateKey(keyPair.secretKey.asHexString.lowercase())
        }
        setKeyPair(publicKey, privateKey)
        return publicKey
    }

    override fun generateTopicAndSharedKey(
        self: PublicKey,
        peer: PublicKey,
        overrideTopic: String?
    ): Pair<SharedKey, Topic> {
        val (publicKey, privateKey) = getKeyPair(self)
        val sharedKeyHex = lazySodium.cryptoScalarMult(privateKey.toKey(), peer.toKey()).asHexString.lowercase()
        val sharedKey = SharedKey(sharedKeyHex)
        val topic = generateTopic(sharedKey.keyAsHex)
        setEncryptionKeys(sharedKey, publicKey, Topic(overrideTopic ?: topic.topicValue))
        return Pair(sharedKey, topic)
    }

    override fun setEncryptionKeys(sharedKey: SharedKey, publicKey: PublicKey, topic: Topic) {
        val sharedKeyObject = object : WCKey {
            override val keyAsHex: String = sharedKey.keyAsHex
        }
        val keys = concatKeys(sharedKeyObject, publicKey)
        keyChain.setKey(topic.topicValue, keys)
    }

    override fun getKeyAgreement(topic: Topic): Pair<SharedKey, PublicKey> {
        val storageKey: String = keyChain.getKey(topic.topicValue).keyAsHex
        val (sharedKey, peerPublic) = splitKeys(storageKey)
        return Pair(SharedKey(sharedKey), PublicKey(peerPublic))
    }

    fun setKeyPair(publicKey: PublicKey, privateKey: PrivateKey) {
        val keys = concatKeys(publicKey, privateKey)
        keyChain.setKey(publicKey.keyAsHex, keys)
    }

    fun getKeyPair(wcKey: WCKey): Pair<PublicKey, PrivateKey> {
        val storageKey: String = keyChain.getKey(wcKey.keyAsHex).keyAsHex
        val (publicKey, privateKey) = splitKeys(storageKey)
        return Pair(PublicKey(publicKey), PrivateKey(privateKey))
    }

    fun concatKeys(keyA: WCKey, keyB: WCKey): String = with(HexMessageEncoder()) {
        encode(decode(keyA.keyAsHex) + decode(keyB.keyAsHex))
    }

    fun splitKeys(concatKeys: String): Pair<String, String> {
        val hexEncoder = HexMessageEncoder()
        val concatKeysByteArray = hexEncoder.decode(concatKeys)
        val privateKeyByteArray =
            concatKeysByteArray.sliceArray(0 until (concatKeysByteArray.size / 2))
        val publicKeyByteArray =
            concatKeysByteArray.sliceArray((concatKeysByteArray.size / 2) until concatKeysByteArray.size)
        return hexEncoder.encode(privateKeyByteArray) to hexEncoder.encode(publicKeyByteArray)
    }

    private fun generateTopic(sharedKey: String): Topic {
        val messageDigest: MessageDigest = MessageDigest.getInstance(SHA_256)
        val hashedBytes: ByteArray = messageDigest.digest(sharedKey.hexToBytes())
        return Topic(hashedBytes.bytesToHex())
    }

    fun getSharedKeyUsingPrivate(self: PrivateKey, peer: PublicKey): String {
        return lazySodium.cryptoScalarMult(self.toKey(), peer.toKey()).asHexString
    }

    private fun WCKey.toKey(): Key {
        return Key.fromHexString(keyAsHex)
    }

    companion object {
        private const val SHA_256: String = "SHA-256"
    }
}