package org.walletconnect.walletconnectv2.crypto.data

data class EncryptionPayload(
    val iv : String,
    val publicKey: String,
    val mac: String,
    val cipherText: String
)
