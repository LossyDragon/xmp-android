package org.helllabs.android.xmp.presentation.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
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
import com.google.accompanist.systemuicontroller.LocalSystemUiController
import com.google.accompanist.systemuicontroller.rememberAndroidSystemUiController
import org.helllabs.android.xmp.PrefManager

private val LightThemeColors = lightColors(
    primary = darkPrimary,
    primaryVariant = darkPrimaryDark,
    secondary = darkAccent,
    secondaryVariant = darkAccent,
)

private val DarkThemeColors = darkColors(
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

@Composable
fun systemDarkTheme(): Boolean {
    return when (PrefManager.themePref) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
}

@OptIn(ExperimentalAnimatedInsets::class)
@Composable
fun AppTheme(
    isDarkTheme: Boolean = systemDarkTheme(),
    onlyStyleStatusBar: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colors = if (isDarkTheme) DarkThemeColors else LightThemeColors,
        typography = Typography,
        shapes = AppShapes,
        content = {
            val controller = rememberAndroidSystemUiController()
            CompositionLocalProvider(LocalSystemUiController provides controller) {
                ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
                    val systemUiController = LocalSystemUiController.current
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

                    content()
                }
            }
        },
    )
}
