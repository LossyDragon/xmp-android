package org.helllabs.android.xmp.util

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.preferences.PrefManager

fun Context.showChangeLog() {
    val versionCode = BuildConfig.VERSION_CODE
    val lastViewed = PrefManager.changelogVersion

    if (lastViewed < versionCode) {
        MaterialDialog(this).show {
            customView(R.layout.layout_changelog)
            cancelOnTouchOutside(false)
            title(text = "Changelog")
            positiveButton(text = "Dismiss") {
                PrefManager.changelogVersion = versionCode
            }
        }
    }
}
