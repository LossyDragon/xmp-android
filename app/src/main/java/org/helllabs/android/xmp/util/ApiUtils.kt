package org.helllabs.android.xmp.util

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Html
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView

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
        getColor(res, null)
    } else {
        @Suppress("DEPRECATION")
        getColor(res)
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
    visibility = View.GONE
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.click(l: (v: View) -> Unit) {
    setOnClickListener(l)
}

fun View.longClick(l: (v: View) -> Boolean) {
    setOnLongClickListener(l)
}

/**
 * setOnItemTouchListener(
 * onInterceptTouchEvent = { rv, e -> }
 * onTouchEvent = { rv, e -> }
 * onRequestDisallowInterceptTouchEvent = { disallowIntercept -> }
 * )
 */
fun RecyclerView.setOnItemTouchListener(
    onInterceptTouchEvent: ((rv: RecyclerView, e: MotionEvent) -> Boolean)? = null,
    onTouchEvent: ((rv: RecyclerView, e: MotionEvent) -> Unit)? = null,
    onRequestDisallowInterceptTouchEvent: ((disallowIntercept: Boolean) -> Unit)? = null
): RecyclerView.OnItemTouchListener {
    val listener = object : RecyclerView.OnItemTouchListener {
        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            return onInterceptTouchEvent?.invoke(rv, e) ?: false
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
            onTouchEvent?.invoke(rv, e)
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            onRequestDisallowInterceptTouchEvent?.invoke(disallowIntercept)
        }
    }
    addOnItemTouchListener(listener)
    return listener
}
