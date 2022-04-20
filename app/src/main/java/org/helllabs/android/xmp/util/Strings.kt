package org.helllabs.android.xmp.util

import android.text.Html
import android.text.Spanned
import androidx.core.text.toSpanned
import java.util.*

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
