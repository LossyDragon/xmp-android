package com.lossydragon.media3.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        content = {
            Column(
                modifier = Modifier.wrapContentHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                        content = {
                            Text(
                                text = state.moduleType.ifBlank {
                                    state.currentModule?.extension?.uppercase() ?: "MOD"
                                }.take(4),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        content = {
                            Text(
                                text = state.moduleName.ifBlank {
                                    state.currentModule?.name ?: ""
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = state.moduleType.ifBlank {
                                    state.currentModule?.extension?.uppercase() ?: ""
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        content = {
                            IconButton(
                                onClick = onPrevious,
                                content = {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            )
                            IconButton(
                                onClick = onPlayPause,
                                content = {
                                    val isPlayingIcon = if (isPlaying) {
                                        Icons.Default.Pause
                                    } else {
                                        Icons.Default.PlayArrow
                                    }
                                    Icon(
                                        imageVector = isPlayingIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            )
                            IconButton(
                                onClick = onNext,
                                content = {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            )
                        }
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
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
