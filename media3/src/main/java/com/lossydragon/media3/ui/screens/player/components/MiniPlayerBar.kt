package com.lossydragon.media3.ui.screens.player.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.core.net.toUri
import androidx.media3.common.Player
import com.lossydragon.media3.R
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.model.PlaybackStatus
import com.lossydragon.media3.model.PlayerUiState
import com.lossydragon.media3.ui.theme.XmpTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniPlayerBar(
    state: PlayerUiState,
    onTap: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val duration = state.durationMs.toFloat().coerceAtLeast(1f)
    val progress = (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    val isPlaying = state.status == PlaybackStatus.PLAYING

    val hasNext = when {
        state.queue.isEmpty() -> false

        state.repeatMode == Player.REPEAT_MODE_ALL ||
            state.repeatMode == Player.REPEAT_MODE_ONE -> true

        else -> state.currentQueueIndex < state.queue.lastIndex
    }
    val hasPrev = when {
        state.queue.isEmpty() -> false

        state.repeatMode == Player.REPEAT_MODE_ALL ||
            state.repeatMode == Player.REPEAT_MODE_ONE -> true

        else -> state.currentQueueIndex > 0
    }

    ListItem(
        modifier = Modifier.padding(6.dp),
        shapes = ListItemDefaults.shapes(
            shape = MaterialTheme.shapes.small,
            focusedShape = MaterialTheme.shapes.small,
            pressedShape = MaterialTheme.shapes.small,
        ),
        onClick = onTap,
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.onSecondary
        ),
        leadingContent = {
            Image(
                painter = painterResource(R.drawable.icon512_trimmed),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    },
            )
        },
        trailingContent = {
            Row {
                IconButton(
                    onClick = onPrevious,
                    enabled = hasPrev,
                    content = {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = null
                        )
                    }
                )
                IconButton(
                    onClick = onPlayPause,
                    content = {
                        val icon = if (isPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        }
                        Icon(imageVector = icon, contentDescription = null)
                    }
                )
                IconButton(
                    onClick = onNext,
                    enabled = hasNext,
                    content = {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = null
                        )
                    }
                )
            }
        },
        content = {
            Text(
                text = state.moduleName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = state.moduleType,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    amplitude = if (isPlaying) {
                        WavyProgressIndicatorDefaults.indicatorAmplitude
                    } else {
                        { 0f }
                    }

                )
            }
        }
    )
}

@Preview
@Composable
private fun PreviewPlaying() {
    XmpTheme {
        MiniPlayerBar(
            state = PlayerUiState(
                status = PlaybackStatus.PLAYING,
                currentModule = ModuleFile(
                    uri = "content://preview/1".toUri(),
                    name = "a_journey_into_sound.far",
                    sizeBytes = 123456L,
                    extension = "far",
                ),
                moduleName = "A Journey Into Sound",
                moduleType = "FAR",
                positionMs = 62000L,
                durationMs = 252849L,
            ),
            onTap = {},
            onPlayPause = {},
            onNext = {},
            onPrevious = {},
        )
    }
}

@Preview
@Composable
private fun PreviewPaused() {
    XmpTheme {
        MiniPlayerBar(
            state = PlayerUiState(
                status = PlaybackStatus.PAUSED,
                currentModule = ModuleFile(
                    uri = "content://preview/1".toUri(),
                    name = "aegis_-_beneath_the_fallen_stars.it",
                    sizeBytes = 1820792L,
                    extension = "it",
                ),
                moduleName = "Beneath the Fallen Stars",
                moduleType = "IT",
                positionMs = 120000L,
                durationMs = 480000L,
            ),
            onTap = {},
            onPlayPause = {},
            onNext = {},
            onPrevious = {},
        )
    }
}
