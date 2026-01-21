package org.helllabs.android.xmp.ui.screens.player

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.helllabs.android.xmp.MainActivity
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.core.Constants
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.setEdgeToEdgeConfig
import org.helllabs.android.xmp.di.appModule
import org.helllabs.android.xmp.di.viewModelModule
import org.helllabs.android.xmp.model.ChannelInfo
import org.helllabs.android.xmp.model.FrameInfo
import org.helllabs.android.xmp.model.ModVars
import org.helllabs.android.xmp.service.EndPlayback
import org.helllabs.android.xmp.service.PlayerConnection
import org.helllabs.android.xmp.service.PlayerEvent
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.ui.components.MessageDialog
import org.helllabs.android.xmp.ui.components.SingleChoiceListDialog
import org.helllabs.android.xmp.ui.screens.player.components.PlayerBottomAppBar
import org.helllabs.android.xmp.ui.screens.player.components.PlayerControls
import org.helllabs.android.xmp.ui.screens.player.components.PlayerControlsEvent
import org.helllabs.android.xmp.ui.screens.player.components.PlayerInfo
import org.helllabs.android.xmp.ui.screens.player.components.PlayerSeekBar
import org.helllabs.android.xmp.ui.screens.player.components.PlayerSheet
import org.helllabs.android.xmp.ui.screens.player.components.PlayerSheetEvent
import org.helllabs.android.xmp.ui.screens.player.components.SeekEvent
import org.helllabs.android.xmp.ui.screens.player.components.ViewFlipper
import org.helllabs.android.xmp.ui.screens.player.viewer.ComposeChannelViewer
import org.helllabs.android.xmp.ui.screens.player.viewer.ComposePatternViewer
import org.helllabs.android.xmp.ui.screens.player.viewer.InstrumentViewer
import org.helllabs.android.xmp.ui.screens.player.viewer.composeSampleChannelInfo
import org.helllabs.android.xmp.ui.screens.player.viewer.composeSampleFrameInfo
import org.helllabs.android.xmp.ui.theme.XmpTheme
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.getKoinApplicationOrNull
import org.koin.core.context.GlobalContext.startKoin
import timber.log.Timber

class PlayerActivity : ComponentActivity() {

    private val viewModel by inject<PlayerViewModel>()

    private val prefManager by inject<PrefManager>()

    private val playerConnection by inject<PlayerConnection>()

    private val snackBarHostState = SnackbarHostState()

    /* Detect if Screen is on or off */
    private lateinit var screenReceiver: ScreenReceiver

    private val modPlayer: PlayerService?
        get() = playerConnection.modPlayer

    private val controls: MediaControllerCompat?
        get() = modPlayer?.mediaController

    override fun onCreate(savedInstanceState: Bundle?) {
        setEdgeToEdgeConfig()
        super.onCreate(savedInstanceState)

        // defensive initialization, when opened via intents
        getKoinApplicationOrNull() ?: startKoin {
            androidContext(this@PlayerActivity)
            modules(listOf(viewModelModule, appModule))
        }

        Timber.d("onCreate")

        handleIntent(intent)

        // Initialize our ScreenReceiver
        screenReceiver = ScreenReceiver(onScreenEvent = viewModel::screenOn)

        // Register ScreenReceiver on/off events
        screenReceiver.register(context = this)

        setContent {
            val isBound by playerConnection.isBound.collectAsStateWithLifecycle()

            // Handle service connection/disconnection
            LaunchedEffect(isBound) {
                if (isBound && modPlayer != null) {
                    Timber.i("Service connected")
                    viewModel.onConnected(true)
                    viewModel.isPlaying(PlayerService.isPlaying.value)

                    modPlayer?.playerEvent?.collect { event ->
                        handlePlayerEvent(event)
                    }
                }
            }

            // Start playback when connected and we have files
            LaunchedEffect(isBound) {
                if (isBound && modPlayer != null) {
                    with(viewModel.activityState.value) {
                        if (fileList.isNotEmpty()) {
                            Timber.d("Start new queue")
                            playNewMod(fileList, start)
                        } else {
                            Timber.d("Reconnect to existing service")
                            viewModel.showNewMod(modPlayer!!, false)
                        }
                    }
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    if (!playerConnection.isBound.value) {
                        Timber.i("Service disconnected")
                        saveAllSeqPreference()
                        viewModel.onConnected(false)
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            }

            // Collect different states
            val buttonState by viewModel.buttonState.collectAsStateWithLifecycle()
            val drawerState by viewModel.drawerState.collectAsStateWithLifecycle()
            val infoState by viewModel.infoState.collectAsStateWithLifecycle()
            val instrumentNames by viewModel.insName.collectAsStateWithLifecycle()
            val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
            val modVars by viewModel.modVars.collectAsStateWithLifecycle()
            val timeState by viewModel.timeState.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val channelInfo by viewModel.channelInfo.collectAsStateWithLifecycle()
            val frameInfo by viewModel.frameInfo.collectAsStateWithLifecycle()
            val sampleData by viewModel.sampleData.collectAsStateWithLifecycle()
            val patternData by viewModel.patternData.collectAsStateWithLifecycle()

            // Add to playlist
            val scope = rememberCoroutineScope()
            // val resources = LocalResources.current
            val choice by viewModel.playlistChoice.collectAsStateWithLifecycle()
            // val playlists by viewModel.playlistList.collectAsStateWithLifecycle()

            // Keep screen on if preference is set.
            val keepScreenOn by prefManager.keepScreenOnFlow()
                .collectAsStateWithLifecycle(initialValue = false)
            DisposableEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }

                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            LaunchedEffect(Unit) {
                viewModel.softError.collectLatest {
                    snackBarHostState.showSnackbar(it)
                }
            }

            // Restart the loop on info change
            LaunchedEffect(uiState.infoTitle, uiState.infoType, uiState.serviceConnected) {
                withContext(Dispatchers.IO) {
                    Timber.d("Start LaunchedEffect Loop")

                    viewModel.resetPlayTime()

                    while (isActive && uiState.serviceConnected) {
                        val currentState = viewModel.uiState.value
                        val activityState = viewModel.activityState.value

                        if (!currentState.serviceConnected) {
                            Timber.i("Service disconnected, stopping update loop")
                            break
                        }

                        if (activityState.playTime < 0) {
                            Timber.i("Stop update")
                            break
                        }

                        if (!currentState.screenOn || modPlayer == null) {
                            Timber.d(
                                "Waiting - Screen On: %s, isPlaying: %s, modPlayer null: %s",
                                currentState.screenOn,
                                viewModel.isPlaying,
                                modPlayer == null
                            )
                            delay(500.milliseconds)
                            continue
                        }

                        // Update ViewerInfo()
                        viewModel.updateViewInfo()

                        // Update sample data
                        viewModel.updateSampleData()

                        // Update pattern data
                        viewModel.updatePatternData()

                        // Get the current playback time
                        val time = Xmp.time().div(100F)
                        viewModel.setPlayTime(time)

                        // Update the seekbar for the current time
                        viewModel.updateSeekBar()

                        // Update playback and total-playback time
                        viewModel.updateInfoTime()

                        // Update Speed, Bpm, Pos, Pat
                        viewModel.updateInfoState()

                        delay(33.milliseconds)
                    }

                    Timber.i("Update loop ended")
                }
            }

            XmpTheme {
                // Hoisted here because of native jni call.
                MessageDialog(
                    isShowing = uiState.showMessageDialog,
                    icon = Icons.Default.Info,
                    title = "Comments",
                    text = uiState.currentMessage,
                    confirmText = stringResource(id = android.R.string.ok),
                    onConfirm = viewModel::closeMessage
                )

                // TODO
                SingleChoiceListDialog(
                    isShowing = choice != null,
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    title = stringResource(id = R.string.dialog_title_select_playlist),
                    selectedIndex = -1,
                    textList = persistentListOf(), // playlists.map { it.name }.toPersistentList(),
                    onConfirm = viewModel::addToPlaylist,
                    onDismiss = {
                        viewModel.clearPlaylist()
                    },
                    onEmpty = {
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                // message = resources.getString(R.string.error_snack_no_playlists)
                                message = "Not Implemented"
                            )
                            viewModel.clearPlaylist()
                        }
                    }
                )

                PlayerScreen(
                    snackBarHostState = snackBarHostState,
                    uiState = uiState,
                    timeState = timeState,
                    drawerState = drawerState,
                    instrumentNames = instrumentNames,
                    modVars = modVars,
                    buttonState = buttonState,
                    frameInfo = frameInfo,
                    channelInfo = channelInfo,
                    isMuted = isMuted,
                    infoState = infoState,
                    sampleData = sampleData,
                    patternData = patternData,
                    onVisibleRowRangeChanged = viewModel::setVisibleRowRange,
                    onControlsEvent = {
                        Timber.d("onControlsEvent $it")
                        when (it) {
                            PlayerControlsEvent.OnNext -> {
                                controls!!.transportControls.skipToNext()
                                viewModel.isPlaying(true)
                            }

                            PlayerControlsEvent.OnPlay -> {
                                if (PlayerService.isPlaying.value) {
                                    controls!!.transportControls.pause()
                                } else {
                                    controls!!.transportControls.play()
                                }
                                viewModel.isPlaying(PlayerService.isPlaying.value)
                            }

                            PlayerControlsEvent.OnPrev -> {
                                controls!!.transportControls.skipToPrevious()
                                viewModel.isPlaying(PlayerService.isPlaying.value)
                            }

                            PlayerControlsEvent.OnStop -> {
                                controls!!.transportControls.stop()
                            }

                            is PlayerControlsEvent.OnRepeat -> {
                                modPlayer!!.toggleLoop(it.value)
                                viewModel.toggleLoop(it.value)
                            }
                        }
                    },
                    onSheetEvent = {
                        when (it) {
                            PlayerSheetEvent.OnAllSeq -> {
                                val res = modPlayer!!.toggleAllSequences()
                                viewModel.onAllSequence(res)
                            }

                            PlayerSheetEvent.OnMessage -> {
                                val comment = String(Xmp.getComment(), StandardCharsets.UTF_8)
                                if (comment.isEmpty()) {
                                    lifecycleScope.launch {
                                        val msg = "No comment to display"
                                        snackBarHostState.showSnackbar(msg)
                                    }
                                } else {
                                    viewModel.showMessage(true, comment.trim())
                                }
                                viewModel.showSheet(false)
                            }

                            PlayerSheetEvent.OnAddToPlaylist -> {
                                viewModel.onAddToPlaylist(modPlayer!!.currentFileUri)
                            }

                            is PlayerSheetEvent.OnSequence -> {
                                Timber.i("Set sequence $it")
                                val res = modPlayer!!.setSequence(it.seq)
                                viewModel.onSequence(res)
                            }
                        }
                    },
                    onSeekEvent = {
                        when (it) {
                            is SeekEvent.OnSeek -> {
                                if (it.isSeeking) {
                                    viewModel.isSeeking(true)
                                } else {
                                    controls!!.transportControls.seekTo(it.value.toLong() * 100)
                                    viewModel.isSeeking(false)
                                    viewModel.setPlayTime(Xmp.time().div(100F))
                                }
                            }
                        }
                    },
                    onChangeViewer = viewModel::changeViewer,
                    onSheetVisibleDialog = viewModel::showSheet,
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")

        lifecycleScope.launch {
            val value = prefManager.getShowInfoLine()
            viewModel.showInfoLine(value)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")

        saveAllSeqPreference()

        playerConnection.unBindService()

        screenReceiver.unregister(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.i("onNewIntent")
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        Timber.d("handleIntent: $intent")

        if (intent == null) {
            Timber.w("Intent was null")
            setResult(RESULT_OK)
            finish()
            return
        }

        // Oops. We don't want to start service if launched from history and service is not running
        // so run the browser instead.
        if ((intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            Timber.i("Player started from history")
            Intent(this, MainActivity::class.java).apply {
                setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }.also(::startActivity)
            finish()
            return
        }

        intent.data?.let { uri ->
            Timber.i("Player started from intent filter")
            viewModel.setActivityState(
                fileList = listOf(uri),
                shuffleMode = false,
                loopListMode = false,
                keepFirst = false,
                start = 0
            )
        }

        intent.extras?.let { extras ->
            // Sanity, because i can't brain
            if (intent.data != null) {
                return@let
            }

            val fileList = PlayerService.fileListUri.toList()
            Timber.i("Player started from intent extras. ${fileList.size} items in list")
            viewModel.setActivityState(
                fileList = fileList,
                shuffleMode = extras.getBoolean(Constants.PARM_SHUFFLE),
                loopListMode = extras.getBoolean(Constants.PARM_LOOP),
                keepFirst = extras.getBoolean(Constants.PARM_KEEPFIRST),
                start = extras.getInt(Constants.PARM_START)
            )

            PlayerService.fileListUri.clear()
        }

        if (!viewModel.uiState.value.serviceConnected) {
            Timber.i("Start service")
            playerConnection.startForegroundService()
        }

        playerConnection.bindService()
    }

    private fun handlePlayerEvent(event: PlayerEvent) {
        when (event) {
            PlayerEvent.EndMod -> {
                Timber.d("endModCallback: end of module")
            }

            is PlayerEvent.NewMod -> {
                Timber.d("newModCallback: show module data")
                viewModel.showNewMod(modPlayer!!, event.isPrevious)
            }

            PlayerEvent.NewSequence -> {
                Timber.d("newSequenceCallback: ")
                if (modPlayer == null) {
                    return
                }

                viewModel.updateModVars()

                viewModel.showNewSequence { time ->
                    val minutes = time / 60000
                    val seconds = time / 1000 % 60
                    val string = "$minutes:${seconds.toString().padStart(2, '0')}"
                    lifecycleScope.launch {
                        snackBarHostState.showSnackbar("New sequence duration: $string")
                    }
                }
            }

            PlayerEvent.Paused -> {
                modPlayer?.let { viewModel.isPlaying(false) }
            }

            PlayerEvent.Play -> {
                modPlayer?.let { viewModel.isPlaying(true) }
            }

            is PlayerEvent.EndPlay -> {
                Timber.d("endPlayCallback: End progress thread")
                val resultIntent = Intent().apply {
                    val message = when (event.result) {
                        EndPlayback.ERROR_FOCUS -> "Unable to get Audio Focus"
                        EndPlayback.ERROR_WATCHDOG -> "Stopped by watchdog"
                        EndPlayback.ERROR_INIT -> "Unable to initialize native XMP library"
                        else -> ""
                    }

                    putExtra("message", message)
                }
                setResult(RESULT_OK, resultIntent)

                finish()
            }

            is PlayerEvent.ErrorMessage -> {
                lifecycleScope.launch {
                    snackBarHostState.showSnackbar(event.msg)
                }
            }
        }
    }

    private fun saveAllSeqPreference() {
        Timber.d("Write all sequences preference")
        // Write our all sequences button status to shared prefs
        lifecycleScope.launch {
            prefManager.setAllSequences(modPlayer?.playAllSequences ?: false)
        }
    }

    private fun playNewMod(fileList: List<Uri>, start: Int) {
        modPlayer?.play(
            fileList = fileList,
            start = start,
            shuffle = viewModel.activityState.value.shuffleMode,
            loopList = viewModel.activityState.value.loopListMode,
            keepFirst = viewModel.activityState.value.keepFirst
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScreen(
    buttonState: PlayerButtonsState,
    drawerState: PlayerSheetState,
    infoState: PlayerInfoState,
    instrumentNames: ImmutableList<String>,
    isMuted: ChannelMuteState,
    modVars: ModVars,
    channelInfo: ChannelInfo,
    frameInfo: FrameInfo,
    snackBarHostState: SnackbarHostState,
    timeState: PlayerTimeState,
    uiState: PlayerState,
    sampleData: SampleDataState,
    patternData: PatternDataState,
    onVisibleRowRangeChanged: (IntRange) -> Unit,
    onChangeViewer: () -> Unit,
    onControlsEvent: (PlayerControlsEvent) -> Unit,
    onSeekEvent: (SeekEvent) -> Unit,
    onSheetEvent: (PlayerSheetEvent) -> Unit,
    onSheetVisibleDialog: (Boolean) -> Unit
) {
    val viewFlipperText by remember(uiState.infoTitle, uiState.infoType) {
        mutableStateOf(Pair(uiState.infoTitle, uiState.infoType))
    }

    if (uiState.showInfoDialog) {
        Dialog(
            onDismissRequest = { onSheetVisibleDialog(false) },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(text = "Module Info") },
                        navigationIcon = {
                            IconButton(onClick = { onSheetVisibleDialog(false) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                PlayerSheet(
                    modifier = Modifier.padding(paddingValues),
                    state = drawerState,
                    onEvent = onSheetEvent
                )
            }
        }
    }

    var showOboeStats by retain { mutableStateOf(false) }
    var oboeStats by remember { mutableStateOf("") }

    LaunchedEffect(showOboeStats) {
        while (isActive && showOboeStats) {
            val stats = Xmp.getAudioStats()

            Timber.d("Fetching Audio Stats")
            oboeStats = """
                Audio Glitches: ${stats.xrunCount} (system), ${stats.underrunCount} (app)
                Sample Rate: ${stats.sampleRate} Hz
                Buffer: ${stats.bufferSize} / ${stats.bufferCapacity} frames
                Frames Per Burst: ${stats.framesPerBurst}
                Audio API: ${stats.audioApi}
                Sharing Mode: ${stats.sharingMode}
            """.trimIndent()

            delay(3.seconds)
        }
    }

    MessageDialog(
        isShowing = showOboeStats,
        icon = Icons.Outlined.BarChart,
        title = "Oboe Audio Engine Stats",
        text = oboeStats,
        confirmText = "Close",
        onConfirm = {
            showOboeStats = false
        },
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            ViewFlipper(
                navigationIcon = {
                    IconButton(onClick = { onSheetVisibleDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showOboeStats = true },
                        content = {
                            Icon(
                                imageVector = Icons.Outlined.BarChart,
                                contentDescription = "Oboe Stats"
                            )
                        }
                    )
                },
                skipToPrevious = uiState.skipToPrevious,
                info = viewFlipperText
            )
        },
        bottomBar = {
            PlayerBottomAppBar {
                PlayerInfo(state = infoState)
                Spacer(modifier = Modifier.height(12.dp))
                PlayerSeekBar(
                    state = timeState,
                    onSeek = onSeekEvent,
                )
                Spacer(modifier = Modifier.height(12.dp))
                PlayerControls(
                    state = buttonState,
                    onEvent = onControlsEvent,
                )
            }
        }
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        val modifier = remember(configuration.orientation) {
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Modifier
            } else {
                Modifier.displayCutoutPadding()
            }
        }

        Box(
            modifier = modifier.padding(paddingValues)
        ) {
            when (uiState.currentViewer) {
                0 -> InstrumentViewer(
                    onTap = onChangeViewer,
                    channelInfo = channelInfo,
                    insName = instrumentNames,
                    isMuted = isMuted,
                    modVars = modVars,
                )

                1 -> ComposePatternViewer(
                    onTap = onChangeViewer,
                    fi = frameInfo,
                    isMuted = isMuted,
                    modType = uiState.infoType,
                    modVars = modVars,
                    patternData = patternData,
                    onVisibleRowRangeChanged = onVisibleRowRangeChanged,
                )

                2 -> ComposeChannelViewer(
                    onTap = onChangeViewer,
                    channelInfo = channelInfo,
                    frameInfo = frameInfo,
                    insName = instrumentNames,
                    isMuted = isMuted,
                    modVars = modVars,
                    sampleData = sampleData,
                )
            }
        }
    }
}

// region [Region] Compose Previews
class PlayerPreviewProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

@Preview
@Composable
private fun Preview_PlayerScreen(
    @PreviewParameter(PlayerPreviewProvider::class) sheetValue: Boolean
) {
    val modVars = remember {
        ModVars(190968, 30, 25, 12, 40, 18, 1, 0)
    }

    val sheetVisible = remember(sheetValue) {
        mutableStateOf(sheetValue)
    }

    XmpTheme {
        PlayerScreen(
            snackBarHostState = SnackbarHostState(),
            uiState = PlayerState(
                infoTitle = "Title 1",
                infoType = "Fast Tracker",
                currentViewer = 0,
                showInfoDialog = sheetVisible.value,
            ),
            infoState = PlayerInfoState(
                infoSpeed = "11",
                infoBpm = "22",
                infoPos = "33",
                infoPat = "44"
            ),
            buttonState = PlayerButtonsState(
                isPlaying = true,
                repeatMode = RepeatMode.REPEAT
            ),
            timeState = PlayerTimeState(
                timeNow = "00:00",
                timeTotal = "00:00",
                seekPos = 25f,
                seekMax = 100f
            ),
            drawerState = PlayerSheetState(
                moduleInfo = listOf(111, 222, 333, 444, 555),
                isPlayAllSequences = true,
                numOfSequences = List(12) { it },
                currentSequence = 2
            ),
            instrumentNames = List(modVars.numInstruments) {
                String.format("%02X %s", it + 1, "Instrument Name")
            }.toPersistentList(),
            modVars = modVars,
            channelInfo = composeSampleChannelInfo(),
            frameInfo = composeSampleFrameInfo(),
            isMuted = ChannelMuteState(
                isMuted = List(modVars.numChannels) {
                    false
                }.toPersistentList()
            ),
            sampleData = SampleDataState(),
            patternData = PatternDataState(),
            onVisibleRowRangeChanged = { },
            onControlsEvent = { },
            onSeekEvent = { },
            onSheetEvent = { },
            onChangeViewer = { },
            onSheetVisibleDialog = {
                sheetVisible.value = it
            },
        )
    }
}
// endregion
