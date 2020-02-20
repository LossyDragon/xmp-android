package org.helllabs.android.xmp.extension

import android.graphics.Typeface
import android.widget.TextView

fun TextView.normal() {
    this.setTypeface(typeface, Typeface.NORMAL)
}

fun TextView.italic() {
    this.setTypeface(typeface, Typeface.ITALIC)
}
