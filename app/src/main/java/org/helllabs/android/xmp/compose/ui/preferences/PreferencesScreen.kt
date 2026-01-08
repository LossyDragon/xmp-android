package org.helllabs.android.xmp.compose.ui.preferences

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.KoinPreview
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.di.appModule
import org.koin.compose.koinInject
import timber.log.Timber

@Composable
fun PreferencesScreen(
    snackBarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onFormats: () -> Unit,
    onAbout: () -> Unit
) {
    val prefManager: PrefManager = koinInject()
    val storageManager: StorageManager = koinInject()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isScrolled = remember {
        derivedStateOf {
            scrollState.value > 0
        }
    }

    val playlistsDir by produceState("") {
        value = storageManager.getPlaylistsRootDirectory().getOrThrow().path
    }

    val setExplorerResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        scope.launch {
            if (uri == null) {
                Timber.w("Failed to get uri changing ")
                snackBarHostState.showSnackbar("Failed to change default directory")
                return@launch
            }
            storageManager.takePersistablePerms(uri)
            prefManager.setExplorerRootPath(uri.toString())
            snackBarHostState.showSnackbar("Default directory changed")
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackBarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = MaterialTheme.shapes.extraLarge,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            )
        },
        topBar = {
            XmpTopBar(
                title = stringResource(id = R.string.screen_title_preferences),
                isScrolled = isScrolled.value,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        val modifier = remember(configuration.orientation) {
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Modifier
            } else {
                Modifier.displayCutoutPadding()
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            val context = LocalContext.current
            SettingsGroupPlaylist()
            SettingsGroupSound()
            SettingsGroupInterface(onChangeExplorerDir = { setExplorerResult.launch(null) })
            SettingsGroupDownload()
            SettingsGroupInformation(
                playlistsDir = playlistsDir,
                onFormats = onFormats,
                onAbout = onAbout
            )

            if (BuildConfig.DEBUG) {
                SettingsGroup(title = { Text(text = "Debug") }) {
                    SettingsMenuLink(
                        title = {
                            Text(
                                text = "Revoke all Uri Permissions",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            val uriPermissions = context.contentResolver.persistedUriPermissions
                            for (permission in uriPermissions) {
                                try {
                                    context.contentResolver.releasePersistableUriPermission(
                                        permission.uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    )
                                } catch (_: SecurityException) {
                                    val uri = permission.uri
                                    Timber.d("Failed to revoke perms for URI: $uri")
                                }
                            }
                            (context as ComponentActivity).finishAffinity()
                        }
                    )
                    SettingsMenuLink(
                        title = {
                            Text(
                                text = "Clear Preferences",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            scope.launch {
                                prefManager.clearPreferences()
                                (context as ComponentActivity).finishAffinity()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview
@Composable
private fun Preview() {
    KoinPreview(modules = listOf(appModule)) {
        PreferencesScreen(
            snackBarHostState = SnackbarHostState(),
            onBack = {},
            onFormats = {},
            onAbout = {}
        )
    }
}
