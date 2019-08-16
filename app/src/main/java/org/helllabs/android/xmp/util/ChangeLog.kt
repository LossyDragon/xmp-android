package org.helllabs.android.xmp.util

import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.preferences.Preferences

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.NameNotFoundException
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View

class ChangeLog(private val context: Context) {

    fun show(): Int {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = packageInfo.versionCode

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val lastViewed = prefs.getInt(Preferences.CHANGELOG_VERSION, 0)

            if (lastViewed < versionCode) {
                val editor = prefs.edit()
                editor.putInt(Preferences.CHANGELOG_VERSION, versionCode)
                editor.apply()
                showLog()
                return 0
            } else {
                return -1
            }
        } catch (e: NameNotFoundException) {
            Log.w(TAG, "Unable to get version code")
            return -1
        }

    }

    private fun showLog() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.changelog, null)

        AlertDialog.Builder(context)
                .setTitle("Changelog")
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setView(view)
                .setNegativeButton("Dismiss") { dialog, whichButton ->
                    // Do nothing
                }.show()
    }

    companion object {
        private val TAG = "ChangeLog"
    }
}
