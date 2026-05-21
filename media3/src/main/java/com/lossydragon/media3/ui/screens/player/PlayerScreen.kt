package com.lossydragon.media3.ui.screens.player

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lossydragon.media3.model.ChannelSnapshot
import com.lossydragon.media3.model.FrameSnapshot
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.model.PlaybackStatus
import com.lossydragon.media3.model.PlayerUiState
import com.lossydragon.media3.player.XmpPlayerViewModel
import com.lossydragon.media3.ui.screens.player.components.ChannelMeterGrid
import com.lossydragon.media3.ui.screens.player.components.PatternInfoRow
import com.lossydragon.media3.ui.screens.player.components.PlaybackProgress
import com.lossydragon.media3.ui.screens.player.components.PlayerBottomAppBar
import com.lossydragon.media3.ui.screens.player.components.QueueSheet
import com.lossydragon.media3.ui.screens.player.components.TransportRow
import com.lossydragon.media3.ui.theme.XmpTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private sealed class PlayerAction {
    data object OnBack : PlayerAction()
    data object OnStop : PlayerAction()
    data object OnPlayPause : PlayerAction()
    data object OnPrevious : PlayerAction()
    data object OnLoop : PlayerAction()
    data object OnShuffle : PlayerAction()
    data object OnNext : PlayerAction()
    data class OnSeek(val seek: Long) : PlayerAction()
    data class OnQueueSheet(val open: Boolean) : PlayerAction()
    data class OnQueueClick(val int: Int) : PlayerAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val viewModel = koinViewModel<XmpPlayerViewModel>(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    var showQueue by remember { mutableStateOf(false) }
    var hasLoadedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentModule) {
        if (state.currentModule != null) hasLoadedOnce = true
        if (hasLoadedOnce && state.currentModule == null) onBack()
    }

    PlayerScreenContent(
        modifier = modifier,
        state = state,
        sheetState = sheetState,
        showQueue = showQueue,
        onAction = { action ->
            when (action) {
                PlayerAction.OnBack -> onBack()

                PlayerAction.OnPlayPause -> viewModel.togglePlayPause()

                PlayerAction.OnStop -> viewModel.stop()

                PlayerAction.OnPrevious -> viewModel.previous()

                PlayerAction.OnNext -> viewModel.next()

                PlayerAction.OnShuffle -> viewModel.toggleShuffle()

                PlayerAction.OnLoop -> viewModel.toggleLoop()

                is PlayerAction.OnSeek -> viewModel.seek(action.seek)

                is PlayerAction.OnQueueClick -> {
                    viewModel.playAtIndex(action.int)
                    scope.launch { sheetState.hide() }
                }

                is PlayerAction.OnQueueSheet -> showQueue = action.open
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScreenContent(
    modifier: Modifier = Modifier,
    state: PlayerUiState,
    sheetState: SheetState,
    showQueue: Boolean,
    onAction: (PlayerAction) -> Unit
) {
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
                title = { Text(text = "Player") },
                navigationIcon = {
                    IconButton(
                        onClick = { onAction(PlayerAction.OnBack) },
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
                        onClick = { onAction(PlayerAction.OnQueueSheet(true)) },
                        content = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = null
                            )
                        }
                    )
                    IconButton(
                        onClick = { onAction(PlayerAction.OnStop) },
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
        // floatingActionButton = {
        //     ExtendedFloatingActionButton(
        //         text = { Text("Show bottom sheet") },
        //         icon = { Icon(Icons.Filled.Add, contentDescription = "") },
        //         onClick = { onAction(PlayerAction.OnQueueSheet(true)) }
        //     )
        // },
        bottomBar = {
            PlayerBottomAppBar(
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                state.frame?.let { PatternInfoRow(frame = it) }
                // HorizontalDivider(modifier = Modifier.fillMaxWidth())
                PlaybackProgress(state = state, onSeek = { onAction(PlayerAction.OnSeek(it)) })
                // HorizontalDivider(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
                TransportRow(
                    status = state.status,
                    hasNext = state.currentQueueIndex < state.queue.lastIndex,
                    hasPrev = state.currentQueueIndex > 0,
                    isShuffle = state.isShuffle,
                    isLoop = state.isLoop,
                    onToggle = { onAction(PlayerAction.OnPlayPause) },
                    onNext = { onAction(PlayerAction.OnNext) },
                    onPrev = { onAction(PlayerAction.OnPrevious) },
                    onShuffle = { onAction(PlayerAction.OnShuffle) },
                    onLoop = { onAction(PlayerAction.OnLoop) },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        },
        content = { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
                content = {
                    // TODO Tracker info does not show.
                    Text(
                        text = state.moduleName.ifBlank {
                            state.currentModule.name.uppercase().ifBlank { "(untitled)" }
                        },
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = state.moduleType.ifBlank {
                            state.currentModule.extension.uppercase().ifBlank { "???" }
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    state.frame?.let {
                        ChannelMeterGrid(
                            modifier = Modifier.weight(1f),
                            channels = it.channels
                        )
                    }
                }
            )

            if (showQueue) {
                QueueSheet(
                    sheetState = sheetState,
                    queue = state.queue,
                    currentIndex = state.currentQueueIndex,
                    onItemClick = { onAction(PlayerAction.OnQueueClick(it)) },
                    onDismiss = { onAction(PlayerAction.OnQueueSheet(false)) },
                )
            }
        }
    )
}

private val previewQueue = persistentListOf(
    ModuleFile(
        uri = "content://preview/1".toUri(),
        name = "a_journey_into_sound.far",
        sizeBytes = 123456L,
        extension = "far",
        resolvedName = "A Journey Into Sound",
        resolvedType = "Farandole Composer",
    ),
    ModuleFile(
        uri = "content://preview/2".toUri(),
        name = "aegis_-_beneath_the_fallen_stars.it",
        sizeBytes = 1820792L,
        extension = "it",
        resolvedName = "Beneath the Fallen Stars",
        resolvedType = "Impulse Tracker",
    ),
    ModuleFile(
        uri = "content://preview/3".toUri(),
        name = "alpharapii.mod",
        sizeBytes = 45678L,
        extension = "mod",
        resolvedName = "alpharapii",
        resolvedType = "Amiga Protracker/Compatible",
    ),
)

private val previewPlayerState = PlayerUiState(
    status = PlaybackStatus.PLAYING,
    currentModule = previewQueue[1],
    moduleName = "A Journey Into Sound",
    moduleType = "Farandole Composer",
    positionMs = 62000L,
    durationMs = 252849L,
    queue = previewQueue,
    currentQueueIndex = 1,
    isShuffle = false,
    isLoop = false,
    frame = FrameSnapshot(
        position = 2,
        pattern = 17,
        row = 44,
        numRows = 64,
        speed = 4,
        bpm = 125,
        timeMs = 62000,
        totalTimeMs = 252849,
        channels = Array(12) {
            ChannelSnapshot(
                volume = (it + 1) * 5,
                finalVol = (it + 2) * 5,
                pan = 0,
                instrument = 0,
                note = 0,
                period = 0,
            )
        }.toImmutableList(),
        presentationNanos = 0L,
    ),
)

private class PlayerPreviewParameter : PreviewParameterProvider<Pair<PlayerUiState, Boolean>> {
    override val values = sequenceOf(
        previewPlayerState to false,
        previewPlayerState to true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview(
    @PreviewParameter(PlayerPreviewParameter::class) params: Pair<PlayerUiState, Boolean>
) {
    val (state, showQueue) = params
    val density = LocalDensity.current
    val sheetState = SheetState(
        skipPartiallyExpanded = false,
        initialValue = if (showQueue) SheetValue.Expanded else SheetValue.Hidden,
        positionalThreshold = { with(density) { 56.dp.toPx() } },
        velocityThreshold = { with(density) { 125.dp.toPx() } },
    )
    XmpTheme {
        PlayerScreenContent(
            state = state,
            sheetState = sheetState,
            showQueue = showQueue,
            onAction = {},
        )
    }
}
