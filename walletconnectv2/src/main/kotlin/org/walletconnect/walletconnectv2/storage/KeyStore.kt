package org.walletconnect.walletconnectv2.storage

import org.walletconnect.walletconnectv2.common.Topic
import org.walletconnect.walletconnectv2.crypto.data.Key

interface KeyStore {
    fun setKey(key: String, value: String)
    fun getKey(tag: String): Key
    fun getKeyAgreement(topic: Topic): Pair<Key, Key>
    fun deleteKeys(topic: Topic)
}