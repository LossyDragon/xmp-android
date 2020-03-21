package org.helllabs.android.xmp.extension

import android.view.View

fun View.click(l: (v: View) -> Unit) {
    this.setOnClickListener(l)
}

fun View.longClick(l: (v: View) -> Boolean) {
    this.setOnLongClickListener(l)
}

fun View.isVisible(): Boolean = this.visibility == View.VISIBLE
