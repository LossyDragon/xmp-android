package org.helllabs.android.xmp.util

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.preferences.PrefManager

fun Context.showChangeLog() {
    val versionCode = BuildConfig.VERSION_CODE
    val lastViewed = PrefManager.changelogVersion

    if (lastViewed < versionCode) {
        val view = LayoutInflater.from(this).inflate(R.layout.changelog, null)
        AlertDialog.Builder(this).apply {
            setTitle("Changelog")
            setIcon(android.R.drawable.ic_menu_info_details)
            setView(view)
            setNegativeButton("Dismiss") { _: DialogInterface?, _: Int ->
                PrefManager.changelogVersion = versionCode
            }.show()
        }
    }
}
