package org.helllabs.android.xmp.util

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.Spanned
import androidx.appcompat.app.AppCompatDelegate
import java.io.BufferedReader
import java.io.InputStreamReader

fun isAtLeastP() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
fun isAtLeastL() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
fun isAtLeastN() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

/**
 * Get Changelog from html file
 */
fun Context.getChangelog(): Spanned {
    val inputStream = InputStreamReader(resources.assets.open("changelog.html"))
    val total = StringBuilder()

    BufferedReader(inputStream).forEachLine {
        total.append(it)
    }

    return if (isAtLeastN()) {
        Html.fromHtml(total.toString(), Html.FROM_HTML_MODE_COMPACT)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(total.toString())
    }
}

/**
 * DayNight Theme
 */
const val THEME_DEFAULT = "default"
const val THEME_LIGHT = "light"
const val THEME_DARK = "dark"
fun applyTheme(theme: String) {

    Log.d("ThemeUtil.kt", "Theme: $theme")

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
