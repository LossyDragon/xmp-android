package org.helllabs.android.xmp.util

import android.app.Activity
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.preferences.Preferences

fun Activity.showChangeLog() {

    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val lastViewed = prefs.getInt(Preferences.CHANGELOG_VERSION, 0)
    val versionCode = BuildConfig.VERSION_CODE

    if (lastViewed < versionCode) {
        MaterialDialog(this).show {
            title(text = "Changelog")
            customView(R.layout.dialog_changelog)
            positiveButton(text = "Dismiss") {
                val editor = prefs.edit()
                editor.putInt(Preferences.CHANGELOG_VERSION, versionCode)
                editor.apply()
            }
        }
    }
}