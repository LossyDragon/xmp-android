package org.helllabs.android.xmp.preferences.about

import android.content.Context
import android.content.pm.PackageManager

object AppInfo {
    fun getVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            var version = packageInfo.versionName
            val end = version.indexOf(' ')
            if (end > 0) {
                version = version.substring(0, end)
            }
            version
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }
}