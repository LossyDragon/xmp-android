package com.lossydragon.media3.ui.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.*
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

    Column {
        Slider(
            value = displayValue,
            onValueChange = {
                isSeeking = true
                seekPosition = it
            },
            onValueChangeFinished = {
                onSeek((seekPosition * duration).toLong())
                isSeeking = false
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            content = {
                val isSeeking = if (isSeeking) {
                    (seekPosition * duration).toLong().formatMs()
                } else {
                    state.positionMs.formatMs()
                }
                Text(
                    text = isSeeking,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = state.durationMs.formatMs(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        )
    }
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        PlaybackProgress(
            state = PlayerUiState(
                positionMs = 12345,
                durationMs = 65535,
            ),
            onSeek = {}
        )
    }
}
