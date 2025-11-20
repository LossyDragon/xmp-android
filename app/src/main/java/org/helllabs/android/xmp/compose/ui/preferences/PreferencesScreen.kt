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
import androidx.core.net.toUri
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import timber.log.Timber

@Serializable
object NavPreferences

@Composable
fun PreferencesScreen(
    snackBarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onFormats: () -> Unit,
    onAbout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isScrolled = remember {
        derivedStateOf {
            scrollState.value > 0
        }
    }

    val documentTreeResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        scope.launch {
            StorageManager.setPlaylistDirectory(uri = uri).onSuccess {
                snackBarHostState.showSnackbar("Default directory changed")
            }.onFailure {
                snackBarHostState.showSnackbar("Failed to change default directory")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
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
                .verticalScroll(scrollState)
        ) {
            val context = LocalContext.current
            SettingsGroupPlaylist(
                onChangeDir = {
                    scope.launch {
                        val dir = PrefManager.safStoragePath.toUri()
                        documentTreeResult.launch(dir)
                    }
                }
            )
            SettingsGroupSound()
            SettingsGroupInterface()
            SettingsGroupDownload()
            SettingsGroupInformation(onFormats = onFormats, onAbout = onAbout)

            if (BuildConfig.DEBUG) {
                SettingsGroup(
                    title = { Text(text = "Debug") }
                ) {
                    SettingsMenuLink(
                        title = { Text(text = "Revoke all Uri Permissions") },
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
                        title = { Text(text = "Clear Preferences") },
                        onClick = {
                            PrefManager.clearPreferences()
                            (context as ComponentActivity).finishAffinity()
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview_PreferencesScreen() {
    val context = LocalContext.current
    PrefManager.init(context)
    XmpTheme(useDarkTheme = true) {
        PreferencesScreen(
            snackBarHostState = SnackbarHostState(),
            onBack = {},
            onFormats = {},
            onAbout = {}
        )
    }
}
