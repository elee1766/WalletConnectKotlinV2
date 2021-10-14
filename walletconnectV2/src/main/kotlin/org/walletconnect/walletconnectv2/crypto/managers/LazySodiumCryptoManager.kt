package org.walletconnect.walletconnectv2.crypto.managers

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.goterl.lazysodium.utils.HexMessageEncoder
import com.goterl.lazysodium.utils.Key
import com.goterl.lazysodium.utils.LibraryLoader
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.CryptoManager
import org.walletconnect.walletconnectv2.crypto.KeyChain
import org.walletconnect.walletconnectv2.crypto.data.PrivateKey
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import org.walletconnect.walletconnectv2.util.Utils.toBytes
import org.walletconnect.walletconnectv2.util.Utils.toHex
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.interfaces.XECPrivateKey
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.NamedParameterSpec
import java.security.spec.NamedParameterSpec.X25519
import java.security.spec.XECPrivateKeySpec
import javax.crypto.KeyAgreement
import org.walletconnect.walletconnectv2.crypto.data.Key as WCKey


class LazySodiumCryptoManager(private val keyChain: KeyChain) : CryptoManager {
    private val lazySodium =
        LazySodiumJava(SodiumJava(LibraryLoader.Mode.PREFER_BUNDLED), StandardCharsets.UTF_8)

    override fun hasKeys(tag: String): Boolean {
        return keyChain.getKey(tag).isNotBlank()
    }

    override fun generateKeyPair(): PublicKey {
        val lsKeyPair = lazySodium.cryptoSignKeypair()
        val curve25519KeyPair = lazySodium.convertKeyPairEd25519ToCurve25519(lsKeyPair)
        val (publicKey, privateKey) = curve25519KeyPair.let { keyPair ->
            PublicKey(keyPair.publicKey.asHexString) to PrivateKey(keyPair.secretKey.asHexString)
        }

//        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(X25519.name)
//        keyPairGenerator.initialize(NamedParameterSpec(X25519.name))
//        val keyPair = keyPairGenerator.generateKeyPair()
//        val publicKey = PublicKey(keyPair.public.encoded.toHex())
//        val privateKey = PrivateKey(keyPair.private.encoded.toHex())

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

        val keyPair = KeyPair(peer.toKey(), privateKey.toKey())

//        val sharedKey = lazySodium.cryptoBoxBeforeNm(keyPair)

//        val keyPair = java.security.KeyPair(peer, privateKey)

        val keyAgreement: KeyAgreement = KeyAgreement.getInstance("X25519")
        val paramSpec = NamedParameterSpec("X25519")
        val xdhc = XECPrivateKeySpec(paramSpec, privateKey.keyAsHex.toBytes())

        val keyFactory: KeyFactory = KeyFactory.getInstance("XDH")

        keyAgreement.init(keyFactory.generatePrivate(xdhc))//SecretKeySpec(keyPair.secretKey.asBytes, "X25519"))
        keyAgreement.doPhase(peer, false)//SecretKeySpec(keyPair.publicKey.asBytes, "X25519"), true)

        val secret: ByteArray = keyAgreement.generateSecret()
        val secretString: String = secret.toHex()

        println("Secret Bytes: $secret; Secret String: $secretString")

        return setEncryptionKeys(secretString, publicKey, overrideTopic)
    }

    internal fun setEncryptionKeys(
        sharedKey: String,
        selfPublicKey: PublicKey,
        overrideTopic: String?
    ): Topic {
        val topic = Topic(overrideTopic ?: lazySodium.cryptoHashSha256(sharedKey))
        val sharedKeyObject = object : WCKey {
            override val keyAsHex: String = sharedKey
        }
        val keys = concatKeys(sharedKeyObject, selfPublicKey)
        keyChain.setKey(topic.topicValue, keys)

        return topic
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
        val privateKeyByteArray =
            concatKeysByteArray.sliceArray(0 until (concatKeysByteArray.size / 2))
        val publicKeyByteArray =
            concatKeysByteArray.sliceArray((concatKeysByteArray.size / 2) until concatKeysByteArray.size)

        return PublicKey(hexEncoder.encode(privateKeyByteArray)) to
                PrivateKey(hexEncoder.encode(publicKeyByteArray))
    }

    private fun WCKey.toKey(): Key {
        return Key.fromHexString(keyAsHex)
    }
}