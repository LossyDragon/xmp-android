package com.lossydragon.media3.ui.player

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lossydragon.media3.player.XmpPlayerViewModel
import com.lossydragon.media3.ui.player.components.ChannelMeterGrid
import com.lossydragon.media3.ui.player.components.PatternInfoRow
import com.lossydragon.media3.ui.player.components.PlaybackProgress
import com.lossydragon.media3.ui.player.components.QueueSheet
import com.lossydragon.media3.ui.player.components.TransportRow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val viewModel = koinViewModel<XmpPlayerViewModel>(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()
    var hasLoadedOnce by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.currentModule) {
        if (state.currentModule != null) hasLoadedOnce = true
        if (hasLoadedOnce && state.currentModule == null) onBack()
    }

    if (state.currentModule == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    QueueSheet(
        sheetState = sheetState,
        queue = state.queue,
        currentIndex = state.currentQueueIndex,
        onItemClick = {
            viewModel.playAtIndex(it)
            scope.launch { sheetState.hide() }
        },
        onDismiss = { scope.launch { sheetState.hide() } },
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
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
                actions = {
                    IconButton(
                        onClick = { scope.launch { sheetState.expand() } },
                        content = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null
                            )
                        }
                    )
                    IconButton(
                        onClick = viewModel::stop,
                        content = {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null
                            )
                        }
                    )
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.weight(0.5f))

                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                    content = {
                        Text(
                            text = state.moduleType.ifBlank {
                                state.currentModule?.extension?.uppercase() ?: "MOD"
                            }.take(4),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                )

                Spacer(modifier = Modifier.weight(0.5f))

                Text(
                    text = state.moduleName.ifBlank { state.currentModule?.name ?: "" },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = state.moduleType.ifBlank {
                        state.currentModule?.extension?.uppercase() ?: ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(24.dp))

                state.frame?.let { PatternInfoRow(frame = it) }

                Spacer(modifier = Modifier.height(24.dp))

                PlaybackProgress(state = state, onSeek = viewModel::seek)

                Spacer(modifier = Modifier.height(8.dp))

                TransportRow(
                    status = state.status,
                    hasNext = state.currentQueueIndex < state.queue.lastIndex,
                    hasPrev = state.currentQueueIndex > 0,
                    isShuffle = state.isShuffle,
                    isLoop = state.isLoop,
                    onToggle = viewModel::togglePlayPause,
                    onNext = viewModel::next,
                    onPrev = viewModel::previous,
                    onShuffle = viewModel::toggleShuffle,
                    onLoop = viewModel::toggleLoop,
                )

                Spacer(modifier = Modifier.height(24.dp))

                state.frame?.let { ChannelMeterGrid(channels = it.channels) }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    )
}
