package org.helllabs.android.xmp.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.systemBarsPadding
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.ui.components.waterfallPadding
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.util.PrefManager
import org.helllabs.android.xmp.util.PrefManager.themeRequest
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.logE
import org.helllabs.android.xmp.util.logW

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Connection
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlayerService.PlayerBinder

            val modPlayer = binder.getService()
            modPlayer.add(XmpApplication.mAddList)
            unbindService(this)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            logW("Service unexpectedly disconnected")
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        logD("onCreate")
        setContent {
            val context = LocalContext.current
            val permissions = rememberMultiplePermissionsState(
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )

            // Invoke permissions
            LaunchedEffect(true) {
                permissions.launchMultiplePermissionRequest()
            }

            val theme = PrefManager.getPreferenceFlow(themeRequest).collectAsState(true)
            val themeMode = when (theme.value) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            ProvideWindowInsets {
                XmpTheme3(isDarkTheme = themeMode) {
                    if (permissions.allPermissionsGranted) {
                        // All Permissions granted.
                        NavigationScreen(
                            onBackPressedCallback = onBackPressedDispatcher,
                            bindService = {
                                val service = Intent(context, PlayerService::class.java)

                                bindService(
                                    service,
                                    connection,
                                    BIND_AUTO_CREATE
                                )
                            }
                        )
                    } else {
                        if (permissions.shouldShowRationale) {
                            // Need Permissions
                            NeedPermissionsScreen(permissions)
                        } else {
                            // Permissions most-likely permanently denied.
                            PermissionsDeniedScreen(permissions)
                        }
                    }
                }
            }
        }
    }

    companion object {
        @Suppress("DEPRECATION") // Not using SAF yet
        private val SD_DIR: File = Environment.getExternalStorageDirectory()

        val DATA_DIR = File(SD_DIR, "Xmp for Android")
        val DEFAULT_MEDIA_PATH = "$SD_DIR/mod"

        fun checkStorage(): Boolean {
            val state = Environment.getExternalStorageState()
            return if (Environment.MEDIA_MOUNTED == state ||
                Environment.MEDIA_MOUNTED_READ_ONLY == state
            ) {
                true
            } else {
                logE("External storage state error: $state")
                false
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun NeedPermissionsScreen(permissions: MultiplePermissionsState) {
    val appName = stringResource(id = R.string.app_name)
    PermissionsScreen(
        message = "The following permissions is needed for\n$appName to run properly: \n",
        permissionsList = getPermissionsText(permissions.revokedPermissions),
        buttonText = "Request permission "
    ) {
        permissions.launchMultiplePermissionRequest()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionsDeniedScreen(permissions: MultiplePermissionsState) {
    val context = LocalContext.current
    PermissionsScreen(
        message = "Permissions denied.\nPlease manually grant permissions in Settings.\n",
        permissionsList = getPermissionsText(permissions.revokedPermissions),
        buttonText = "Open Settings"
    ) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", context.packageName, null)
        context.startActivity(intent)
    }
}

@Composable
private fun PermissionsScreen(
    message: String,
    permissionsList: AnnotatedString,
    buttonText: String,
    onClicked: () -> Unit
) {
    Surface {
        Column(
            modifier = Modifier
                .waterfallPadding()
                .fillMaxSize()
                .systemBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = message)
            Text(text = permissionsList)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onClicked) {
                Text(
                    text = buttonText,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun getPermissionsText(permissions: List<PermissionState>): AnnotatedString {
    val revokedPermissionsSize = permissions.size

    val permissionsString = buildAnnotatedString {
        if (revokedPermissionsSize == 0)
            append("")

        for (perm in permissions.indices) {
            append(permissions[perm].permission.substringAfterLast('.'))
            when (perm) {
                revokedPermissionsSize - 1 -> {
                    append(" ")
                }
                else -> {
                    append(",\n")
                }
            }
        }
    }

    return permissionsString
}
