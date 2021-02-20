package org.helllabs.android.xmp.util

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Html
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat

/**
 * API level helpers
 */
val isAtLeastM: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
val isAtLeastN: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

/**
 * Resource helpers
 */
inline fun <reified T : Resources> T.drawable(@DrawableRes res: Int): Drawable? =
    ResourcesCompat.getDrawable(this, res, null)

inline fun <reified T : Resources> T.color(@ColorRes res: Int): Int {
    return if (isAtLeastM) {
        this.getColor(res, null)
    } else {
        @Suppress("DEPRECATION")
        this.getColor(res)
    }
}

/**
 * Format helpers
 */
fun String?.asHtml(): String {
    if (this.isNullOrEmpty()) {
        return ""
    }
    return if (isAtLeastN) {
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(this).toString()
    }
}
