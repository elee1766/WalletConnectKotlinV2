package org.walletconnect.walletconnectv2.crypto.managers

import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.goterl.lazysodium.utils.HexMessageEncoder
import com.goterl.lazysodium.utils.Key
import com.goterl.lazysodium.utils.KeyPair
import com.goterl.lazysodium.utils.LibraryLoader
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.CryptoManager
import org.walletconnect.walletconnectv2.crypto.KeyChain
import org.walletconnect.walletconnectv2.crypto.data.PrivateKey
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import java.nio.charset.StandardCharsets
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

        setKeyPair(publicKey, privateKey)

        return publicKey
    }

    override fun generateSharedKey(
        selfPublic: PublicKey,
        peerPublic: PublicKey,
        overrideTopic: String?
    ): Topic {
        val (publicKey, privateKey) = getKeyPair(selfPublic)

        val keyPair = KeyPair(privateKey.toKey(), peerPublic.toKey())
        val sharedKey = lazySodium.cryptoBoxBeforeNm(keyPair)

        val sharedKey2 = lazySodium.cryptoKxClientSessionKeys(
            Key.fromHexString(publicKey.keyAsHex),
            Key.fromHexString(privateKey.keyAsHex),
            Key.fromHexString(peerPublic.keyAsHex)
        )


        val sharedKey3 = lazySodium.cryptoScalarMult(Key.fromHexString(privateKey.keyAsHex), Key.fromHexString(peerPublic.keyAsHex))


        println("Shared1: $sharedKey\n Shared2: ${sharedKey2.rxString}\n Shared3: ${sharedKey3.asHexString}\n")

        println(
            "TopicB_1: ${lazySodium.cryptoHashSha256(sharedKey)}\n TopicB_2: ${
                lazySodium.cryptoHashSha256(
                    sharedKey2.rxString
                )
            }; ${lazySodium.cryptoHashSha256(sharedKey2.txString)}\n TopicB_3: ${
                lazySodium.cryptoHashSha256(sharedKey3.asHexString)
            }\n"
        )

        return setEncryptionKeys(sharedKey3.asHexString, publicKey, overrideTopic)
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

        println("Topic B: ${topic.topicValue}")

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