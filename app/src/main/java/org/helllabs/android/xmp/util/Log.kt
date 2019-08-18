package org.helllabs.android.xmp.util


object Log {
    private const val TAG = "Xmp"

    fun d(tag: String, message: String) {
        android.util.Log.d(TAG, "[$tag] $message")
    }

    fun i(tag: String, message: String) {
        android.util.Log.i(TAG, "[$tag] $message")
    }

    fun w(tag: String, message: String) {
        android.util.Log.w(TAG, "[$tag] $message")
    }

    fun e(tag: String, message: String) {
        android.util.Log.e(TAG, "[$tag] $message")
    }
}
