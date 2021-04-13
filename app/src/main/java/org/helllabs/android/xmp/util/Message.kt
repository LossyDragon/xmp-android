package org.helllabs.android.xmp.util

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R

inline fun <reified T : Context> T.toast(message: String) =
    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()

inline fun <reified T : Context> T.toast(@StringRes resId: Int) =
    Toast.makeText(applicationContext, this.getString(resId), Toast.LENGTH_LONG).show()

fun Context.errorDialog(
    owner: LifecycleOwner,
    message: String,
    @StringRes buttonText: Int = R.string.dismiss,
    onConfirm: () -> Unit
) {
    MaterialDialog(this).show {
        lifecycleOwner(owner)
        title(R.string.error)
        message(text = message)
        positiveButton(buttonText) {
            onConfirm()
        }
    }
}

fun Context.yesNoDialog(
    owner: LifecycleOwner,
    @StringRes title: Int,
    message: String,
    @StringRes confirmText: Int = R.string.yes,
    @StringRes dismissText: Int = R.string.no,
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    MaterialDialog(this).show {
        lifecycleOwner(owner)
        title(title)
        message(text = message)
        positiveButton(confirmText) { onConfirm() }
        negativeButton(dismissText) { onDismiss?.invoke() }
    }
}

fun Context.showChangeLog(owner: LifecycleOwner, onConfirm: () -> Unit) {
    val versionCode = BuildConfig.VERSION_CODE
    val lastViewed = PrefManager.changelogVersion

    if (lastViewed < versionCode) {
        MaterialDialog(this).show {
            lifecycleOwner(owner)
            title(text = getString(R.string.changelog_title, BuildConfig.VERSION_NAME))
            message(R.string.changelog_text)
            cancelOnTouchOutside(false)
            positiveButton(R.string.dismiss) {
                PrefManager.changelogVersion = versionCode
                onConfirm()
            }
        }
    } else {
        // We've already showed the changelog, continue executing.
        onConfirm()
    }
}
