package org.walletconnect.walletconnectv2.crypto.data

data class EncryptionPayload(
    val iv : String,
    val publicKey: PublicKey,
    val mac: String,
    val cipherText: String
)
