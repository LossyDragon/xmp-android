package com.lossydragon.media3.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lossydragon.media3.model.PlaybackStatus
import com.lossydragon.media3.ui.theme.XmpTheme

@Composable
internal fun TransportRow(
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            IconButton(
                onClick = onShuffle,
                content = {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = if (isShuffle) activeColor else inactiveColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
            )
            IconButton(
                onClick = onPrev,
                enabled = hasPrev,
                content = {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                }
            )
            FilledIconButton(
                onClick = onToggle,
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                content = {
                    val icon = if (status == PlaybackStatus.PLAYING) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                    )
                }
            )
            IconButton(
                onClick = onNext,
                enabled = hasNext,
                content = {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                }
            )
            IconButton(
                onClick = onLoop,
                content = {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = if (isLoop) activeColor else inactiveColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
            )
        }
    )
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
