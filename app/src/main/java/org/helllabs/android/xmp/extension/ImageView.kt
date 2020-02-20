package org.helllabs.android.xmp.extension

import android.view.View
import android.widget.ImageView

fun ImageView.hide() {
    this.visibility = View.GONE
}

fun ImageView.show() {
    this.visibility = View.VISIBLE
}
