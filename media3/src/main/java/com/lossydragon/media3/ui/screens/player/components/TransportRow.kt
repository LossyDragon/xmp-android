package com.lossydragon.media3.ui.screens.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lossydragon.media3.model.PlaybackStatus
import com.lossydragon.media3.ui.theme.XmpTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TransportRow(
    modifier: Modifier = Modifier,
    status: PlaybackStatus,
    hasNext: Boolean,
    hasPrev: Boolean,
    isShuffle: Boolean,
    isLoop: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onShuffle: () -> Unit,
    onLoop: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier.padding(vertical = 4.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onShuffle,
            shapes = IconButtonDefaults.shapes(),
            content = {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) activeColor else inactiveColor,
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

        FilledIconButton(
            onClick = onToggle,
            modifier = Modifier.size(68.dp),
            shapes = IconButtonDefaults.shapes(),
            content = {
                Icon(
                    imageVector = if (status == PlaybackStatus.PLAYING) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
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
            onClick = onLoop,
            shapes = IconButtonDefaults.shapes(),
            content = {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Loop",
                    tint = if (isLoop) activeColor else inactiveColor,
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
                isShuffle = false,
                isLoop = true,
                onToggle = {},
                onNext = {},
                onPrev = {},
                onShuffle = {},
                onLoop = {},
            )
        }
    }
}
