package org.helllabs.android.xmp.compose

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.navkey.NavKeyRoot
import org.helllabs.android.xmp.compose.ui.preferences.AboutScreen
import org.helllabs.android.xmp.compose.ui.preferences.FormatsScreen
import org.helllabs.android.xmp.compose.ui.preferences.PreferencesScreen

@Composable
fun RootNavigation() {
    val rootBackStack = rememberNavBackStack(NavKeyRoot.Main)

    val snackBarHostState = retain { SnackbarHostState() }

    NavDisplay(
        backStack = rootBackStack,
        onBack = { rootBackStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            /* Playlist, Explorer, Download */
            entry<NavKeyRoot.Main> {
                MainNavigation(
                    snackBarHostState = snackBarHostState,
                    onSettings = {
                        val screen = NavKeyRoot.Settings
                        rootBackStack.add(screen)
                    },
                    onTextClick = {
                        TODO()
                    }
                )
            }

            /* Settings */
            entry<NavKeyRoot.Settings> {
                PreferencesScreen(
                    snackBarHostState = snackBarHostState,
                    onBack = { rootBackStack.removeLastOrNull() },
                    onFormats = {
                        val screen = NavKeyRoot.SettingsFormats
                        rootBackStack.add(screen)
                    },
                    onAbout = {
                        val screen = NavKeyRoot.SettingsAbout
                        rootBackStack.add(screen)
                    },
                )
            }
            entry<NavKeyRoot.SettingsAbout> {
                val versionName by remember { mutableStateOf(BuildConfig.VERSION_NAME) }
                val versionCode by remember { mutableIntStateOf(BuildConfig.VERSION_CODE) }
                val xmpVersion by remember { mutableStateOf(Xmp.getVersion()) }
                AboutScreen(
                    buildVersionName = versionName,
                    buildVersionCode = versionCode,
                    libVersion = xmpVersion,
                    onBack = { rootBackStack.removeLastOrNull() },
                )
            }
            entry<NavKeyRoot.SettingsFormats> {
                val formats by remember { mutableStateOf(Xmp.formats) }
                FormatsScreen(
                    snackBarHostState = snackBarHostState,
                    formatsList = formats,
                    onBack = { rootBackStack.removeLastOrNull() },
                )
            }

            /* Player */
            // entry<NavKeyRoot.Player> {
            // }
        }
    )
}
