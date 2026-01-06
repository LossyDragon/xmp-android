package org.helllabs.android.xmp.compose.ui.preferences

import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.SingleChoiceListDialog
import org.helllabs.android.xmp.core.PrefManager
import org.koin.compose.koinInject
import timber.log.Timber

@Composable
fun SettingsGroupPlaylist(
    onChangeDir: () -> Unit
) {
    val prefManager: PrefManager = koinInject()
    val scope = rememberCoroutineScope()

    // Collect preferences as state
    val examplesValue by prefManager.examplesFlow().collectAsStateWithLifecycle(initialValue = true)
    val playlistModeValue by prefManager.playlistModeFlow().collectAsStateWithLifecycle(
        initialValue = 1
    )
    val useFileNameValue by prefManager.useFileNameFlow().collectAsStateWithLifecycle(
        initialValue = false
    )

    SettingsGroup(
        title = {
            Text(text = stringResource(id = R.string.pref_category_files))
        }
    ) {
        SettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.pref_media_path_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_media_path_summary)) },
            onClick = onChangeDir
        )

        // Install Modules
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_examples_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_examples_summary)) },
            state = examplesValue,
            onCheckedChange = {
                scope.launch {
                    prefManager.setExamples(it)
                }
            }
        )

        // Playlist Mode
        var playlistModeDialog by remember { mutableStateOf(false) }
        val playlistModeValues = stringArrayResource(id = R.array.playlist_mode_values)
        val playlistMode = remember(playlistModeValue) {
            playlistModeValues.indexOfFirst { it.toInt() == playlistModeValue }.coerceAtLeast(0)
        }

        SettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.pref_playlist_mode_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_playlist_mode_summary)) },
            onClick = {
                playlistModeDialog = true
            }
        )
        SingleChoiceListDialog(
            isShowing = playlistModeDialog,
            icon = Icons.Filled.CheckCircle,
            title = stringResource(id = R.string.pref_playlist_mode_title),
            selectedIndex = playlistMode,
            textList = stringArrayResource(id = R.array.playlist_mode_array).toPersistentList(),
            onConfirm = {
                scope.launch {
                    prefManager.setPlaylistMode(playlistModeValues[it].toInt())
                    playlistModeDialog = false
                }
            },
            onDismiss = {
                playlistModeDialog = false
            },
            onEmpty = {
                Timber.e("Playlist modes was empty")
                playlistModeDialog = false
            }
        )

        // Use Filename
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_use_filename_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_use_filename_summary)) },
            state = useFileNameValue,
            onCheckedChange = {
                scope.launch {
                    prefManager.setUseFileName(it)
                }
            }
        )
    }
}
