package org.helllabs.android.xmp.extension

import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Html
import android.text.Spanned

fun isAtLeastP() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
fun isAtLeastL() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
fun isAtLeastN() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
fun isAtLeastO() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
fun isAtMostN() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N

// More elegant way to call Intent(context, class)
fun Context.intent(cls: Class<*>): Intent = Intent(this, cls)

// One stop function for any related Html.fromHtml() function
fun fromHtml(string: String?): Spanned {
    return if (isAtLeastN()) {
        Html.fromHtml(string, Html.FROM_HTML_MODE_LEGACY)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(string)
    }
}
