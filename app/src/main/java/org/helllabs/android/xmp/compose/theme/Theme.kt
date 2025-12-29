package org.helllabs.android.xmp.compose.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

@Composable
fun XmpTheme(
    useDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = rememberDynamicColorScheme(
        seedColor = seed,
        primary = seed,
        secondary = Color(0xFF4A90BA),
        isDark = useDarkTheme,
        isAmoled = false, // Maybe Pure-Dark mode?
        style = PaletteStyle.Expressive,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
