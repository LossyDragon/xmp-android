package org.helllabs.android.xmp.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.Log
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.text.toSpanned
import java.util.*
import org.helllabs.android.xmp.R

/**
 * Log helpers
 */
inline fun <reified T : Any> T.logD(message: String) =
    Log.d("Xmp", "[${this::class.java.simpleName}] $message")

inline fun <reified T : Any> T.logI(message: String) =
    Log.i("Xmp", "[${this::class.java.simpleName}] $message")

inline fun <reified T : Any> T.logW(message: String) =
    Log.w("Xmp", "[${this::class.java.simpleName}] $message")

inline fun <reified T : Any> T.logE(message: String) =
    Log.e("Xmp", "[${this::class.java.simpleName}] $message")

/**
 * API level helpers
 */
object Api {
    val isAtLeastM: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    val isAtLeastN: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    val isAtLeastO: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    val isAtLeastR: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val isAtLeastS: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

/**
 * Resource helpers
 */
fun Context.getIconBitmap(): Bitmap? {
    // Emu kept crashing with some reference to this
    // AppCompatResources.getDrawable(this, R.drawable.ic_xmp_vector)?.toBitmap()
    val drawable = ContextCompat.getDrawable(this, R.drawable.ic_xmp_vector) ?: return null
    return Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    ).also {
        val canvas = Canvas(it)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
    }
}

inline fun <reified T : Resources> T.color(@ColorRes res: Int): Int {
    return if (Api.isAtLeastM) {
        getColor(res, null)
    } else {
        @Suppress("DEPRECATION")
        getColor(res)
    }
}

/**
 * Format helpers
 */
fun String?.asHtml(): Spanned {
    if (this.isNullOrEmpty()) {
        return "".toSpanned()
    }
    return if (Api.isAtLeastN) {
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(this)
    }
}

fun String.upperCase(): String = this.uppercase(Locale.getDefault())

fun String.toList(): List<String> =
    if (this.isBlank()) emptyList() else listOf(this)

inline fun String?.ifNullOrEmpty(defaultValue: () -> String) =
    if (this.isNullOrBlank()) defaultValue() else this
