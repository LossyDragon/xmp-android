package com.lossydragon.media3.ui.screens.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import com.lossydragon.media3.model.PlaybackStatus
import com.lossydragon.media3.ui.theme.XmpTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TransportRow(
    modifier: Modifier = Modifier,
    status: PlaybackStatus,
    hasNext: Boolean,
    hasPrev: Boolean,
    onStop: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onQueueSheet: () -> Unit
) {
    Row(
        modifier = modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onStop,
            shapes = IconButtonDefaults.shapes(),
            content = {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    modifier = Modifier.size(24.dp),
                )
            }
        )

        IconButton(
            onClick = onPrev,
            enabled = hasPrev,
            shapes = IconButtonDefaults.shapes(),
            content = {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(32.dp),
                )
            }
        )

        FloatingActionButton(
            onClick = onPlayPause,
            content = {
                Icon(
                    imageVector = if (status == PlaybackStatus.PLAYING) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                )
            }
        )

        IconButton(
            onClick = onNext,
            enabled = hasNext,
            shapes = IconButtonDefaults.shapes(),
            content = {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(32.dp),
                )
            }
        )

        IconButton(
            onClick = onQueueSheet,
            shapes = IconButtonDefaults.shapes(),
            content = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Open Queue",
                    modifier = Modifier.size(24.dp),
                )
            }
        )
    }
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        Surface {
            TransportRow(
                status = PlaybackStatus.PLAYING,
                hasNext = false,
                hasPrev = true,
                onStop = {},
                onPrev = {},
                onPlayPause = {},
                onNext = {},
                onQueueSheet = {},
            )
        }
    }
}
