package org.helllabs.android.xmp.util

import android.app.Activity
import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import kotlinx.android.synthetic.main.dialog_changelog.view.*
import kotlinx.android.synthetic.main.dialog_input.view.*
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.preferences.Preferences


fun Activity.fatalError(@StringRes resId: Int? = null, text: String? = null) {
    MaterialDialog(this).show {
        title(R.string.error)
        message(text = resId?.let { getString(it) } ?: text)
        positiveButton(R.string.dismiss) {
            finish()
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

fun Activity.showChangeLog() {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val lastViewed = prefs.getInt(Preferences.CHANGELOG_VERSION, 0)
    val versionCode = BuildConfig.VERSION_CODE
    if (lastViewed < versionCode) {
        val dialog = MaterialDialog(this)
                .title(R.string.changelog)
                .customView(R.layout.dialog_changelog)
                .positiveButton(R.string.dismiss) {
                    val editor = prefs.edit()
                    editor.putInt(Preferences.CHANGELOG_VERSION, versionCode)
                    editor.apply()
                }

        val view = dialog.getCustomView()
        view.text_version.text = String.format(getString(R.string.title_changelog), BuildConfig.VERSION_NAME)
        view.text_changelog.text = getChangelog()
        dialog.show()
    }
}

fun Activity.showChangeDir(prefs: SharedPreferences, runnable: Runnable) {
    val mediaPath = prefs.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH)!!
    val dialog = MaterialDialog(this).customView(R.layout.dialog_input)
    val customView = dialog.getCustomView()

    //TextWatcher
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
            if (value != mediaPath) {
                val editor = prefs.edit()
                editor.putString(Preferences.MEDIA_PATH, value)
                editor.apply()
            }

            runnable.run()
        }
        negativeButton(R.string.cancel)
    }

}
