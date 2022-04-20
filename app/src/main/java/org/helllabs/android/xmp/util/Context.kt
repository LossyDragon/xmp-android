package org.helllabs.android.xmp.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import org.helllabs.android.xmp.R

fun Context.launchActivity(intent: Intent) {
    startActivity(intent)
    (this as Activity).overridePendingTransition(
        R.anim.slide_in_right,
        R.anim.slide_out_left
    )
}

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
