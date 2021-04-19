package org.helllabs.android.xmp.presentation.ui.player

import android.content.*
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.session.MediaControllerCompat.TransportControls
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.afollestad.materialdialogs.MaterialDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.presentation.components.DetailsSheet
import org.helllabs.android.xmp.presentation.components.PlayerButtons
import org.helllabs.android.xmp.presentation.components.PlayerInfo
import org.helllabs.android.xmp.presentation.components.PlayerTimeBar
import org.helllabs.android.xmp.presentation.theme.lightGray
import org.helllabs.android.xmp.presentation.theme.sectionBackground
import org.helllabs.android.xmp.presentation.ui.player.viewer.ChannelViewer
import org.helllabs.android.xmp.presentation.ui.player.viewer.InstrumentViewer
import org.helllabs.android.xmp.presentation.ui.player.viewer.PatternViewer
import org.helllabs.android.xmp.presentation.ui.player.viewer.Viewer
import org.helllabs.android.xmp.presentation.ui.playlistMenu.PlaylistMenu
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.service.utils.*
import org.helllabs.android.xmp.util.*

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private lateinit var modPlayer: PlayerService

    @Inject
    lateinit var eventBus: EventBus

    private val viewModel: PlayerActivityViewModel by viewModels()

    private lateinit var playerDisplay: Display
    private var playerJob: Job? = null
    private val modVars = IntArray(10)
    private val seqVars = IntArray(16) // this is MAX_SEQUENCES defined in common.h
    private var currentViewer = 0
    private var fileList: List<String>? = null
    private var flipperPage = 0
    private var info: Viewer.Info? = null
    private var isBound = false
    private var keepFirst = false
    private var loopListMode = false
    private var playTime = 0F
    private var screenOn = false
    private var screenReceiver: BroadcastReceiver? = null
    private var showHex: Boolean = false
    private var shuffleMode = false
    private var skipToPrevious = false
    private var start = 0
    private var totalTime = 0

    /* Views */
    private lateinit var channelViewer: Viewer
    private lateinit var instrumentViewer: Viewer
    private lateinit var patternViewer: Viewer

    private lateinit var viewer: Viewer
    private lateinit var infoName: Array<TextView>
    private lateinit var infoType: Array<TextView>

    // Update Runnable Loops
    private var oldSpd = -1
    private var oldBpm = -1
    private var oldPos = -1
    private var oldPat = -1
    private var oldTime = -1
    private var oldTotalTime = -1
    private val c = CharArray(2)
    private val s = StringBuilder()

    private val mediaControls: TransportControls
        get() = modPlayer.getMediaSession().controller.transportControls

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            this@PlayerActivity.logI("Service connected")
            val binder = service as PlayerService.PlayerBinder
            modPlayer = binder.getService()
            isBound = true
            flipperPage = 0
            if (fileList != null && fileList!!.isNotEmpty()) {
                // Start new queue
                playNewMod(fileList!!, start)
                // checkPlayState()
            } else {
                // Reconnect to existing service
                showNewMod()
                // checkPlayState()
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            saveAllSeqPreference()
            stopUpdate = true
            isBound = false
            logI("Service unexpectedly disconnected")
            finish()
        }
    }

    // region [REGION] EventBus Listeners
    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun newModEvent(event: NewModCallback) {
        logD("newModCallback: show module data")
        showNewMod()
        canChangeViewer = true
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun endModEvent(event: EndModCallback) {
        logD("endModCallback: end of module")
        stopUpdate = true
        canChangeViewer = false
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun endPlayEvent(event: EndPlayCallback) {
        logD("endPlayCallback: End progress thread")
        stopUpdate = true
        saveAllSeqPreference()

        when (event.result) {
            PlayerService.RESULT_CANT_OPEN_AUDIO -> toast(R.string.error_opensl)
            PlayerService.RESULT_NO_AUDIO_FOCUS -> toast(R.string.error_audiofocus)
            PlayerService.RESULT_WATCHDOG -> toast(R.string.error_watchdog)
            PlayerService.RESULT_OK -> Unit
        }

        playerJob?.cancel()

        if (!isFinishing) {
            finish()
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun pauseEvent(event: PlayStateCallback) {
        logD("pauseCallback")
        // checkPlayState()
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun newSequenceEvent(event: NewSequenceCallback) {
        logD("newSequenceCallback: show new sequence")
        showNewSequence()
    }
// endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_player)
        logI("Create player interface")

        onNewIntent(intent)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.color(R.color.primary)
        window.navigationBarColor = resources.color(R.color.section_background_darker)

        playerDisplay = if (isAtLeastR) {
            display!!
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay!!
        }

        eventBus.register(this)
        // sheet = PlayerSheet(this)

        // INITIALIZE RECEIVER by jwei512
        screenOn = true
        screenReceiver = ScreenReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)

        infoName = arrayOf(
            findViewById(R.id.info_name_0),
            findViewById(R.id.info_name_1)
        )
        infoType = arrayOf(
            findViewById(R.id.info_type_0),
            findViewById(R.id.info_type_1)
        )

        // Viewer
        // Get the background color of the activity.
        var color: Int = android.graphics.Color.parseColor("#FF000000")
        val background = window.decorView.background
        if (background is ColorDrawable) color = background.color
        instrumentViewer = InstrumentViewer(this, color)
        channelViewer = ChannelViewer(this, color)
        patternViewer = PatternViewer(this, color)
        viewer = instrumentViewer

        findViewById<ComposeView>(R.id.composedPlayerLayout).setContent {
            PlayerLayout(
                background = color,
                viewModel = viewModel,
                viewer = viewer,
                onFrameClick = {
                    if (canChangeViewer) {
                        changeViewer()
                    }
                },
                onSeek = {
                    if (isBound) {
                        mediaControls.seekTo((it * 100).toLong())
                        playTime = Xmp.time() / 100F
                    }
                },
                onStop = {
                    if (isBound) {
                        logD("Stop button pressed")
                        mediaControls.stop()
                    }
                },
                onPrev = {
                    if (isBound) {
                        logD("Back button pressed")
                        mediaControls.skipToPrevious()
                        skipToPrevious = true
                    }
                },
                onPlay = {
                    if (isBound) {
                        val isPaused = modPlayer.isPaused()
                        logD("Play/pause button pressed (paused=$isPaused)")
                        if (isPaused) {
                            mediaControls.play()
                            viewModel.setPlaying(true)
                        } else {
                            mediaControls.pause()
                            viewModel.setPlaying(false)
                        }
                    }
                },
                onNext = {
                    if (isBound) {
                        logD("Next button pressed")
                        mediaControls.skipToNext()
                        skipToPrevious = false
                    }
                },
                onRepeat = {
                    if (isBound) {
                        logD("Loop button pressed")
                        viewModel.setRepeat(modPlayer.toggleLoop())
                    }
                },
                onAllSeq = {
                    if (isBound) {
                        val bool = modPlayer.toggleAllSequences()
                        viewModel.setAllSequences(bool)
                    }
                },
                onSequence = {
                    if (isBound) {
                        modPlayer.setSequence(it)
                        viewModel.currentSequence(it)
                    }
                }
            )
        }

        viewer.keepScreenOn = PrefManager.keepScreenOn

        if (PlayerService.isLoaded) {
            canChangeViewer = true
        }

        setResult(RESULT_OK)
    }

    override fun onStop() {
        super.onStop()

        try {
            unbindService(connection)
            logI("Unbind service")
        } catch (e: IllegalArgumentException) {
            logI("Can't unbind service")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        saveAllSeqPreference()

        stopUpdate = true
        playerJob?.cancel()
        playerJob = null

        eventBus.unregister(this)
        unregisterReceiver(screenReceiver)

        isBound = false

        // Clear app cache for any files from intents.
        if (cacheDir.listFiles()!!.isNotEmpty())
            cacheDir.deleteRecursively()
    }

    override fun onPause() {
        // Stop screen updates when screen is off
        if (ScreenReceiver.wasScreenOn) {
            screenOn = false
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        screenOn = true
        showHex = PrefManager.showInfoLineHex
    }

    // We don't have an action bar, so this is only a phone with a hardware menu button.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (PrefManager.enableDelete) {
            menuInflater.inflate(R.menu.menu_delete, menu)
            return true
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_delete) {
            yesNoDialog(
                this,
                R.string.delete,
                getString(R.string.msg_delete_file, modPlayer.getModName()),
                onConfirm = {
                    if (modPlayer.deleteFile()) {
                        toast(R.string.msg_file_deleted)
                        setResult(RESULT_FIRST_USER)
                        mediaControls.skipToNext()
                    } else {
                        toast(R.string.msg_cant_delete)
                    }
                }
            )
        }
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewer.setRotation(playerDisplay.rotation)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        var reconnect = false
        var fromHistory = false

        // When running in background and clicked fom notification, it'll restart the list
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            logD("Brought back to front")
            return
        }

        // Then brought to background, then foreground, tapping the notification will new task it.
        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
            logD("New Task")
            return
        }

        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) {
            logI("Player started from history")
            fromHistory = true
        }

        var path: String? = null
        if (intent.data != null) {
            logD("Intent: ${intent.flags}")
            path = if (intent.action == Intent.ACTION_VIEW) {
                FileUtils.getPathFromUri(this, intent.data!!)
            } else {
                intent.data!!.path
            }
        }

        if (path != null) {
            // from intent filter
            logI("Player started from intent filter $path")
            fileList = listOf(path)
            shuffleMode = false
            loopListMode = false
            keepFirst = false
            start = 0
        } else if (fromHistory) {
            // Oops. We don't want to start service if launched from history and service is not running
            // so run the browser instead.
            logI("Start file browser")
            val browserIntent = Intent(this, PlaylistMenu::class.java)
            browserIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(browserIntent)
            finish()
            return
        } else {
            val extras = intent.extras
            if (extras != null) {
                fileList = XmpApplication.fileList
                shuffleMode = extras.getBoolean(PARM_SHUFFLE)
                loopListMode = extras.getBoolean(PARM_LOOP)
                keepFirst = extras.getBoolean(PARM_KEEPFIRST)
                start = extras.getInt(PARM_START)
                XmpApplication.fileList = null
            } else {
                reconnect = true
            }
        }

        val service = Intent(this, PlayerService::class.java)
        if (!reconnect) {
            logI("Start service")
            startService(service)
        }
        if (!bindService(service, connection, BIND_AUTO_CREATE)) {
            logE("Can't bind to service")
            finish()
        }
    }

    private fun changeViewer() {
        if (isBound) {
            currentViewer++
            currentViewer %= 3
            with(findViewById<FrameLayout>(R.id.composeFrameLayout)) {
                removeAllViews()
                when (currentViewer) {
                    0 -> viewer = instrumentViewer
                    1 -> viewer = channelViewer
                    2 -> viewer = patternViewer
                }
                addView(viewer)
            }
            viewer.setup(modVars)
            viewer.setRotation(playerDisplay.rotation)
        }
    }

    private fun saveAllSeqPreference() {
        // Write our all sequences button status to shared prefs
        if (isBound) {
            val allSeq = modPlayer.getAllSequences()
            if (allSeq != PrefManager.allSequences) {
                logD("Write all sequences preference")
                PrefManager.allSequences = allSeq
            }
        }
    }

    private fun showNewSequence() {
        if (isBound) {
            Xmp.getModVars(modVars)

            val time = modVars[0]
            totalTime = time / 1000

            viewModel.setSeekPos(0F)
            viewModel.setSeekMax(time / 100F)

            toast(getString(R.string.msg_new_seq_duration, time / 60000, time / 1000 % 60))
            val sequence = modVars[7] // Current Sequence
            viewModel.currentSequence(sequence)
        }
    }

    private fun showNewMod() {
        logI("Show new module")

        Xmp.getModVars(modVars)
        Xmp.getSeqVars(seqVars)
        playTime = Xmp.time() / 100F

        val time = modVars[0] // Sequence duration
        // val len = modVars[1] // Module length in patterns
        val pat = modVars[2] // Number of patterns
        val chn = modVars[3] // Tracks per pattern
        val ins = modVars[4] // Number of instruments
        val smp = modVars[5] // Number of samples
        val numSeq = modVars[6] // Number of valid sequences

        viewModel.setDetails(pat, ins, smp, chn)
        viewModel.setAllSequences(modPlayer.getAllSequences())

        val seq = mutableListOf<Int>()
        for (i in 0 until numSeq) {
            seq.add(i, seqVars[i])
        }
        viewModel.numOfSequences(seq)
        viewModel.currentSequence(0)

        totalTime = time / 1000

        viewModel.setSeekPos(playTime)
        viewModel.setSeekMax(time / 100F)

        with(findViewById<ViewFlipper>(R.id.title_flipper)) {
            flipperPage = (flipperPage + 1) % 2
            infoName[flipperPage].text = modPlayer.getModName()
            infoType[flipperPage].text = Xmp.getModType()
            if (skipToPrevious) {
                setInAnimation(this@PlayerActivity, R.anim.slide_in_left_slow)
                setOutAnimation(this@PlayerActivity, R.anim.slide_out_right_slow)
            } else {
                setInAnimation(this@PlayerActivity, R.anim.slide_in_right_slow)
                setOutAnimation(this@PlayerActivity, R.anim.slide_out_left_slow)
            }
            skipToPrevious = false
            showNext()
        }

        viewer.setup(modVars)
        viewer.setRotation(playerDisplay.rotation)

        info = Viewer.Info()
        info!!.type = Xmp.getModType()
        stopUpdate = false

        playerJob?.cancel()
        playerJob = progressJob()
    }

    private fun playNewMod(fileList: List<String>, start: Int) {
        if (isBound) {
            modPlayer.play(fileList, start, shuffleMode, loopListMode, keepFirst)
        }
    }

    private fun progressJob(): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            logI("Start progress thread")
            var frameStartTime: Long
            var frameTime: Long
            playTime = 0F
            do {

                if (stopUpdate) {
                    logI("Stop update")
                    break
                }

                playTime = Xmp.time() / 100F

                if (screenOn && isBound) {
                    if (!modPlayer.isPaused()) {
                        // update seekbar
                        if (playTime >= 0) {
                            viewModel.setSeekPos(playTime)
                        }

                        // get current frame info
                        Xmp.getInfo(info!!.values)
                        info!!.time = Xmp.time() / 1000
                        if (modPlayer.getUpdateData()) {
                            Xmp.getChannelData(
                                info!!.volumes,
                                info!!.finalVols,
                                info!!.pans,
                                info!!.instruments,
                                info!!.keys,
                                info!!.periods
                            )
                        }

                        /* Display frame info */
                        // Frame Info - Speed
                        if (info!!.values[5] != oldSpd) {
                            s.clear()
                            if (showHex) {
                                PlayerUtil.to02X(c, info!!.values[5])
                                s.append(c)
                            } else {
                                info!!.values[5].let {
                                    if (it < 10) s.append(0)
                                    s.append(it)
                                }
                            }
                            viewModel.setInfoSpeed(s.toString())
                            oldSpd = info!!.values[5]
                        }

                        // Frame Info - BPM
                        if (info!!.values[6] != oldBpm) {
                            s.clear()
                            if (showHex) {
                                PlayerUtil.to02X(c, info!!.values[6])
                                s.append(c)
                            } else {
                                info!!.values[6].let {
                                    if (it < 10) s.append(0)
                                    s.append(it)
                                }
                            }
                            viewModel.setInfoBpm(s.toString())
                            oldBpm = info!!.values[6]
                        }

                        // Frame Info - Position
                        if (info!!.values[0] != oldPos) {
                            s.clear()
                            if (showHex) {
                                PlayerUtil.to02X(c, info!!.values[0])
                                s.append(c)
                            } else {
                                info!!.values[0].let {
                                    if (it < 10) s.append(0)
                                    s.append(it)
                                }
                            }
                            viewModel.setInfoPos(s.toString())
                            oldPos = info!!.values[0]
                        }

                        // Frame Info - Pattern
                        if (info!!.values[1] != oldPat) {
                            s.clear()
                            if (showHex) {
                                PlayerUtil.to02X(c, info!!.values[1])
                                s.append(c)
                            } else {
                                info!!.values[1].let {
                                    if (it < 10) s.append(0)
                                    s.append(it)
                                }
                            }
                            viewModel.setInfoPat(s.toString())
                            oldPat = info!!.values[1]
                        }

                        // display playback time
                        if (info!!.time != oldTime) {
                            var t = info!!.time
                            if (t < 0) {
                                t = 0
                            }
                            s.delete(0, s.length)
                            PlayerUtil.to2d(c, t / 60)
                            s.append(c)
                            s.append(":")
                            PlayerUtil.to02d(c, t % 60)
                            s.append(c)

                            viewModel.setTimeNow(s.toString())
                            oldTime = info!!.time
                        }

                        // display total playback time
                        if (totalTime != oldTotalTime) {
                            s.delete(0, s.length)
                            PlayerUtil.to2d(c, totalTime / 60)
                            s.append(c)
                            s.append(":")
                            PlayerUtil.to02d(c, totalTime % 60)
                            s.append(c)

                            viewModel.setTimeTotal(s.toString())
                            oldTotalTime = totalTime
                        }
                    }

                    // always call viewer update (for scrolls during pause)
                    viewer.update(info, modPlayer.isPaused())
                }

                frameStartTime = System.nanoTime()
                frameTime = (System.nanoTime() - frameStartTime) / 1000000

                if (frameTime < FRAME_RATE && !stopUpdate) {
                    delay(FRAME_RATE - frameTime)
                }
            } while (playTime >= 0)

            logI("Flush interface update")
            // finished playing, we can release the module
            if (isBound)
                modPlayer.allowRelease()

            cancel()
        }
    }

    companion object {
        const val PARM_SHUFFLE = "shuffle"
        const val PARM_LOOP = "loop"
        const val PARM_START = "start"
        const val PARM_KEEPFIRST = "keepFirst"

        // Phone CPU's are more than capable enough to do more work with drawing.
        // With android O+, we can use hardware rendering on the canvas, if supported.
        private val FRAME_RATE: Int = 1000 / if (PrefManager.useNewWaveform) 50 else 30

        private var stopUpdate = false
        private var canChangeViewer = false
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PlayerLayout(
    @ColorRes background: Int,
    viewModel: PlayerActivityViewModel,
    viewer: Viewer,
    onFrameClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onStop: () -> Unit,
    onPrev: () -> Unit,
    onPlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    onAllSeq: (Boolean) -> Unit,
    onSequence: (Int) -> Unit,
) {
    BottomSheetScaffold(
        sheetPeekHeight = 155.dp,
        sheetBackgroundColor = sectionBackground,
        sheetShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        sheetContent = {
            PlayerSheetLayout(
                viewModel,
                onSeek = { onSeek(it) },
                onStop = { onStop() },
                onPrev = { onPrev() },
                onPlay = { onPlay() },
                onNext = { onNext() },
                onRepeat = { onRepeat() },
                onAllSeq = { onAllSeq(it) },
                onSequence = { onSequence(it) },
            )
        }
    ) {
        // The Box colors underneath the bottom sheet.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(background))
        ) {
            PlayerFrameLayout(viewer, onFrameClick = { onFrameClick() })
        }
    }
}

@Composable
fun PlayerSheetLayout(
    viewModel: PlayerActivityViewModel,
    onSeek: (Float) -> Unit,
    onStop: () -> Unit,
    onPrev: () -> Unit,
    onPlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    onAllSeq: (Boolean) -> Unit,
    onSequence: (Int) -> Unit,
) {
    PlayerSheetPeekContent(
        viewModel = viewModel,
        onSeek = { onSeek(it) },
        onStop = { onStop() },
        onPrev = { onPrev() },
        onPlay = { onPlay() },
        onNext = { onNext() },
        onRepeat = { onRepeat() },
    )
    PlayerSheetContent(
        viewModel = viewModel,
        onAllSeq = { onAllSeq(it) },
        onSequence = { onSequence(it) }
    )
}

@Composable
fun PlayerSheetPeekContent(
    viewModel: PlayerActivityViewModel,
    onSeek: (Float) -> Unit,
    onStop: () -> Unit,
    onPrev: () -> Unit,
    onPlay: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
) {
    val showInfo by remember { mutableStateOf(PrefManager.showInfoLine) }
    val spd = viewModel.infoSpeed.observeAsState("00")
    val bpm = viewModel.infoBpm.observeAsState("00")
    val pos = viewModel.infoPos.observeAsState("00")
    val pat = viewModel.infoPat.observeAsState("00")
    val now = viewModel.timeNow.observeAsState("-:--")
    val total = viewModel.timeTotal.observeAsState("-:--")
    val position = viewModel.seekPos.observeAsState(0F)
    val positionMax = viewModel.seekMax.observeAsState(0F)
    val isRepeating = viewModel.setRepeat.observeAsState(false)
    val isPlaying = viewModel.setPlaying.observeAsState(true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(155.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_sheet_handle),
            tint = lightGray,
            contentDescription = null
        )
        if (showInfo) {
            Spacer(modifier = Modifier.height(8.dp))
            PlayerInfo(speed = spd.value, bpm = bpm.value, pos = pos.value, pat = pat.value)
            Spacer(modifier = Modifier.height(12.dp))
            PlayerTimeBar(
                currentTime = now.value,
                totalTime = total.value,
                position = position.value,
                range = positionMax.value,
                onSeek = { onSeek(it) },
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        PlayerButtons(
            modifier = if (!showInfo) Modifier.fillMaxHeight() else Modifier,
            onStop = { onStop() },
            onPrev = { onPrev() },
            onPlay = { onPlay() },
            onNext = { onNext() },
            onRepeat = { onRepeat() },
            isPlaying = isPlaying.value,
            isRepeating = isRepeating.value,
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun PlayerSheetContent(
    viewModel: PlayerActivityViewModel,
    onAllSeq: (Boolean) -> Unit,
    onSequence: (Int) -> Unit,
) {
    val context = LocalContext.current
    val info = viewModel.setDetails.observeAsState()
    val allSeq = viewModel.setAllSequences.observeAsState()
    val currentSeq = viewModel.currentSequence.observeAsState()
    val numSeq = viewModel.numOfSequences.observeAsState()

    DetailsSheet(
        onMessage = {
            val message = Xmp.getComment()
            if (message.isNullOrEmpty()) {
                context.toast(R.string.msg_no_song_info)
            } else {
                MaterialDialog(context).show {
                    title(R.string.dialog_title_song_message)
                    message(text = message)
                    positiveButton(R.string.ok)
                }
            }
        },
        moduleInfo = info.value!!,
        playAllSeq = allSeq.value!!,
        onAllSeq = { onAllSeq(it) },
        sequences = numSeq.value!!,
        currentSequence = currentSeq.value!!,
        onSequence = { onSequence(it) },
    )
}

@Composable
fun PlayerFrameLayout(viewer: Viewer, onFrameClick: () -> Unit) {
    AndroidView(
        modifier = Modifier.padding(bottom = 155.dp),
        factory = { context ->
            FrameLayout(context).apply {
                id = R.id.composeFrameLayout
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                addView(viewer)
                click { onFrameClick() }
            }
        }
    )
}
