package com.lossydragon.media3.ui.player

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lossydragon.media3.model.ChannelSnapshot
import com.lossydragon.media3.model.FrameSnapshot
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.model.PlaybackStatus
import com.lossydragon.media3.model.PlayerUiState
import com.lossydragon.media3.player.XmpPlayerViewModel
import com.lossydragon.media3.util.formatMs
import kotlinx.collections.immutable.ImmutableList
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val viewModel = koinViewModel<XmpPlayerViewModel>(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var hasLoadedOnce by remember { mutableStateOf(false) }

    // Auto-navigate back when playback finishes and queue is empty
    LaunchedEffect(state.currentModule) {
        Timber.d("LaunchedEffect currentModule=${state.currentModule}")
        if (state.currentModule != null) {
            hasLoadedOnce = true
        }
        if (hasLoadedOnce && state.currentModule == null) {
            Timber.d("currentModule null — calling onBack()")
            onBack()
        }
    }

    // Filler to eliminate wierd states when the screen is transitioning backwards.
    if (state.currentModule == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.moduleName.ifBlank { "No module loaded" },
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (state.moduleType.isNotBlank()) {
                            Text(
                                text = state.moduleType,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        content = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    )
                },
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.frame?.let { PatternInfoRow(it) }

                PlaybackProgress(state = state, onSeek = { viewModel.seek(it) })

                state.frame?.let { ChannelMeterGrid(channels = it.channels) }

                if (state.queue.isNotEmpty()) {
                    QueueList(
                        queue = state.queue,
                        currentIndex = state.currentQueueIndex,
                        onItemClick = { viewModel.playAtIndex(it) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                TransportRow(
                    status = state.status,
                    hasNext = state.currentQueueIndex < state.queue.lastIndex,
                    hasPrev = state.currentQueueIndex > 0,
                    onToggle = viewModel::togglePlayPause,
                    onNext = viewModel::next,
                    onPrev = viewModel::previous,
                    onStop = viewModel::stop,
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    )
}

@Composable
private fun PatternInfoRow(frame: FrameSnapshot) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        content = {
            InfoChip(label = "Pos", value = "${frame.position}")
            InfoChip(label = "Pat", value = "${frame.pattern}")
            InfoChip(label = "Row", value = "${frame.row}/${frame.numRows}")
            InfoChip(label = "Spd", value = "${frame.speed}")
            InfoChip(label = "BPM", value = "${frame.bpm}")
        }
    )
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    )
}

@Composable
private fun PlaybackProgress(
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
                val seekMs = (seekPosition * duration).toLong()
                onSeek(seekMs)
                isSeeking = false
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            content = {
                Text(
                    text = if (isSeeking) {
                        (seekPosition * duration).toLong().formatMs()
                    } else {
                        state.positionMs.formatMs()
                    },
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

@Composable
fun ChannelMeterGrid(channels: ImmutableList<ChannelSnapshot>) {
    Column {
        Text(
            text = "Channels (${channels.size})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = {
                itemsIndexed(
                    items = channels,
                    itemContent = { idx, ch -> ChannelMeter(index = idx, channel = ch) }
                )
            }
        )
    }
}

@Composable
private fun ChannelMeter(
    index: Int,
    channel: ChannelSnapshot,
    width: Dp = 24.dp,
    height: Dp = 120.dp
) {
    val volFraction = (channel.volume / 64f).coerceIn(0f, 1f)
    val finalVolFraction = (channel.finalVol / 64f).coerceIn(0f, 1f)

    val animatedVol by animateFloatAsState(
        targetValue = volFraction,
        animationSpec = tween(durationMillis = 40),
        label = "vol_ch$index",
    )
    val animatedFinalVol by animateFloatAsState(
        targetValue = finalVolFraction,
        animationSpec = tween(durationMillis = 80),
        label = "finalVol_ch$index",
    )

    val barColor = when {
        animatedVol > 0.85f -> MaterialTheme.colorScheme.error
        animatedVol > 0.65f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(width),
        content = {
            Box(
                modifier = Modifier
                    .width(width - 4.dp)
                    .height(height)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.BottomCenter,
                content = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height * animatedFinalVol)
                            .background(barColor.copy(alpha = 0.25f)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height * animatedVol)
                            .background(barColor),
                    )
                }
            )
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    )
}

@Composable
private fun TransportRow(
    status: PlaybackStatus,
    hasNext: Boolean,
    hasPrev: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            IconButton(
                onClick = onStop,
                content = {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onPrev,
                enabled = hasPrev,
                content = {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = onToggle,
                modifier = Modifier.size(64.dp),
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

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onNext,
                enabled = hasNext,
                content = {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(32.dp)
                    )
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniPlayerBar(
    state: PlayerUiState,
    onTap: () -> Unit,
    onToggle: () -> Unit
) {
    val duration = state.durationMs.toFloat().coerceAtLeast(1f)
    val progress = (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)

    HorizontalFloatingToolbar(
        expanded = true,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onTap),
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = onToggle,
                content = {
                    Icon(
                        imageVector = if (state.status == PlaybackStatus.PLAYING) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = null,
                    )
                }
            )
        },
        content = {
            if (state.status == PlaybackStatus.PLAYING) {
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .width(48.dp)
                        .padding(horizontal = 4.dp),
                    amplitude = { 2.dp.value },
                    wavelength = 8.dp,
                )
            }
            Text(
                text = state.moduleName.ifBlank { state.currentModule?.name ?: "" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    )
}

@Composable
private fun QueueList(
    queue: ImmutableList<ModuleFile>,
    currentIndex: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Scroll to current item
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && currentIndex < queue.size) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(queue, key = { _, f -> f.uri.toString() }) { index, file ->
            val isCurrent = index == currentIndex
            ListItem(
                headlineContent = {
                    Text(
                        text = file.name,
                        style = if (isCurrent) {
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingContent = {
                    if (isCurrent) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                supportingContent = {
                    Text(
                        text = file.extension.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(index) }
                    .background(
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
            )
        }
    }
}
