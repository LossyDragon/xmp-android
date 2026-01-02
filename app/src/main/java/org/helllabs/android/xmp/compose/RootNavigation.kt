package org.helllabs.android.xmp.compose

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.navkey.NavKeyRoot
import org.helllabs.android.xmp.compose.ui.preferences.AboutScreen
import org.helllabs.android.xmp.compose.ui.preferences.FormatsScreen
import org.helllabs.android.xmp.compose.ui.preferences.PreferencesScreen
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

@Composable
fun RootNavigation() {
    val context = LocalContext.current

    val rootBackStack = rememberNavBackStack(NavKeyRoot.Main)

    val snackBarHostState = retain { SnackbarHostState() }

    // region [REGION] Permissions
    val permsViewModel = koinInject<PermissionViewModel> {
        val perms = persistentListOf(
            PermissionModel(
                permission = Manifest.permission.POST_NOTIFICATIONS,
                rational = "Show notification for media playback"
            )
        )
        parametersOf(perms)
    }
    val permsState by permsViewModel.state.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = permsViewModel::onResult
    )
    LaunchedEffect(permsState.askPermission) {
        if (permsState.askPermission) {
            permissionLauncher.launch(permsState.permissions.toTypedArray())
        }
    }
    LaunchedEffect(permsState.navigateToSetting) {
        if (permsState.navigateToSetting) {
            val result = snackBarHostState.showSnackbar(
                message = "${permsState.permissions.size} permission(s) were not granted",
                actionLabel = "Show",
                withDismissAction = true
            )
            when (result) {
                SnackbarResult.Dismissed -> Timber.w("Permissions dismissed")

                SnackbarResult.ActionPerformed -> {
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    ).also {
                        context.startActivity(it)
                    }
                }
            }
            permsViewModel.onPermissionRequested()
        }
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
                val formats by remember {
                    val list = Xmp.formats.sorted().toPersistentList()
                    mutableStateOf(list)
                }
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
