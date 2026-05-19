package com.lossydragon.media3.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lossydragon.media3.model.PlayerUiState
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
