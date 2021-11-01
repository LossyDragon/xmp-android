package org.helllabs.android.xmp.ui.theme

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.google.accompanist.insets.ExperimentalAnimatedInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.systemuicontroller.rememberSystemUiController

val XmpLightThemeColors = lightColorScheme(
    primary = darkPrimary,
    secondary = darkAccent,
)

val XmpDarkThemeColors = darkColorScheme(
    primary = darkPrimary,
    secondary = darkAccent,
)

val LightThemeColors = lightColors(
    primary = darkPrimary,
    primaryVariant = darkPrimaryDark,
    secondary = darkAccent,
    secondaryVariant = darkAccent,
)

val DarkThemeColors = darkColors(
    primary = darkPrimary,
    primaryVariant = darkPrimaryDark,
    secondary = darkAccent,
    secondaryVariant = darkAccent,
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

@OptIn(ExperimentalAnimatedInsets::class)
@Composable
fun XmpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    onlyStyleStatusBar: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = if (darkTheme) DarkThemeColors else LightThemeColors,
        typography = MaterialTheme.typography,
        shapes = Shapes,
        content = {
            val systemUiController = rememberSystemUiController()
            val useDarkIcons = MaterialTheme.colors.isLight
            val backgroundColor = MaterialTheme.colors.background.copy(alpha = .75f)

            SideEffect {
                if (onlyStyleStatusBar) {
                    with(systemUiController) {
                        setStatusBarColor(
                            color = backgroundColor,
                            darkIcons = useDarkIcons
                        )
                        setNavigationBarColor(color = sectionBackgroundDark)
                    }
                } else {
                    systemUiController.setSystemBarsColor(
                        color = backgroundColor,
                        darkIcons = useDarkIcons
                    )
                }
            }

            ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
                content()
            }
        },
    )
}
