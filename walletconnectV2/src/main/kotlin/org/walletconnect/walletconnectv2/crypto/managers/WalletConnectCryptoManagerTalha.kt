package org.walletconnect.walletconnectv2.crypto.managers

import com.goterl.lazysodium.exceptions.SodiumException
import com.goterl.lazysodium.interfaces.Hash
import com.goterl.lazysodium.utils.HexMessageEncoder
import com.goterl.lazysodium.utils.KeyPair
import okhttp3.internal.and
import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.CryptoManager
import org.walletconnect.walletconnectv2.crypto.KeyChain
import org.walletconnect.walletconnectv2.crypto.data.Key
import org.walletconnect.walletconnectv2.crypto.data.PrivateKey
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import java.lang.StringBuilder
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.spec.KeySpec
import javax.crypto.KeyAgreement

//class WalletConnectCryptoManagerTalha(private val keyChain: KeyChain) : CryptoManager {
//
//    override fun hasKeys(tag: String): Boolean {
//        return keyChain.getKey(tag).isNotBlank()
//    }
//
//    override fun generateKeyPair(): PublicKey {
//        val kpg = KeyPairGenerator.getInstance("X25519")
//        val keyPair = kpg.generateKeyPair()
//
//        val publicKey = PublicKey(bytesToHex(keyPair.public.encoded))
//        val privateKey = PrivateKey(bytesToHex(keyPair.private.encoded))
//
//        setKeyPair(publicKey, privateKey)
//
//        return publicKey
//    }
//
//    override fun generateSharedKey(self: PublicKey, peer: PublicKey, overrideTopic: String?): Topic {
//        val (publicKey, privateKey) = getKeyPair(self)
//        val keyPair = KeyPair(privateKey.toKey(), peer.toKey())
//        val sharedKey = lazySodium.cryptoBoxBeforeNm(keyPair)
//
//        val kf = KeyFactory.getInstance("X25519")
//        kf.
//        return setEncryptionKeys(sharedKey, publicKey, overrideTopic)
//    }
//
//    internal fun setEncryptionKeys(sharedKey: String, selfPublicKey: PublicKey, overrideTopic: String?): Topic {
//        val topic = Topic(overrideTopic ?: cryptoHashSha256(sharedKey))
//        val sharedKeyObject = object: Key {
//            override val keyAsHex: String = sharedKey
//        }
//        val keys = concatKeys(sharedKeyObject, selfPublicKey)
//
//        keyChain.setKey(topic.topicValue, keys)
//
//        return topic
//    }
//
//    private fun cryptoHashSha256(message: String): String {
//        val digest = MessageDigest.getInstance("SHA-256")
//        val msgBytes: ByteArray = message.toByteArray()
//        val hashedBytes = digest.digest(msgBytes)
//
//        return bytesToHex(hashedBytes)
//    }
//
//    internal fun setKeyPair(publicKey: PublicKey, privateKey: PrivateKey) {
//        val keys = concatKeys(publicKey, privateKey)
//
//        keyChain.setKey(publicKey.keyAsHex, keys)
//    }
//
//    internal fun getKeyPair(wcKey: Key): Pair<PublicKey, PrivateKey> {
//        val storageKey: String = keyChain.getKey(wcKey.keyAsHex)
//
//        return splitKeys(storageKey)
//    }
//
//    internal fun concatKeys(keyA: Key, keyB: Key): String {
//        return bytesToHex(hexToBytes(keyA.keyAsHex) + hexToBytes(keyB.keyAsHex))
//    }
//
//    internal fun splitKeys(concatKeys: String): Pair<PublicKey, PrivateKey> {
//        val concatKeysByteArray = hexToBytes(concatKeys)
//        val privateKeyByteArray = concatKeysByteArray.sliceArray(0 until (concatKeysByteArray.size / 2))
//        val publicKeyByteArray = concatKeysByteArray.sliceArray((concatKeysByteArray.size / 2) until concatKeysByteArray.size)
//
//        return PublicKey(bytesToHex(privateKeyByteArray)) to PrivateKey(bytesToHex(publicKeyByteArray))
//    }
//
//    private fun bytesToHex(bytes: ByteArray): String {
//        val hexChars = CharArray(bytes.size * 2)
//
//        for (j in bytes.indices) {
//            val v: Int = bytes.get(j) and 0xFF
//            hexChars[j * 2] = hexArray[v ushr 4]
//            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
//        }
//
//        return String(hexChars)
//    }
//
//    private fun bytesToHex2(hash: ByteArray): String? {
//        val hexString = StringBuilder(2 * hash.size)
//
//        for (i in hash.indices) {
//            val hex = Integer.toHexString(0xff and hash[i].toInt())
//            if (hex.length == 1) {
//                hexString.append('0')
//            }
//            hexString.append(hex)
//        }
//        return hexString.toString()
//    }
//
//    private fun hexToBytes(s: String): ByteArray {
//        val len = s.length
//        val data = ByteArray(len / 2)
//        var i = 0
//
//        while (i < len) {
//            data[i / 2] = ((Character.digit(s[i], 16) shl 4)
//                    + Character.digit(s[i + 1], 16)).toByte()
//            i += 2
//        }
//
//        return data
//    }
//
//    private companion object {
//        val hexArray = "0123456789ABCDEF".toCharArray()
//    }
//}