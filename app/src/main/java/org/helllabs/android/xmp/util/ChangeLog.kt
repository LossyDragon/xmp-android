package org.helllabs.android.xmp.util

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.view.LayoutInflater
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.preferences.PrefManager

class ChangeLog(private val context: Context) {
    fun show(): Int {
        return try {
            val versionCode = BuildConfig.VERSION_CODE
            val lastViewed = PrefManager.changelogVersion
            if (lastViewed < versionCode) {
                PrefManager.changelogVersion = versionCode
                showLog()
                0
            } else {
                -1
            }
        } catch (e: PackageManager.NameNotFoundException) {
            logW("Unable to get version code")
            -1
        }
    }

    private fun showLog() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.changelog, null)
        AlertDialog.Builder(context)
            .setTitle("Changelog")
            .setIcon(android.R.drawable.ic_menu_info_details)
            .setView(view)
            .setNegativeButton("Dismiss") { _: DialogInterface?, _: Int -> }.show()
    }
}
