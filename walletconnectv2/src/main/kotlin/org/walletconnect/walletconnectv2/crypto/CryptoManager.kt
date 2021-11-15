package org.walletconnect.walletconnectv2.crypto

import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.data.PublicKey
import org.walletconnect.walletconnectv2.crypto.data.SharedKey

interface CryptoManager {
    fun hasKeys(tag: String): Boolean
    fun generateKeyPair(): PublicKey
    fun generateTopicAndSharedKey(self: PublicKey, peer: PublicKey, overrideTopic: String? = null): Pair<SharedKey, Topic>
    fun getKeyAgreement(topic: Topic): Pair<SharedKey, PublicKey>
    fun setEncryptionKeys(sharedKey: SharedKey, publicKey: PublicKey, topic: Topic)
}