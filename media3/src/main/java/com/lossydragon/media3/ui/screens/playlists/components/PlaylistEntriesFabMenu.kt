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
internal fun PlaylistEntriesFabMenu(
    expanded: Boolean,
    onExpand: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    FloatingActionButtonMenu(
        modifier = Modifier.offset(x = 16.dp, y = 16.dp),
        expanded = expanded,
        button = {
            FloatingActionButton(
                onClick = onExpand,
                shape = MaterialTheme.shapes.small,
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
                onClick = onShuffle,
                text = { Text(text = "Play Shuffled") },
                icon = { Icon(imageVector = Icons.Default.Shuffle, contentDescription = null) }
            )
            FloatingActionButtonMenuItem(
                onClick = onPlayAll,
                text = { Text(text = "Play All") },
                icon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null) }
            )
        },
    )
}

private class PlaylistEntriesPreviewParameter : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

@Preview
@Composable
private fun Preview(
    @PreviewParameter(PlaylistEntriesPreviewParameter::class) expanded: Boolean
) {
    XmpTheme {
        Surface {
            PlaylistEntriesFabMenu(
                expanded = expanded,
                onExpand = {},
                onPlayAll = {},
                onShuffle = {},
            )
        }
    }
}
