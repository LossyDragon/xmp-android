package org.helllabs.android.xmp.util

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

fun isAtLeastP() = Build.VERSION.SDK_INT >= 28
fun isAtLeastL() = Build.VERSION.SDK_INT >= 21

/**
 * DayNight Theme
 */
const val THEME_DEFAULT = "default"
const val THEME_LIGHT = "light"
const val THEME_DARK = "dark"
fun applyTheme(theme: String) {

    Log.i("ThemeUtil.kt", "Theme: $theme")

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