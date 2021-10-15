package org.walletconnect.walletconnectv2.util

import okhttp3.internal.and
import org.walletconnect.walletconnectv2.util.Utils.and
import java.lang.System.currentTimeMillis

object Utils {

    fun generateId(): Int = (currentTimeMillis() + (0..100).random()).toInt()

    fun ByteArray.bytesToHex(): String {
        val hexString = StringBuilder(2 * this.size)
        for (i in this.indices) {
            val hex = Integer.toHexString(0xff and this[i].toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }

    private fun ByteArray.toHexString(): String {
        val stringBuilder = StringBuilder()
        for (i in 0 until 0 + this.size) {
            stringBuilder.append(String.format("%02x", this[i] and 0xFF))
        }
        return stringBuilder.toString()
    }

    private infix fun Byte.and(mask: Int): Int = toInt() and mask

    private fun String.toByteArray(): ByteArray {
        val cleanInput: String = cleanHexPrefix(this)
        val len = cleanInput.length
        if (len == 0) {
            return byteArrayOf()
        }
        val data: ByteArray
        val startIdx: Int
        if (len % 2 != 0) {
            data = ByteArray(len / 2 + 1)
            data[0] = Character.digit(cleanInput[0], 16).toByte()
            startIdx = 1
        } else {
            data = ByteArray(len / 2)
            startIdx = 0
        }
        var i = startIdx
        while (i < len) {
            data[(i + 1) / 2] = ((Character.digit(cleanInput[i], 16) shl 4)
                    + Character.digit(cleanInput[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun cleanHexPrefix(input: String): String =
        if (containsHexPrefix(input)) {
            input.substring(2)
        } else {
            input
        }

    private fun containsHexPrefix(input: String): Boolean = input.startsWith("0x")

//    fun String.toBytes(): ByteArray {
//        val len = this.length
//        val data = ByteArray(len / 2)
//        var i = 0
//        while (i < len) {
//            data[i / 2] = ((Character.digit(this[i], 16) shl 4)
//                    + Character.digit(this[i + 1], 16)).toByte()
//            i += 2
//        }
//        return data
//    }
//
//    fun ByteArray.toHex(): String {
//        val hexArray = "0123456789ABCDEF".toCharArray()
//        val hexChars = CharArray(this.size * 2)
//        for (j in this.indices) {
//            val v = this[j] and 0xFF
//            hexChars[j * 2] = hexArray[v ushr 4]
//            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
//        }
//        return String(hexChars)
//    }

//    private fun toHexString(
//        input: ByteArray,
//        length: Int
//    ): String {
//        val stringBuilder = StringBuilder()
//        for (i in 0 until 0 + length) {
//            stringBuilder.append(String.format("%02x", input[i] and 0xFF))
//        }
//        return stringBuilder.toString()
//    }

//    private fun hexStringToByteArray(input: String): ByteArray {
//        val cleanInput: String = cleanHexPrefix(input)
//        val len = cleanInput.length
//        if (len == 0) {
//            return byteArrayOf()
//        }
//        val data: ByteArray
//        val startIdx: Int
//        if (len % 2 != 0) {
//            data = ByteArray(len / 2 + 1)
//            data[0] = Character.digit(cleanInput[0], 16).toByte()
//            startIdx = 1
//        } else {
//            data = ByteArray(len / 2)
//            startIdx = 0
//        }
//        var i = startIdx
//        while (i < len) {
//            data[(i + 1) / 2] = ((Character.digit(cleanInput[i], 16) shl 4)
//                    + Character.digit(cleanInput[i + 1], 16)).toByte()
//            i += 2
//        }
//        return data
//    }
}