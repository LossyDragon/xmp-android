package org.helllabs.android.xmp.util

import android.util.Log

inline fun <reified T : Any> T.logD(message: String) =
    Log.d("Xmp", "[${this::class.java.simpleName}] $message")

inline fun <reified T : Any> T.logI(message: String) =
    Log.i("Xmp", "[${this::class.java.simpleName}] $message")

inline fun <reified T : Any> T.logW(message: String) =
    Log.w("Xmp", "[${this::class.java.simpleName}] $message")

inline fun <reified T : Any> T.logE(message: String) =
    Log.e("Xmp", "[${this::class.java.simpleName}] $message")
