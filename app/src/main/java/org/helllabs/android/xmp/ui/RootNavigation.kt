package org.helllabs.android.xmp.ui

import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.collections.immutable.toPersistentList
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.PlayerActivityLauncher
import org.helllabs.android.xmp.ui.navkey.NavKeyRoot
import org.helllabs.android.xmp.ui.screens.preferences.AboutScreen
import org.helllabs.android.xmp.ui.screens.preferences.FormatsScreen
import org.helllabs.android.xmp.ui.screens.preferences.PreferencesScreen
import org.helllabs.libxmp.Xmp

@Composable
fun RootNavigation(
    snackBarHostState: SnackbarHostState,
    onPlayAll: (List<Uri>, Boolean, Boolean, PlayerActivityLauncher) -> Unit,
    onAddQueue: (List<Uri>, Boolean, Boolean, PlayerActivityLauncher) -> Unit,
    onPlayModule: (List<Uri>, Int, Boolean, Boolean, Boolean, PlayerActivityLauncher) -> Unit,
    onItemClick: (List<Uri>, Int, Boolean, Boolean, PlayerActivityLauncher) -> Unit
) {
    val rootBackStack = rememberNavBackStack(NavKeyRoot.Main)

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
                    onPlayAll = onPlayAll,
                    onAddQueue = onAddQueue,
                    onPlayModule = onPlayModule,
                    onItemClick = onItemClick,
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
                val versionName = remember { BuildConfig.VERSION_NAME }
                val versionCode = remember { BuildConfig.VERSION_CODE }
                val xmpVersion = remember { Xmp.getVersion() }
                AboutScreen(
                    buildVersionName = versionName,
                    buildVersionCode = versionCode,
                    libVersion = xmpVersion,
                    onBack = { rootBackStack.removeLastOrNull() },
                )
            }
            entry<NavKeyRoot.SettingsFormats> {
                val formats = remember { Xmp.formatsSorted.toPersistentList() }
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
