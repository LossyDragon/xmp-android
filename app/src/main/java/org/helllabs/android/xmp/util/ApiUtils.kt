package org.helllabs.android.xmp.util

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Html
import android.view.View
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

/**
 * View helpers
 */
fun View.hide() {
    this.visibility = View.GONE
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.click(l: (v: View) -> Unit) {
    this.setOnClickListener(l)
}

fun View.longClick(l: (v: View) -> Boolean) {
    this.setOnLongClickListener(l)
}
