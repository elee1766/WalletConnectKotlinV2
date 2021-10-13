package org.walletconnect.walletconnectv2.crypto

import org.walletconnect.walletconnectv2.crypto.data.EncryptionPayload

interface Codec {
    fun encrypt(message: String, sharedKey: String): EncryptionPayload

    fun decrypt(payload: EncryptionPayload, sharedKey: String): String
}