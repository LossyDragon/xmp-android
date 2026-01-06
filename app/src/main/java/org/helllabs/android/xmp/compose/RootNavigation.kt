package org.helllabs.android.xmp.compose

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.meticha.permissions_compose.AppPermission
import com.meticha.permissions_compose.rememberAppPermissionState
import kotlinx.collections.immutable.persistentListOf
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.PlayerActivityLauncher
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.navkey.NavKeyRoot
import org.helllabs.android.xmp.compose.ui.preferences.AboutScreen
import org.helllabs.android.xmp.compose.ui.preferences.FormatsScreen
import org.helllabs.android.xmp.compose.ui.preferences.PreferencesScreen

@Composable
fun RootNavigation(
    snackBarHostState: SnackbarHostState,
    onPlayAll: (List<Uri>, Boolean, Boolean, PlayerActivityLauncher) -> Unit,
    onAddQueue: (List<Uri>, Boolean, Boolean, PlayerActivityLauncher) -> Unit,
    onPlayModule: (List<Uri>, Int, Boolean, Boolean, Boolean, PlayerActivityLauncher) -> Unit,
    onItemClick: (List<Uri>, Int, Boolean, Boolean, PlayerActivityLauncher) -> Unit
) {
    val rootBackStack = rememberNavBackStack(NavKeyRoot.Main)

    // region [REGION] Permissions
    val permissionsList = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            persistentListOf(
                AppPermission(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    description = "Post Notifications access is needed to display the foreground service icon",
                    isRequired = true,
                ),
            )
        } else {
            persistentListOf()
        }
    }
    val permissions = rememberAppPermissionState(permissions = permissionsList)
    LaunchedEffect(Unit) {
        permissions.requestPermission()
    }
    // endregion

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
                val formats = remember { Xmp.formats }
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
