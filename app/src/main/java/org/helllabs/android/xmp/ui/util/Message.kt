package org.helllabs.android.xmp.util

import android.content.Context
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.preferences.PrefManager

inline fun <reified T : Context> T.toast(message: String) =
    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()

inline fun <reified T : Context> T.toast(@StringRes resId: Int) =
    Toast.makeText(applicationContext, this.getString(resId), Toast.LENGTH_SHORT).show()

fun Context.dialogMessage(
    lifecycleOwner: LifecycleOwner,
    @StringRes title: Int = R.string.error,
    message: String,
    block: () -> Unit? = { }
) {
    MaterialDialog(this).show {
        lifecycleOwner(lifecycleOwner)
        title(title)
        message(text = message)
        positiveButton(R.string.ok) {
            block.invoke()
        }
    }
}

fun Context.yesNoDialog(
    lifecycleOwner: LifecycleOwner,
    title: String,
    message: String,
    block: () -> Unit
) {
    MaterialDialog(this).show {
        lifecycleOwner(lifecycleOwner)
        title(text = title)
        message(text = message)
        positiveButton(R.string.yes) { block() }
        negativeButton(R.string.no)
    }
}

fun Context.showChangeLog(lifecycleOwner: LifecycleOwner) {
    if (PrefManager.changelogVersion < BuildConfig.VERSION_CODE) {
        MaterialDialog(this).show {
            lifecycleOwner(lifecycleOwner)
            customView(R.layout.layout_changelog)
            getCustomView().findViewById<TextView>(R.id.changelog_version_title).apply {
                text = getString(R.string.changelog_title, BuildConfig.VERSION_NAME)
            }
            cancelOnTouchOutside(false)
            title(text = "Changelog")
            positiveButton(text = "Dismiss") {
                PrefManager.changelogVersion = BuildConfig.VERSION_CODE
            }
        }
    }
}
