package org.helllabs.android.xmp.util

import android.content.Context
import android.text.Spanned
import androidx.appcompat.app.AppCompatDelegate
import org.helllabs.android.xmp.extension.fromHtml
import org.helllabs.android.xmp.extension.isAtLeastL
import org.helllabs.android.xmp.extension.isAtLeastP
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Get Changelog from html file
 */
fun Context.getChangelog(): Spanned {
    val changelog = resources.assets.open("changelog.html")
    val total = StringBuilder()

    BufferedReader(InputStreamReader(changelog)).forEachLine {
        total.append(it)
    }

    return fromHtml(total.toString())
}

/**
 * DayNight Theme
 */
const val THEME_DEFAULT = "default"
const val THEME_LIGHT = "light"
const val THEME_DARK = "dark"
fun applyTheme(theme: String) {

    Log.d("Utils.kt", "Theme: $theme")

    val mode = when (theme) {
        THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
        else -> {
            when {
                isAtLeastP() -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                isAtLeastL() -> AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                else -> AppCompatDelegate.MODE_NIGHT_NO
            }
        }
    }
    AppCompatDelegate.setDefaultNightMode(mode)
}
