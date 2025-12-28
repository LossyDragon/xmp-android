package org.helllabs.android.xmp.compose.ui.preferences

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsSwitch
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.core.PrefManager
import org.koin.compose.koinInject

@Composable
fun SettingsGroupDownload() {
    val prefManager: PrefManager = koinInject()
    val scope = rememberCoroutineScope()

    // Collect preferences as state
    val modArchiveFolderValue by prefManager.modArchiveFolderFlow().collectAsStateWithLifecycle(
        initialValue = true
    )
    val artistFolderValue by prefManager.artistFolderFlow().collectAsStateWithLifecycle(
        initialValue = true
    )

    SettingsGroup(
        title = { Text(text = stringResource(id = R.string.pref_category_modarchive)) }
    ) {
        // ModArchive Folder
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_modarchive_folder_title)) },
            subtitle = {
                Text(text = stringResource(id = R.string.pref_modarchive_folder_summary))
            },
            state = modArchiveFolderValue,
            onCheckedChange = {
                scope.launch {
                    prefManager.setModArchiveFolder(it)
                }
            }
        )

        // Artist Folder
        SettingsSwitch(
            title = { Text(text = stringResource(id = R.string.pref_artist_folder_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_artist_folder_summary)) },
            state = artistFolderValue,
            onCheckedChange = {
                scope.launch {
                    prefManager.setArtistFolder(it)
                }
            }
        )
    }
}
