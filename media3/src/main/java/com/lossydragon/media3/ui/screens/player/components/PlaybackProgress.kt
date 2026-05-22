package com.lossydragon.media3.ui.screens.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import com.lossydragon.media3.model.PlayerUiState
import com.lossydragon.media3.ui.theme.XmpTheme
import com.lossydragon.media3.util.formatMs

@Composable
internal fun PlaybackProgress(
    state: PlayerUiState,
    onSeek: (Long) -> Unit
) {
    val duration = state.durationMs.toFloat().coerceAtLeast(1f)

    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    val displayValue = if (isSeeking) {
        seekPosition
    } else {
        (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            Text(
                modifier = Modifier.padding(horizontal = 6.dp),
                text = if (isSeeking) {
                    (seekPosition * duration).toLong().formatMs()
                } else {
                    state.positionMs.formatMs()
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Slider(
                modifier = Modifier.weight(1f),
                value = displayValue,
                onValueChange = {
                    isSeeking = true
                    seekPosition = it
                },
                onValueChangeFinished = {
                    onSeek((seekPosition * duration).toLong())
                    isSeeking = false
                },
            )

            Text(
                modifier = Modifier.padding(horizontal = 6.dp),
                text = state.durationMs.formatMs(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    )
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        Surface {
            PlaybackProgress(
                state = PlayerUiState(
                    positionMs = 12345,
                    durationMs = 65535,
                ),
                onSeek = {}
            )
        }
    }
}
