package org.walletconnect.walletconnectv2.crypto.managers

import com.goterl.lazysodium.utils.HexMessageEncoder
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.CryptoManager
import org.walletconnect.walletconnectv2.crypto.KeyChain
import org.walletconnect.walletconnectv2.crypto.data.Key
import org.walletconnect.walletconnectv2.crypto.data.PrivateKey
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import org.walletconnect.walletconnectv2.util.Utils.toBytes
import org.walletconnect.walletconnectv2.util.Utils.toHex
import sun.security.ec.XDHPublicKeyImpl
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.spec.NamedParameterSpec
import java.security.spec.NamedParameterSpec.X25519
import java.security.spec.XECPrivateKeySpec
import javax.crypto.KeyAgreement

class WalletConnectCryptoManager(private val keyChain: KeyChain) : CryptoManager {

    override fun generateKeyPair(): PublicKey {
        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(X25519.name)
        keyPairGenerator.initialize(NamedParameterSpec(X25519.name))
        val keyPair = keyPairGenerator.generateKeyPair()
        val publicKey = PublicKey(keyPair.public.encoded.toHex())
        val privateKey = PrivateKey(keyPair.private.encoded.toHex())

        println("PrivateKey1: ${keyPair.private.encoded}; ${keyPair.private.encoded.toHex()} PublicKey1: ${keyPair.public.encoded}; ${keyPair.public.encoded.toHex()}")

        setKeyPair(publicKey, privateKey)

        return publicKey
    }

    override fun generateSharedKey(
        self: PublicKey,
        peer: PublicKey,
        overrideTopic: String?
    ): Topic {
        val (publicKey, privateKey) = getKeyPair(self)
        println("PrivateKey2: $privateKey; PublicKey2: $publicKey")

        val keyPair = KeyPair(peer, privateKey)

        val keyAgreement: KeyAgreement = KeyAgreement.getInstance(X25519.name)
        val paramSpec = NamedParameterSpec(X25519.name)
        val xdhc = XECPrivateKeySpec(paramSpec, privateKey.keyAsHex.toBytes())

        val keyFactory: KeyFactory = KeyFactory.getInstance("XDH")

//        XDHPublicKeyImpl(ByteArray(1))
        keyAgreement.init(keyFactory.generatePrivate(xdhc))
        keyAgreement.doPhase(peer, false)

        val secret: ByteArray = keyAgreement.generateSecret()
        val secretString: String = secret.toHex()

        println("Secret Bytes: $secret; Secret String: $secretString")

        return setEncryptionKeys(sharedKey, publicKey, overrideTopic)
    }

    private fun setEncryptionKeys(
        sharedKey: String,
        selfPublicKey: PublicKey,
        overrideTopic: String?
    ): Topic {

        val sharedBytes = sharedKey.toBytes()
        val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-256")
        val hashedBytes: ByteArray = messageDigest.digest(sharedBytes)

        val topic = Topic(overrideTopic ?: )

        val sharedKeyObject = object : Key {
            override val keyAsHex: String = sharedKey
        }
        val keys = concatKeys(sharedKeyObject, selfPublicKey)
        keyChain.setKey(topic.topicValue, keys)

        return topic
    }

    override fun hasKeys(tag: String): Boolean {
        return keyChain.getKey(tag).isNotBlank()
    }

    private fun setKeyPair(publicKey: PublicKey, privateKey: PrivateKey) {
        val keys = concatKeys(publicKey, privateKey)

        keyChain.setKey(publicKey.keyAsHex, keys)
    }

    private fun getKeyPair(wcKey: Key): Pair<PublicKey, PrivateKey> {
        val storageKey: String = keyChain.getKey(wcKey.keyAsHex)

        return splitKeys(storageKey)
    }

    private fun concatKeys(keyA: Key, keyB: Key): String {
        val encoder = HexMessageEncoder()
        return encoder.encode(encoder.decode(keyA.keyAsHex) + encoder.decode(keyB.keyAsHex))
    }

    private fun splitKeys(concatKeys: String): Pair<PublicKey, PrivateKey> {
        val hexEncoder = HexMessageEncoder()
        val concatKeysByteArray = hexEncoder.decode(concatKeys)
        val privateKeyByteArray =
            concatKeysByteArray.sliceArray(0 until (concatKeysByteArray.size / 2))
        val publicKeyByteArray =
            concatKeysByteArray.sliceArray((concatKeysByteArray.size / 2) until concatKeysByteArray.size)

        return PublicKey(hexEncoder.encode(privateKeyByteArray)) to
                PrivateKey(hexEncoder.encode(publicKeyByteArray))
    }
}