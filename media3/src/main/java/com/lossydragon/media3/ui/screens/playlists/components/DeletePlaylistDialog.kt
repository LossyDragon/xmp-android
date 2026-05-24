package com.lossydragon.media3.ui.screens.playlists.components

import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.*
import com.lossydragon.media3.db.entity.PlaylistEntity
import com.lossydragon.media3.ui.theme.XmpTheme

@Composable
internal fun DeletePlaylistDialog(
    playlist: PlaylistEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
        },
        title = { Text(text = "Delete Playlist") },
        text = {
            Text(
                text = "Are you sure you want to delete '${playlist.name}'?\nThis cannot be undone!"
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                content = { Text(text = "Delete") }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, content = { Text(text = "Cancel") })
        }
    )
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        DeletePlaylistDialog(
            playlist = PlaylistEntity(name = "Amiga Tunes"),
            onDismiss = {},
            onConfirm = {}
        )
    }
}
