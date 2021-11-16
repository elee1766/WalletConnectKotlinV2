package org.walletconnect.walletconnectv2.storage

import android.content.SharedPreferences
import com.goterl.lazysodium.utils.HexMessageEncoder
import org.walletconnect.walletconnectv2.crypto.data.Key
import org.walletconnect.walletconnectv2.util.empty

class KeyChain(private val sharedPreferences: SharedPreferences) : KeyStore {

    override fun setKey(tag: String, key1: Key, key2: Key) {
        with(sharedPreferences.edit()) {
            val keys = concatKeys(key1, key2)
            putString(tag, keys)
            commit()
        }
    }

    override fun getKeys(tag: String): Pair<String, String> {
        with(sharedPreferences) {
            val concatKeys = getString(tag, String.empty) ?: String.empty
            return splitKeys(concatKeys)
        }
    }

    override fun deleteKeys(tag: String) {

        //TODO get public key under the topic and delete private key for given session
        //TODO delete the shared key with public key

        with(sharedPreferences.edit()) {
            remove(tag)
            commit()
        }
    }

    private fun concatKeys(keyA: Key, keyB: Key): String = with(HexMessageEncoder()) {
        encode(decode(keyA.keyAsHex) + decode(keyB.keyAsHex))
    }

    private fun splitKeys(concatKeys: String): Pair<String, String> = with(HexMessageEncoder()) {
        val concatKeysByteArray = decode(concatKeys)
        val privateKeyByteArray = concatKeysByteArray.sliceArray(0 until (concatKeysByteArray.size / 2))
        val publicKeyByteArray = concatKeysByteArray.sliceArray((concatKeysByteArray.size / 2) until concatKeysByteArray.size)
        return encode(privateKeyByteArray) to encode(publicKeyByteArray)
    }
}