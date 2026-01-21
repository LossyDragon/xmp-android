package org.helllabs.android.xmp.ui.screens.preferences

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.ui.components.SingleChoiceListDialog
import org.helllabs.android.xmp.ui.navkey.NavKeyMain
import org.koin.compose.koinInject

@Composable
fun SettingsGroupInterface(
    onChangeExplorerDir: () -> Unit
) {
    val prefManager: PrefManager = koinInject()
    val scope = rememberCoroutineScope()

    // Collect preferences as state
    val showInfoLineValue by prefManager.showInfoLineFlow().collectAsStateWithLifecycle(
        initialValue = true
    )
    val keepScreenOnValue by prefManager.keepScreenOnFlow().collectAsStateWithLifecycle(
        initialValue = false
    )
    val showHexValue by prefManager.showHexFlow().collectAsStateWithLifecycle(initialValue = false)

    val initialScreenList = remember { NavKeyMain.getAllScreens() }
    val initialScreen by prefManager.initialStartFlow().collectAsStateWithLifecycle("Playlists")
    var initialScreenDialog by remember { mutableStateOf(false) }
    SingleChoiceListDialog(
        isShowing = initialScreenDialog,
        onDismiss = { initialScreenDialog = false },
        onEmpty = { initialScreenDialog = false },
        icon = Icons.Filled.Map,
        title = "Start Navigation",
        selectedIndex = initialScreenList.indexOf(initialScreen),
        textList = initialScreenList,
        onConfirm = {
            scope.launch {
                prefManager.setInitialStart(initialScreenList[it])
            }
            initialScreenDialog = false
        },
    )

    SettingsGroup(
        title = { Text(text = stringResource(id = R.string.pref_category_interface)) }
    ) {
        // Initial Start Tab
        SettingsMenuLink(
            title = { Text(text = "Initial Start Navigation") },
            subtitle = {
                Text(text = "Have the app start either on Playlists, Explorer, or Downloads")
            },
            onClick = { initialScreenDialog = true }
        )

        SettingsMenuLink(
            title = { Text(text = "Explorer default path") },
            subtitle = { Text(text = stringResource(id = R.string.pref_media_path_summary)) },
            onClick = onChangeExplorerDir
        )

        // SettingsMenuLink(
        //     title = { Text(text = "Playlists default path") },
        //     subtitle = { Text(text = "The directory where playlist files are located") },
        //     onClick = onChangePlaylistDir
        // )

        // Show Info Line
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_show_info_line_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_show_info_line_summary)) },
            state = showInfoLineValue,
            onCheckedChange = {
                scope.launch {
                    prefManager.setShowInfoLine(it)
                }
            }
        )

        // Keep Screen On
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_keep_screen_on_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_keep_screen_on_summary)) },
            state = keepScreenOnValue,
            onCheckedChange = {
                scope.launch {
                    prefManager.setKeepScreenOn(it)
                }
            }
        )

        // Show Hex
        SettingsSwitch(
            title = { Text(text = "Show hex values") },
            subtitle = { Text(text = "Show hex values for player info") },
            state = showHexValue,
            onCheckedChange = {
                scope.launch {
                    prefManager.setShowHex(it)
                }
            }
        )
    }
}
