package com.lossydragon.media3.ui.screens.player

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SheetValue.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
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
import com.lossydragon.media3.ui.screens.player.components.ChipList
import com.lossydragon.media3.ui.screens.player.components.DurationsSheet
import com.lossydragon.media3.ui.screens.player.components.PatternInfoRow
import com.lossydragon.media3.ui.screens.player.components.PlaybackProgress
import com.lossydragon.media3.ui.screens.player.components.PlayerBottomAppBar
import com.lossydragon.media3.ui.screens.player.components.QueueSheet
import com.lossydragon.media3.ui.screens.player.components.TransportRow
import com.lossydragon.media3.ui.theme.XmpTheme
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private sealed class PlayerAction {
    data class OnQueueClick(val int: Int) : PlayerAction()
    data class OnDurationClick(val int: Int) : PlayerAction()
    data class OnQueueSheet(val open: Boolean) : PlayerAction()
    data class OnDurationSheet(val open: Boolean) : PlayerAction()
    data class OnSeek(val seek: Long) : PlayerAction()
    data object OnAllSequences : PlayerAction()
    data object OnBack : PlayerAction()
    data object OnInstruments : PlayerAction()
    data object OnLoop : PlayerAction()
    data object OnModInfo : PlayerAction()
    data object OnNext : PlayerAction()
    data object OnPlayPause : PlayerAction()
    data object OnPrevious : PlayerAction()
    data object OnShuffle : PlayerAction()
    data object OnAudioInfo : PlayerAction()
    data object OnSongMessage : PlayerAction()
    data object OnStop : PlayerAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val snackBarHostState = remember { SnackbarHostState() }
    val viewModel = koinViewModel<XmpPlayerViewModel>(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val queueSheetState = rememberBottomSheetState(
        initialValue = Hidden,
        enabledValues = setOf(Hidden, Expanded)
    )
    val durationsSheetState = rememberBottomSheetState(
        initialValue = Hidden,
        enabledValues = setOf(Hidden, Expanded)
    )

    var showQueue by remember { mutableStateOf(false) }
    var showDurations by remember { mutableStateOf(false) }
    var hasLoadedOnce by remember { mutableStateOf(false) }
    var showAudioInfo by remember { mutableStateOf(false) }
    var audioInfoText by remember { mutableStateOf("") }
    var showModInfo by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentModule) {
        if (state.currentModule != null) hasLoadedOnce = true
        if (hasLoadedOnce && state.currentModule == null) onBack()
    }

    LaunchedEffect(showAudioInfo) {
        while (isActive && showAudioInfo) {
            audioInfoText = viewModel.getAudioStats()
            delay(3.seconds)
        }
    }

    if (showModInfo) {
        PlayerAlertDialog(
            onDismissRequest = { showModInfo = false },
            icon = Icons.Default.MusicNote,
            title = "Mod Info",
            content = {
                Column {
                    Text(text = "Number of Channels: ${state.numChannels}")
                    Text(text = "Number of Instruments: ${state.numInstruments}")
                    Text(text = "Number of Patterns: ${state.numPatterns}")
                    Text(text = "Number of Samples: ${state.numSamples}")
                    Text(text = "Number of Sequences ${state.numSequences}")
                }
            },
        )
    }

    if (showAudioInfo) {
        PlayerAlertDialog(
            onDismissRequest = { showAudioInfo = false },
            icon = Icons.Default.Info,
            title = "Audio Info",
            content = { Text(text = audioInfoText) },
        )
    }

    if (state.songInstruments.isNotEmpty()) {
        PlayerAlertDialog(
            onDismissRequest = viewModel::closeModInstruments,
            icon = Icons.AutoMirrored.Filled.List,
            title = "Song Instruments",
            content = {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    content = {
                        items(
                            items = state.songInstruments,
                            itemContent = { Text(text = it) }
                        )
                    }
                )
            },
        )
    }

    if (state.songMessage.isNotBlank()) {
        PlayerAlertDialog(
            onDismissRequest = viewModel::closeModComment,
            icon = Icons.AutoMirrored.Filled.Message,
            title = "Song Message",
            content = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier.verticalScroll(scrollState),
                    content = { Text(text = state.songMessage) }
                )
            },
        )
    }

    PlayerScreenContent(
        modifier = modifier,
        snackBarHostState = snackBarHostState,
        state = state,
        queueSheetState = queueSheetState,
        durationsSheetState = durationsSheetState,
        showQueue = showQueue,
        showDurations = showDurations,
        onAction = { action ->
            when (action) {
                PlayerAction.OnBack -> onBack()

                PlayerAction.OnPlayPause -> viewModel.togglePlayPause()

                PlayerAction.OnStop -> viewModel.stop()

                PlayerAction.OnPrevious -> viewModel.previous()

                PlayerAction.OnNext -> viewModel.next()

                PlayerAction.OnShuffle -> viewModel.toggleShuffle()

                PlayerAction.OnLoop -> viewModel.toggleLoop()

                PlayerAction.OnAllSequences -> viewModel.toggleAllSequences()

                PlayerAction.OnInstruments -> {
                    if (!viewModel.getModInstruments()) {
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                message = "No instruments to display"
                            )
                        }
                    }
                }

                PlayerAction.OnModInfo -> showModInfo = true

                PlayerAction.OnAudioInfo -> showAudioInfo = true

                PlayerAction.OnSongMessage -> {
                    if (!viewModel.getModComment()) {
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                message = "No song message to display"
                            )
                        }
                    }
                }

                is PlayerAction.OnSeek -> viewModel.seek(action.seek)

                is PlayerAction.OnQueueClick -> {
                    viewModel.playAtIndex(action.int)
                    scope.launch { queueSheetState.hide() }
                }

                is PlayerAction.OnQueueSheet -> showQueue = action.open

                is PlayerAction.OnDurationClick -> {
                    viewModel.setSequence(action.int)
                    scope.launch { durationsSheetState.hide() }
                }

                is PlayerAction.OnDurationSheet -> showDurations = action.open
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScreenContent(
    modifier: Modifier = Modifier,
    snackBarHostState: SnackbarHostState,
    state: PlayerUiState,
    queueSheetState: SheetState,
    durationsSheetState: SheetState,
    showQueue: Boolean,
    showDurations: Boolean,
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
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = {
                            Text(
                                text = state.moduleName,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = state.moduleType,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                },
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
                    Spacer(modifier = Modifier.size(48.dp))
                }
            )
        },
        bottomBar = {
            PlayerBottomAppBar(
                contentPadding = PaddingValues(bottom = 8.dp),
                content = {
                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    state.frame?.let { PatternInfoRow(frame = it) }
                    PlaybackProgress(state = state, onSeek = { onAction(PlayerAction.OnSeek(it)) })
                    Spacer(modifier = Modifier.height(4.dp))
                    ChipList(
                        isShuffle = state.isShuffle,
                        repeatMode = state.repeatMode,
                        isSubSongs = state.playAllSequences,
                        onShuffle = { onAction(PlayerAction.OnShuffle) },
                        onLoop = { onAction(PlayerAction.OnLoop) },
                        onModInfo = { onAction(PlayerAction.OnModInfo) },
                        onShowSongMessage = { onAction(PlayerAction.OnSongMessage) },
                        onShowSongInstruments = { onAction(PlayerAction.OnInstruments) },
                        onPlaySubSongs = { onAction(PlayerAction.OnAllSequences) },
                        onShowDurations = { onAction(PlayerAction.OnDurationSheet(true)) },
                        onAudioInfo = { onAction(PlayerAction.OnAudioInfo) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TransportRow(
                        status = state.status,
                        hasNext = state.currentQueueIndex < state.queue.lastIndex,
                        hasPrev = state.currentQueueIndex > 0,
                        onStop = { onAction(PlayerAction.OnStop) },
                        onPrev = { onAction(PlayerAction.OnPrevious) },
                        onPlayPause = { onAction(PlayerAction.OnPlayPause) },
                        onNext = { onAction(PlayerAction.OnNext) },
                        onQueueSheet = { onAction(PlayerAction.OnQueueSheet(true)) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            )
        },
        content = { contentPadding ->
            state.frame?.let {
                ChannelMeterGrid(
                    modifier = Modifier
                        .padding(contentPadding),
                    channels = it.channels
                )
            }

            if (showDurations) {
                DurationsSheet(
                    sheetState = durationsSheetState,
                    sequenceDurations = state.sequenceDurations,
                    currentSequence = state.currentSequence,
                    onItemClick = { onAction(PlayerAction.OnDurationClick(it)) },
                    onDismiss = { onAction(PlayerAction.OnDurationSheet(false)) },
                )
            }

            if (showQueue) {
                QueueSheet(
                    sheetState = queueSheetState,
                    queue = state.queue,
                    currentIndex = state.currentQueueIndex,
                    onItemClick = { onAction(PlayerAction.OnQueueClick(it)) },
                    onDismiss = { onAction(PlayerAction.OnQueueSheet(false)) },
                )
            }
        }
    )
}

@Composable
private fun PlayerAlertDialog(
    onDismissRequest: () -> Unit,
    icon: ImageVector,
    title: String,
    content: @Composable (() -> Unit)
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(imageVector = icon, contentDescription = null) },
        title = { Text(text = title) },
        text = content,
        confirmButton = {
            TextButton(
                onClick = onDismissRequest,
                content = { Text(text = "Close") }
            )
        }
    )
}

private val previewQueue = Array(10) {
    val number = it + 1
    ModuleFile(
        uri = "content://preview/$number".toUri(),
        name = "a_journey_into_sound.far",
        sizeBytes = 123456L,
        extension = "far",
        resolvedName = "A Journey Into Sound",
        resolvedType = "Farandole Composer",
    )
}.toPersistentList()

private val previewPlayerState = PlayerUiState(
    status = PlaybackStatus.PLAYING,
    currentModule = previewQueue[1],
    moduleName = "A Journey Into Sound",
    moduleType = "Farandole Composer",
    positionMs = 1_000_000L,
    durationMs = 2_000_000L,
    queue = previewQueue,
    currentQueueIndex = 1,
    isShuffle = false,
    repeatMode = 2,
    sequenceDurations = listOf(
        183_000,
        94_000,
        211_000,
        183_000,
        94_000,
        183_000,
        211_000,
        94_000,
        211_000,
    ).toImmutableList(),
    currentSequence = 2,
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

private class PlayerPreviewParameter :
    PreviewParameterProvider<Triple<PlayerUiState, Boolean, Boolean>> {
    override val values = sequenceOf(
        Triple(previewPlayerState, false, false),
        Triple(previewPlayerState, true, false), // queue sheet
        Triple(previewPlayerState, false, true), // durations sheet
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview(
    @PreviewParameter(PlayerPreviewParameter::class) params: Triple<PlayerUiState, Boolean, Boolean>
) {
    val (state, showQueue, showDurations) = params
    val queueSheetState = SheetState(
        enabledValues = setOf(Hidden, Expanded),
        initialValue = if (showQueue) Expanded else Hidden,
        positionalThreshold = { 1f },
        velocityThreshold = { 1f },
    )
    val durationsSheetState = SheetState(
        enabledValues = setOf(Hidden, Expanded),
        initialValue = if (showDurations) Expanded else Hidden,
        positionalThreshold = { 1f },
        velocityThreshold = { 1f },
    )
    XmpTheme {
        PlayerScreenContent(
            state = state,
            snackBarHostState = SnackbarHostState(),
            queueSheetState = queueSheetState,
            durationsSheetState = durationsSheetState,
            showQueue = showQueue,
            showDurations = showDurations,
            onAction = {},
        )
    }
}
