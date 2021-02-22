package org.helllabs.android.xmp.util

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.afollestad.materialdialogs.MaterialDialog
import org.helllabs.android.xmp.R

inline fun <reified T : Context> T.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

inline fun <reified T : Context> T.toast(@StringRes resId: Int) =
    Toast.makeText(this, this.getString(resId), Toast.LENGTH_SHORT).show()

fun Activity.fatalError(message: String) {
    MaterialDialog(this).show {
        title(R.string.error)
        message(text = message)
        positiveButton(R.string.exit) {
            finish()
        }
    }
}

fun Activity.generalError(message: String) {
    MaterialDialog(this).show {
        title(R.string.error)
        message(text = message)
        positiveButton(R.string.dismiss)
    }
}

fun Activity.yesNoDialog(title: String, message: String, block: () -> Unit) {
    MaterialDialog(this).show {
        title(text = title)
        message(text = message)
        positiveButton(R.string.yes) { block() }
        negativeButton(R.string.no)
    }
}
