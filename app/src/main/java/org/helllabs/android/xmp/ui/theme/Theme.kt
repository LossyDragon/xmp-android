package org.helllabs.android.xmp.ui.theme

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

val XmpLightThemeColors = lightColorScheme(
    primary = darkPrimary,
    secondary = darkAccent,
)

val XmpDarkThemeColors = darkColorScheme(
    primary = darkPrimary,
    secondary = darkAccent,
)

// Accent the "Xmp" part of the text, if we're on the main screen.
@Composable
fun themedText(@StringRes res: Int): AnnotatedString {
    val string = stringResource(id = res)
    return buildAnnotatedString {
        withStyle(style = SpanStyle(color = darkAccent)) {
            append(string.substring(0, 3))
        }

        withStyle(
            style = SpanStyle(
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
            )
        ) {
            append(string.substring(3, string.length))
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun XmpTheme3(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) XmpDarkThemeColors else XmpLightThemeColors

    androidx.compose.material3.MaterialTheme(colorScheme = colorScheme) {
        val rippleIndication = rememberRipple()
        CompositionLocalProvider(
            LocalIndication provides rippleIndication,
            content = content
        )
    }
}
