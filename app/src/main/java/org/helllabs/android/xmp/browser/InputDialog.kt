package org.helllabs.android.xmp.browser

import android.app.AlertDialog
import android.content.Context
import android.text.method.SingleLineTransformationMethod
import android.widget.EditText
import android.widget.LinearLayout

class InputDialog(context: Context) : AlertDialog.Builder(context) {
    val input: EditText

    init {
        val scale = context.resources.displayMetrics.density
        val layout = LinearLayout(context)
        val pad = (scale * 6).toInt()
        layout.setPadding(pad, pad, pad, pad)
        input = EditText(context)
        input.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        input.transformationMethod = SingleLineTransformationMethod()
        layout.addView(input)
        setView(layout)
    }
}
