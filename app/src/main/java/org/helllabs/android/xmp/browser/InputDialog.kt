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
        val pad = (scale * 6).toInt()

        input = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            transformationMethod = SingleLineTransformationMethod()
        }

        LinearLayout(context).apply {
            setPadding(pad, pad, pad, pad)
            addView(input)
        }.also {
            setView(it)
        }
    }
}
