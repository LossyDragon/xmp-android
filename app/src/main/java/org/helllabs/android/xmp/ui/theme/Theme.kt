package org.helllabs.android.xmp.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.google.accompanist.insets.ExperimentalAnimatedInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.systemuicontroller.rememberSystemUiController

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

        append(string.substring(3, string.length))
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
