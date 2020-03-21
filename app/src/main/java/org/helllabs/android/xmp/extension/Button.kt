package org.helllabs.android.xmp.extension

import android.view.View
import com.google.android.material.button.MaterialButton

fun MaterialButton.click(l: (v: View) -> Unit) {
    this.setOnClickListener(l)
}

fun MaterialButton.hide() {
    this.visibility = View.GONE
}

fun MaterialButton.show() {
    this.visibility = View.VISIBLE
}
