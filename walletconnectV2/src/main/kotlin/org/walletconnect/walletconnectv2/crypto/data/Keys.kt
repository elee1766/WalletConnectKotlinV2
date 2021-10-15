package org.walletconnect.walletconnectv2.crypto.data

import org.walletconnect.walletconnectv2.util.Utils.toBytes
import java.security.PrivateKey
import java.security.PublicKey

interface Key {
    val keyAsHex: String
}

@JvmInline
value class PublicKey(override val keyAsHex: String): Key
//    , PublicKey {
//
//    override fun getAlgorithm(): String {
//        return "ECDH"
//    }
//
//    override fun getFormat(): String {
//        TODO("Not yet implemented")
//    }
//
//    override fun getEncoded(): ByteArray {
//       return keyAsHex.toBytes()
//    }
//}

@JvmInline
value class PrivateKey(override val keyAsHex: String): Key
//    , PrivateKey {
//    override fun getAlgorithm(): String {
//        return "ECDH"
//    }
//
//    override fun getFormat(): String {
//        TODO("Not yet implemented")
//    }
//
//    override fun getEncoded(): ByteArray {
//       return keyAsHex.toBytes()
//    }
//}