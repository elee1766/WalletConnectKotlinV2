package org.walletconnect.walletconnectv2.crypto

import org.walletconnect.walletconnectv2.crypto.data.EncryptionPayload
import org.walletconnect.walletconnectv2.crypto.data.PublicKey

interface Codec {
    fun encrypt(message: String, sharedKey: String, selfPublicKey: PublicKey): EncryptionPayload

    fun decrypt(payload: EncryptionPayload, sharedKey: String, publicKey: PublicKey): String
}