package com.lossydragon.media3.ui.theme

import androidx.compose.foundation.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.*
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

private val seed = Color(0xFF3460ba)

@Composable
fun XmpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = rememberDynamicColorScheme(
        seedColor = seed,
        primary = seed,
        secondary = Color(0xFF4A90BA),
        isDark = darkTheme,
        isAmoled = false, // Maybe Pure-Dark mode?
        style = PaletteStyle.Expressive,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
