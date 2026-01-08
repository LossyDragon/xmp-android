package org.helllabs.android.xmp.compose.ui.preferences

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import org.helllabs.android.xmp.R

@Composable
fun SettingsGroupInformation(
    playlistsDir: String,
    onFormats: () -> Unit,
    onAbout: () -> Unit
) {
    SettingsGroup(
        title = { Text(text = stringResource(id = R.string.pref_category_information)) }
    ) {
        SettingsMenuLink(
            title = { Text(text = stringResource(R.string.pref_list_formats_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_list_formats_summary)) },
            onClick = onFormats
        )
        SettingsMenuLink(
            title = { Text(text = stringResource(R.string.pref_about_title)) },
            subtitle = { Text(text = stringResource(id = R.string.pref_about_summary)) },
            onClick = onAbout
        )
        SettingsMenuLink(
            title = { Text(text = "Playlists Location") },
            subtitle = { Text(text = playlistsDir) },
            onClick = { }
        )
    }
}
