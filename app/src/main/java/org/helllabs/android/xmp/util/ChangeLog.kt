package org.helllabs.android.xmp.util

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.view.LayoutInflater
import androidx.preference.PreferenceManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.preferences.Preferences

class ChangeLog(private val context: Context) {

    fun show(): Int {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

            //Older API's like this deprecated value
            @Suppress("DEPRECATION")
            val versionCode = packageInfo.versionCode

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val lastViewed = prefs.getInt(Preferences.CHANGELOG_VERSION, 0)

            return if (lastViewed < versionCode) {
                val editor = prefs.edit()
                editor.putInt(Preferences.CHANGELOG_VERSION, versionCode)
                editor.apply()
                showLog()
                0
            } else {
                -1
            }
        } catch (e: NameNotFoundException) {
            Log.w(TAG, "Unable to get version code")
            return -1
        }

    }

    @SuppressLint("InflateParams")
    private fun showLog() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.changelog, null)

        AlertDialog.Builder(context)
                .setTitle("Changelog")
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setView(view)
                .setNegativeButton("Dismiss") { _, _ ->
                    // Do nothing
                }.show()
    }

    companion object {
        private const val TAG = "ChangeLog"
    }
}
