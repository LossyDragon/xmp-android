package org.helllabs.android.xmp.presentation.components

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.presentation.theme.AppTheme

@Composable
fun MainMenuItems(
    downloadClick: () -> Unit,
    settingsClick: () -> Unit
) {
    IconButton(onClick = { downloadClick() }) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = stringResource(id = R.string.download)
        )
    }
    IconButton(onClick = { settingsClick() }) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = stringResource(id = R.string.settings)
        )
    }
}

@Composable
fun DeleteMenu(
    deleteClick: () -> Unit,
) {
    IconButton(onClick = { deleteClick() }) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(id = R.string.delete)
        )
    }
}

/************
 * Previews *
 ************/

@Preview
@Composable
private fun MenuAppBarPreview() {
    AppTheme(false) {
        AppBar(
            title = stringResource(id = R.string.app_name),
            menuActions = { MainMenuItems({}, {}) }
        )
    }
}

@Preview
@Composable
private fun MenuAppBarPreviewDark() {
    AppTheme(true) {
        AppBar(
            title = stringResource(id = R.string.app_name),
            menuActions = { MainMenuItems({}, {}) }
        )
    }
}

@Preview
@Composable
private fun HistoryMenuPreview() {
    AppTheme(false) {
        AppBar(
            title = stringResource(id = R.string.app_name),
            menuActions = { DeleteMenu {} }
        )
    }
}

@Preview
@Composable
private fun HistoryMenuPreviewDark() {
    AppTheme(true) {
        AppBar(
            title = stringResource(id = R.string.app_name),
            menuActions = { DeleteMenu {} }
        )
    }
}
