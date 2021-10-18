package org.walletconnect.walletconnectv2.crypto

import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.data.EncryptionPayload
import org.walletconnect.walletconnectv2.crypto.data.PublicKey

interface CryptoManager {

    fun hasKeys(tag: String): Boolean

    fun generateKeyPair(): PublicKey

    fun generateSettledTopic(
        selfPublic: PublicKey,
        peerPublic: PublicKey,
        overrideTopic: String? = null
    ): Topic

    fun generateSharedKey(selfPublic: PublicKey, peerPublic: PublicKey): String

    fun encrypt(pubKey: ByteArray, message: ByteArray, signature: ByteArray): ByteArray
    fun createShared(bytesPeerPublic: ByteArray, bytesSelfPrivate: ByteArray): ByteArray
}