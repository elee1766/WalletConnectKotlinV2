package org.walletconnect.walletconnectv2.crypto.managers

import com.goterl.lazysodium.utils.HexMessageEncoder
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.CryptoManager
import org.walletconnect.walletconnectv2.crypto.KeyChain
import org.walletconnect.walletconnectv2.crypto.data.Key
import org.walletconnect.walletconnectv2.crypto.data.PrivateKey
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import org.walletconnect.walletconnectv2.util.Utils.bytesToHex
import org.walletconnect.walletconnectv2.util.Utils.toBytes
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.XECPublicKey
import java.security.spec.*
import java.security.spec.NamedParameterSpec.X25519
import javax.crypto.KeyAgreement

class WalletConnectCryptoManager(private val keyChain: KeyChain) : CryptoManager {

    var localPublicKey: PublicKey = PublicKey("")
    lateinit var localPrivateKey: java.security.PrivateKey

    override fun generateKeyPair(): PublicKey {
//        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance("XDH")//X25519.name) //getInstance("XDH"), .getInstance("DH")
//        keyPairGenerator.initialize(ECGenParameterSpec(X25519.name))//NamedParameterSpec("EC"))//NamedParameterSpec(X25519.name))

        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance("XDH")
        keyPairGenerator.initialize(NamedParameterSpec("X25519"))

        val keyPair = keyPairGenerator.generateKeyPair()

        localPublicKey = PublicKey(keyPair.public.encoded.bytesToHex())
        localPrivateKey = keyPair.private//PrivateKey(keyPair.private.encoded.bytesToHex())

        println("PrivateKey1: ${keyPair.private.encoded}; ${keyPair.private.encoded.bytesToHex()} PublicKey1: ${keyPair.public.encoded}; ${keyPair.public.encoded.bytesToHex()}")

//        setKeyPair(publicKey, privateKey)

        return localPublicKey
    }

    override fun generateSharedKey(
        selfPublic: PublicKey,
        peerPublic: PublicKey,
        overrideTopic: String?
    ): Topic {

//        val (publicKey, privateKey) = getKeyPair(selfPublic)
        println("PrivateKey2: $localPrivateKey; PublicKey2: $localPublicKey")

//        println("PrivateKey Self Bytes: ${privateKey.keyAsHex.toBytes()}")

        val keyFactory: KeyFactory = KeyFactory.getInstance("XDH")//X25519.name)

        val peerPublicSpec = XECPublicKeySpec(NamedParameterSpec("X25519"), BigInteger(peerPublic.keyAsHex.toBytes()))//X509EncodedKeySpec(peerPublic.keyAsHex.toByteArray())
//        val selfPrivateSpec =  XECPrivateKeySpec(NamedParameterSpec("X25519"), localPrivateKey.encoded)//localPrivateKey.keyAsHex.toBytes())//X509EncodedKeySpec(privateKey.keyAsHex.toByteArray())

        val peerPublicKey = keyFactory.generatePublic(peerPublicSpec)
//        val selfPrivateKey = keyFactory.generatePrivate(selfPrivateSpec)

        val keyAgreement: KeyAgreement = KeyAgreement.getInstance("XDH")//getInstance(X25519.name)//KeyAgreement.getInstance("ECDH")
        keyAgreement.init(localPrivateKey)
        keyAgreement.doPhase(peerPublicKey, true)

        val secret: ByteArray = keyAgreement.generateSecret()
        val secretString: String = secret.bytesToHex()

        println("1 Secret Bytes: $secret; Secret String: $secretString")

        return setEncryptionKeys(secretString, localPublicKey, overrideTopic)
    }

    private fun setEncryptionKeys(
        sharedKey: String,
        selfPublicKey: PublicKey,
        overrideTopic: String?
    ): Topic {

        val sharedBytes = sharedKey.toBytes()

        println("2 Secret Bytes: $sharedBytes; Secret String: $sharedKey")

        val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-256")
        val hashedBytes: ByteArray = messageDigest.digest(sharedBytes)

        val topic = Topic(overrideTopic ?: hashedBytes.bytesToHex())

        println("Topic B: ${topic.topicValue}")

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