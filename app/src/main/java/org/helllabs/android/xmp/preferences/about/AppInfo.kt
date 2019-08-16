package org.helllabs.android.xmp.preferences.about

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.NameNotFoundException

internal object AppInfo {

    fun getVersion(context: Context): String {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName, 0)
            var version = packageInfo.versionName
            val end = version.indexOf(' ')
            if (end > 0) {
                version = version.substring(0, end)
            }

            return version
        } catch (e: NameNotFoundException) {
            return ""
        }

    }
}
