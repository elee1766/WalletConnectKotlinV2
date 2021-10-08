package org.walletconnect.walletconnectv2.util

import java.lang.System.currentTimeMillis

fun generateId(): Int = (currentTimeMillis() + (0..100).random()).toInt()