package org.helllabs.android.xmp.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.theme.XmpTheme

@Composable
fun PlaylistMenuItems(
    downloadClick: () -> Unit,
    settingsClick: () -> Unit
) {
    IconButton(onClick = { downloadClick() }) {
        Icon(
            imageVector = Icons.Default.Download,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            contentDescription = stringResource(id = R.string.download)
        )
    }
    IconButton(onClick = { settingsClick() }) {
        Icon(
            imageVector = Icons.Default.Settings,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            contentDescription = stringResource(id = R.string.settings)
        )
    }
}

@Composable
fun DeleteMenu(
    deleteClick: () -> Unit,
    image: ImageVector = Icons.Default.Delete
) {
    IconButton(onClick = { deleteClick() }) {
        Icon(
            imageVector = image,
            contentDescription = stringResource(id = R.string.delete)
        )
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Light Theme", uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun MenuAppBarPlaylistPreview() {
    XmpTheme {
        AppBar(
            title = stringResource(id = R.string.app_name),
            menuActions = { PlaylistMenuItems({}, {}) }
        )
    }
}

@Preview(name = "Light Theme", uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun MenuAppBarDeletePreview() {
    XmpTheme {
        AppBar(
            title = stringResource(id = R.string.app_name),
            menuActions = { DeleteMenu({}) }
        )
    }
}
