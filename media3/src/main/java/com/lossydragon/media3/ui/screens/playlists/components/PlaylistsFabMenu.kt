package com.lossydragon.media3.ui.screens.playlists.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.*
import com.lossydragon.media3.ui.theme.XmpTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlaylistsFabMenu(
    expanded: Boolean,
    onExpand: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onNewPlaylist: () -> Unit
) {
    FloatingActionButtonMenu(
        modifier = Modifier.offset(x = 16.dp, y = 16.dp),
        expanded = expanded,
        button = {
            FloatingActionButton(
                onClick = onExpand,
                content = {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.PlaylistRemove
                        } else {
                            Icons.AutoMirrored.Filled.PlaylistPlay
                        },
                        contentDescription = null,
                    )
                }
            )
        },
        content = {
            FloatingActionButtonMenuItem(
                onClick = onImport,
                text = { Text(text = "Import Playlists") },
                icon = { Icon(imageVector = Icons.Default.Download, contentDescription = null) }
            )
            FloatingActionButtonMenuItem(
                onClick = onExport,
                text = { Text(text = "Export Playlists") },
                icon = { Icon(imageVector = Icons.Default.Upload, contentDescription = null) }
            )
            FloatingActionButtonMenuItem(
                onClick = onNewPlaylist,
                text = { Text(text = "New Playlist") },
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) }
            )
        },
    )
}

private class PlaylistsPreviewParameter : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

@Preview
@Composable
private fun Preview(
    @PreviewParameter(PlaylistsPreviewParameter::class) expanded: Boolean
) {
    XmpTheme {
        Surface {
            PlaylistsFabMenu(
                expanded = expanded,
                onExpand = {},
                onImport = {},
                onExport = {},
                onNewPlaylist = {},
            )
        }
    }
}
