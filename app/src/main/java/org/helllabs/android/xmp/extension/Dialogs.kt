package org.helllabs.android.xmp.extension

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import kotlinx.android.synthetic.main.dialog_changelog.view.*
import kotlinx.android.synthetic.main.dialog_input.view.*
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.util.getChangelog

fun Activity.fatalError(@StringRes resId: Int? = null, text: String? = null) {
    MaterialDialog(this).show {
        title(R.string.error)
        message(text = resId?.let { getString(it) } ?: text)
        positiveButton(R.string.exit) {
            finishAffinity()
        }
    }
}

fun Activity.error(@StringRes resId: Int? = null, text: String? = null) {
    MaterialDialog(this).show {
        title(R.string.error)
        message(text = resId?.let { getString(it) } ?: text)
        positiveButton(R.string.dismiss)
    }
}

fun Activity.toast(@StringRes resId: Int? = null, text: String? = null) {
    Toast.makeText(this, resId?.let { getString(it) } ?: text, Toast.LENGTH_SHORT).show()
}

fun Activity.yesNoDialog(title: String, message: String, callback: (result: Boolean) -> Unit) {
    MaterialDialog(this).show {
        title(text = title)
        message(text = message)
        positiveButton(R.string.yes) { callback(true) }
        negativeButton(R.string.no) { callback(false) }
    }
}

// Show changelog on first time use, and on version upgrades.
fun Activity.showChangeLog(lastViewed: Int, callback: (result: Boolean, versionCode: Int) -> Unit) {
    val versionCode = BuildConfig.VERSION_CODE
    val hasPermission =
            ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED
    if (lastViewed < versionCode && hasPermission) {
        val dialog = MaterialDialog(this)
                .title(R.string.changelog)
                .customView(R.layout.dialog_changelog)
                .cancelOnTouchOutside(false)
                .positiveButton(R.string.dismiss) {
                    callback(true, versionCode)
                }

        val view = dialog.getCustomView()
        view.text_version.text =
                String.format(getString(R.string.title_changelog), BuildConfig.VERSION_NAME)
        view.text_changelog.text = getChangelog()
        dialog.show()
    }
}

fun Activity.showChangeDir(mediaPath: String, callback: (result: Boolean, path: String) -> Unit) {
    val dialog = MaterialDialog(this).customView(R.layout.dialog_input)
    val customView = dialog.getCustomView()

    // TextWatcher
    customView.dialog_input_text.addTextChangedListener(object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            dialog.setActionButtonEnabled(WhichButton.POSITIVE, !s.isNullOrEmpty())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun afterTextChanged(s: Editable?) {}
    })

    dialog.show {
        title(R.string.title_change_directory)
        setActionButtonEnabled(WhichButton.POSITIVE, false)
        cancelOnTouchOutside(true)
        customView.dialog_input_text.setText(mediaPath)
        positiveButton(R.string.ok) {
            val value = getCustomView().dialog_input_text.text.toString()
            callback(true, value)
        }
        negativeButton(R.string.cancel)
    }
}
