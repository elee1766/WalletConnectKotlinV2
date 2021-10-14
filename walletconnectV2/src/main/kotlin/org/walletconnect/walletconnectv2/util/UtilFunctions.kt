package org.walletconnect.walletconnectv2.util

import okhttp3.internal.and
import java.lang.System.currentTimeMillis

object Utils {

    fun generateId(): Int = (currentTimeMillis() + (0..100).random()).toInt()


    fun String.toBytes(): ByteArray {
        val len = this.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(this[i], 16) shl 4)
                    + Character.digit(this[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun ByteArray.toHex(): String {
        val hexArray = "0123456789ABCDEF".toCharArray()
        val hexChars = CharArray(this.size * 2)
        for (j in this.indices) {
            val v = this[j] and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}