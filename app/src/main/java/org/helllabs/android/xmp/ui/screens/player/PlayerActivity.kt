package org.helllabs.android.xmp.ui.screens.player

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.MainActivity
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.core.Constants
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.setEdgeToEdgeConfig
import org.helllabs.android.xmp.di.appModule
import org.helllabs.android.xmp.di.viewModelModule
import org.helllabs.android.xmp.service.EndPlayback
import org.helllabs.android.xmp.service.PlayerConnection
import org.helllabs.android.xmp.service.PlayerEvent
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.ui.components.MessageDialog
import org.helllabs.android.xmp.ui.components.SingleChoiceListDialog
import org.helllabs.android.xmp.ui.screens.player.components.*
import org.helllabs.android.xmp.ui.screens.player.viewer.ComposeChannelViewer
import org.helllabs.android.xmp.ui.screens.player.viewer.ComposePatternViewer
import org.helllabs.android.xmp.ui.screens.player.viewer.InstrumentViewer
import org.helllabs.android.xmp.ui.theme.XmpTheme
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.context.GlobalContext.getKoinApplicationOrNull
import org.koin.core.context.GlobalContext.startKoin
import timber.log.Timber

class PlayerActivity : ComponentActivity() {

    // private val viewModel by inject<PlayerViewModel>()
    private val viewModel: PlayerViewModel by viewModel() // "Lifecycle awareness" for vm ...ok

    private val prefManager by inject<PrefManager>()
    private val playerConnection by inject<PlayerConnection>()
    private val snackBarHostState = SnackbarHostState()
    private lateinit var screenReceiver: ScreenReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        setEdgeToEdgeConfig()
        super.onCreate(savedInstanceState)
        initKoinIfNeeded()

        Timber.d("onCreate")
        handleIntent(intent)
        setupScreenReceiver()

        setContent {
            val isBound by playerConnection.isBound.collectAsStateWithLifecycle()

            // Handle service lifecycle
            ServiceLifecycleEffects(isBound)

            // Consolidated state collection - single object instead of 12+ separate flows
            val screenState by viewModel.screenState.collectAsStateWithLifecycle()

            // Keep screen on preference
            KeepScreenOnEffect()

            // Snackbar error collection
            LaunchedEffect(Unit) {
                viewModel.softError.collect { snackBarHostState.showSnackbar(it) }
            }

            // Main update loop - triggered by state changes
            UpdateLoopEffect(screenState.ui.serviceConnected)

            XmpTheme {
                // Dialogs
                PlayerDialogs(screenState)

                // Main screen
                PlayerScreen(
                    snackBarHostState = snackBarHostState,
                    screenState = screenState,
                    onControlsEvent = viewModel::handleControlsEvent,
                    onSheetEvent = { handleSheetEvent(it) },
                    onSeekEvent = viewModel::handleSeekEvent,
                    onChangeViewer = viewModel::changeViewer,
                    onSheetVisibleDialog = viewModel::showSheet,
                    onVisibleRowRangeChanged = viewModel::setVisibleRowRange
                )
            }
        }
    }

    // region Lifecycle

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")
        lifecycleScope.launch {
            viewModel.showInfoLine(prefManager.getShowInfoLine())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onConnected(false) // Band-Aid fix
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

    // endregion

    // region Setup

    private fun initKoinIfNeeded() {
        getKoinApplicationOrNull() ?: startKoin {
            androidContext(this@PlayerActivity)
            modules(listOf(viewModelModule, appModule))
        }
    }

    private fun setupScreenReceiver() {
        screenReceiver = ScreenReceiver(onScreenEvent = viewModel::screenOn)
        screenReceiver.register(context = this)
    }

    // endregion

    // region Composable Effects

    @Composable
    private fun ServiceLifecycleEffects(isBound: Boolean) {
        val modPlayer = playerConnection.modPlayer

        LaunchedEffect(isBound) {
            if (isBound && modPlayer != null) {
                Timber.i("Service connected")
                viewModel.onConnected(true)
                viewModel.isPlaying(PlayerService.isPlaying.value)
                modPlayer.playerEvent.collect { handlePlayerEvent(it) }
            } else if (!isBound && viewModel.screenState.value.ui.serviceConnected) {
                Timber.i("Service disconnected, stopping update loop")
                viewModel.onConnected(false)
                setResult(RESULT_OK)
                finish()
            } else if (!isBound) {
                viewModel.onConnected(false)
            }
        }

        LaunchedEffect(isBound) {
            if (isBound && modPlayer != null) {
                val state = viewModel.activityState.value
                if (state.fileList.isNotEmpty()) {
                    Timber.d("Start new queue")
                    viewModel.playNewMod(modPlayer, state.fileList, state.start)
                } else {
                    Timber.d("Reconnect to existing service")
                    viewModel.showNewMod(modPlayer, false)
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
    }

    @Composable
    private fun KeepScreenOnEffect() {
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
    }

    @Composable
    private fun UpdateLoopEffect(serviceConnected: Boolean) {
        LaunchedEffect(serviceConnected) {
            if (serviceConnected) {
                Timber.d("Starting update loop")
                viewModel.startUpdateLoop(playerConnection.modPlayer)
            }
        }
    }

    @Composable
    private fun PlayerDialogs(screenState: PlayerScreenState) {
        val scope = rememberCoroutineScope()
        val choice by viewModel.playlistChoice.collectAsStateWithLifecycle()

        MessageDialog(
            isShowing = screenState.ui.showMessageDialog,
            icon = Icons.Default.Info,
            title = "Comments",
            text = screenState.ui.currentMessage,
            confirmText = stringResource(id = android.R.string.ok),
            onConfirm = viewModel::closeMessage
        )

        SingleChoiceListDialog(
            isShowing = choice != null,
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            title = stringResource(id = R.string.dialog_title_select_playlist),
            selectedIndex = -1,
            textList = persistentListOf(),
            onConfirm = viewModel::addToPlaylist,
            onDismiss = { viewModel.clearPlaylist() },
            onEmpty = {
                scope.launch {
                    snackBarHostState.showSnackbar("Not Implemented")
                    viewModel.clearPlaylist()
                }
            }
        )
    }

    // endregion

    // region Event Handlers

    private fun handleSheetEvent(event: PlayerSheetEvent) {
        val modPlayer = playerConnection.modPlayer ?: return

        when (event) {
            PlayerSheetEvent.OnAllSeq -> {
                viewModel.onAllSequence(modPlayer.toggleAllSequences())
            }

            PlayerSheetEvent.OnMessage -> {
                val comment = String(Xmp.getComment(), StandardCharsets.UTF_8)
                if (comment.isEmpty()) {
                    lifecycleScope.launch {
                        snackBarHostState.showSnackbar("No comment to display")
                    }
                } else {
                    viewModel.showMessage(true, comment.trim())
                }
                viewModel.showSheet(false)
            }

            PlayerSheetEvent.OnAddToPlaylist -> {
                viewModel.onAddToPlaylist(modPlayer.currentFileUri)
            }

            is PlayerSheetEvent.OnSequence -> {
                Timber.i("Set sequence ${event.seq}")
                viewModel.onSequence(modPlayer.setSequence(event.seq))
            }
        }
    }

    private fun handlePlayerEvent(event: PlayerEvent) {
        val modPlayer = playerConnection.modPlayer

        when (event) {
            PlayerEvent.EndMod -> Timber.d("endModCallback: end of module")

            is PlayerEvent.NewMod -> {
                Timber.d("newModCallback: show module data")
                modPlayer?.let { viewModel.showNewMod(it, event.isPrevious) }
            }

            PlayerEvent.NewSequence -> {
                Timber.d("newSequenceCallback")
                modPlayer ?: return
                viewModel.updateModVars()
                viewModel.showNewSequence { time ->
                    val minutes = time / 60000
                    val seconds = time / 1000 % 60
                    lifecycleScope.launch {
                        snackBarHostState.showSnackbar(
                            "New sequence duration: $minutes:${seconds.toString().padStart(2, '0')}"
                        )
                    }
                }
            }

            PlayerEvent.Paused -> modPlayer?.let { viewModel.isPlaying(false) }

            PlayerEvent.Play -> modPlayer?.let { viewModel.isPlaying(true) }

            is PlayerEvent.EndPlay -> {
                Timber.d("endPlayCallback: End progress thread")
                viewModel.onConnected(false)
                val message = when (event.result) {
                    EndPlayback.ERROR_FOCUS -> "Unable to get Audio Focus"
                    EndPlayback.ERROR_WATCHDOG -> "Stopped by watchdog"
                    EndPlayback.ERROR_INIT -> "Unable to initialize native XMP library"
                    else -> ""
                }
                setResult(RESULT_OK, Intent().putExtra("message", message))
                finish()
            }

            is PlayerEvent.ErrorMessage -> {
                lifecycleScope.launch { snackBarHostState.showSnackbar(event.msg) }
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        Timber.d("handleIntent: $intent")

        if (intent == null) {
            Timber.w("Intent was null")
            setResult(RESULT_OK)
            finish()
            return
        }

        // Don't start service if launched from history
        if ((intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            Timber.i("Player started from history")
            startActivity(
                Intent(this, MainActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
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
            if (intent.data != null) return@let

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

        if (!viewModel.screenState.value.ui.serviceConnected) {
            Timber.i("Start service")
            playerConnection.startForegroundService()
        }

        playerConnection.bindService()
    }

    private fun saveAllSeqPreference() {
        Timber.d("Write all sequences preference")
        lifecycleScope.launch {
            prefManager.setAllSequences(playerConnection.modPlayer?.playAllSequences ?: false)
        }
    }

    // endregion
}

// region PlayerScreen Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScreen(
    snackBarHostState: SnackbarHostState,
    screenState: PlayerScreenState,
    onControlsEvent: (PlayerControlsEvent) -> Unit,
    onSeekEvent: (SeekEvent) -> Unit,
    onSheetEvent: (PlayerSheetEvent) -> Unit,
    onChangeViewer: () -> Unit,
    onSheetVisibleDialog: (Boolean) -> Unit,
    onVisibleRowRangeChanged: (IntRange) -> Unit
) {
    val viewFlipperText = remember(screenState.ui.infoTitle, screenState.ui.infoType) {
        screenState.ui.infoTitle to screenState.ui.infoType
    }

    // Module info dialog
    if (screenState.ui.showInfoDialog) {
        ModuleInfoDialog(
            drawerState = screenState.drawer,
            onDismiss = { onSheetVisibleDialog(false) },
            onEvent = onSheetEvent
        )
    }

    // Oboe stats dialog
    var showOboeStats by remember { mutableStateOf(false) }
    var oboeStats by remember { mutableStateOf("") }

    LaunchedEffect(showOboeStats) {
        while (isActive && showOboeStats) {
            oboeStats = Xmp.getAudioStats().let { stats ->
                """
                Audio Glitches: ${stats.xrunCount} (system), ${stats.underrunCount} (app)
                Sample Rate: ${stats.sampleRate} Hz
                Buffer: ${stats.bufferSize} / ${stats.bufferCapacity} frames
                Frames Per Burst: ${stats.framesPerBurst}
                Audio API: ${stats.audioApi}
                Sharing Mode: ${stats.sharingMode}
                """.trimIndent()
            }
            delay(3.seconds)
        }
    }

    MessageDialog(
        isShowing = showOboeStats,
        icon = Icons.Outlined.BarChart,
        title = "Oboe Audio Engine Stats",
        text = oboeStats,
        confirmText = "Close",
        onConfirm = { showOboeStats = false }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            ViewFlipper(
                navigationIcon = {
                    IconButton(onClick = { onSheetVisibleDialog(true) }) {
                        Icon(Icons.Default.Menu, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showOboeStats = true }) {
                        Icon(Icons.Outlined.BarChart, contentDescription = "Oboe Stats")
                    }
                },
                skipToPrevious = screenState.ui.skipToPrevious,
                info = viewFlipperText
            )
        },
        bottomBar = {
            PlayerBottomAppBar {
                PlayerInfo(state = screenState.info)
                Spacer(modifier = Modifier.height(12.dp))
                PlayerSeekBar(state = screenState.time, onSeek = onSeekEvent)
                Spacer(modifier = Modifier.height(12.dp))
                PlayerControls(state = screenState.buttons, onEvent = onControlsEvent)
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

        Box(modifier = modifier.padding(paddingValues)) {
            when (screenState.ui.currentViewer) {
                0 -> InstrumentViewer(
                    onTap = onChangeViewer,
                    channelInfo = screenState.channelInfo,
                    insName = screenState.instrumentNames,
                    isMuted = screenState.isMuted,
                    modVars = screenState.modVars
                )

                1 -> ComposePatternViewer(
                    onTap = onChangeViewer,
                    fi = screenState.frameInfo,
                    isMuted = screenState.isMuted,
                    modType = screenState.ui.infoType,
                    modVars = screenState.modVars,
                    patternData = screenState.patternData,
                    onVisibleRowRangeChanged = onVisibleRowRangeChanged
                )

                2 -> ComposeChannelViewer(
                    onTap = onChangeViewer,
                    channelInfo = screenState.channelInfo,
                    frameInfo = screenState.frameInfo,
                    insName = screenState.instrumentNames,
                    isMuted = screenState.isMuted,
                    modVars = screenState.modVars,
                    sampleData = screenState.sampleData
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleInfoDialog(
    drawerState: PlayerSheetState,
    onDismiss: () -> Unit,
    onEvent: (PlayerSheetEvent) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = "Module Info") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                )
            }
        ) { paddingValues ->
            PlayerSheet(
                modifier = Modifier.padding(paddingValues),
                state = drawerState,
                onEvent = onEvent
            )
        }
    }
}

// endregion
