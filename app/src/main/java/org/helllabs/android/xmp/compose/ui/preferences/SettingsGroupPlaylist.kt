package org.helllabs.android.xmp.compose.ui.preferences

import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import kotlinx.collections.immutable.toPersistentList
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.SingleChoiceListDialog
import org.helllabs.android.xmp.core.PrefManager
import timber.log.Timber

@Composable
fun SettingsGroupPlaylist(
    onChangeDir: () -> Unit
) {
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

        var installModules by remember { mutableStateOf(PrefManager.examples) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_examples_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_examples_summary)) },
            state = installModules,
            onCheckedChange = {
                PrefManager.examples = it
                installModules = it
            }

        )

        var playlistMode by remember { mutableIntStateOf(0) }
        val playlistModeValues = stringArrayResource(id = R.array.playlist_mode_values)
        var playlistModeDialog by remember { mutableStateOf(false) }
        LaunchedEffect(playlistModeDialog) {
            val mode = PrefManager.playlistMode
            playlistModeValues.forEachIndexed { index, s ->
                if (mode == s.toInt()) {
                    playlistMode = index
                }
            }
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
            list = stringArrayResource(id = R.array.playlist_mode_array).toPersistentList(),
            onConfirm = {
                PrefManager.playlistMode = it + 1
                playlistModeDialog = false
            },
            onDismiss = {
                playlistModeDialog = false
            },
            onEmpty = {
                Timber.e("Playlist modes was empty")
                playlistModeDialog = false
            }
        )

        var useFileName by remember { mutableStateOf(PrefManager.useFileName) }
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_use_filename_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_use_filename_summary)) },
            state = useFileName,
            onCheckedChange = {
                PrefManager.useFileName = it
                useFileName = it
            }
        )

        var backButton by remember { mutableStateOf(PrefManager.backButtonNavigation) }
        SettingsSwitch(
            title = {
                Text(text = stringResource(id = R.string.pref_back_button_navigation_title))
            },
            subtitle = {
                Text(text = stringResource(id = R.string.pref_back_button_navigation_summary))
            },
            state = backButton,
            onCheckedChange = {
                PrefManager.backButtonNavigation = it
                backButton = it
            }
        )
    }
}
