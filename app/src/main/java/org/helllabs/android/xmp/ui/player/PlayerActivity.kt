package org.helllabs.android.xmp.ui.player

import android.content.*
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.view.Display
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.databinding.ActivityPlayerBinding
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.service.utils.*
import org.helllabs.android.xmp.ui.browser.PlaylistMenu
import org.helllabs.android.xmp.ui.player.viewer.ChannelViewer
import org.helllabs.android.xmp.ui.player.viewer.InstrumentViewer
import org.helllabs.android.xmp.ui.player.viewer.PatternViewer
import org.helllabs.android.xmp.ui.player.viewer.Viewer
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.util.*

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    internal lateinit var binder: ActivityPlayerBinding
    private lateinit var modPlayer: PlayerService

    @Inject
    lateinit var eventBus: EventBus

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
    private var playTime = 0
    private var screenOn = false
    private var screenReceiver: BroadcastReceiver? = null
    private var seeking = false
    private var showHex: Boolean = false
    private var shuffleMode = false
    private var skipToPrevious = false
    private var start = 0
    private var totalTime = 0

    /* Views */
    private lateinit var channelViewer: Viewer
    private lateinit var instrumentViewer: Viewer
    private lateinit var patternViewer: Viewer
    private var sheet: PlayerSheet? = null
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

    private val isLoopEnabled
        get() = if (modPlayer.getLoop())
            R.drawable.ic_repeat_one_on
        else
            R.drawable.ic_repeat_one_off

    private val mediaSession: MediaSessionCompat
        get() = modPlayer.getMediaSession()

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            logI("Service connected")
            val binder = service as PlayerService.PlayerBinder
            modPlayer = binder.getService()
            isBound = true
            flipperPage = 0
            if (fileList != null && fileList!!.isNotEmpty()) {
                // Start new queue
                playNewMod(fileList!!, start)
                checkPlayState()
            } else {
                // Reconnect to existing service
                showNewMod()
                checkPlayState()
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
        checkPlayState()
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

        binder = ActivityPlayerBinding.inflate(layoutInflater)

        setContentView(binder.root)
        logI("Create player interface")

        onNewIntent(intent)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ResourcesCompat.getColor(resources, R.color.primary, null)

        playerDisplay = if (isAtLeastR) {
            display!!
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay!!
        }

        eventBus.register(this)
        sheet = PlayerSheet(this)

        // INITIALIZE RECEIVER by jwei512
        screenOn = true
        screenReceiver = ScreenReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)

        // Get the background color of the activity.
        var color: Int = Color.parseColor("#FF000000")
        val background = window.decorView.background
        if (background is ColorDrawable) color = background.color
        instrumentViewer = InstrumentViewer(this, color)
        channelViewer = ChannelViewer(this, color)
        patternViewer = PatternViewer(this, color)
        viewer = instrumentViewer
        binder.viewerLayout.addView(viewer)
        binder.viewerLayout.click {
            if (canChangeViewer) {
                changeViewer()
            }
        }

        infoName = arrayOf(binder.infoName0, binder.infoName1)
        infoType = arrayOf(binder.infoType0, binder.infoType1)

        binder.controlsSheet.apply {
            buttonPrev.click { onBackButton() }
            buttonForward.click { onForwardButton() }
            buttonPlay.click { onPlayButton() }
            buttonStop.click { onStopButton() }
            buttonLoop.click { onLoopButton() }
            seekbar.apply {
                progress = 0
                setOnSeekBarChangeListener(
                    onStartTrackingTouch = { seeking = true },
                    onStopTrackingTouch = {
                        if (isBound) {
                            mediaSession.controller
                                .transportControls.seekTo((it!!.progress * 100).toLong())
                            playTime = Xmp.time() / 100
                        }
                        seeking = false
                    }
                )
            }

            if (!PrefManager.showInfoLine) {
                timeNow.hide()
                timeTotal.hide()
                infoLayout.infoLine.hide()
            }
        }

        if (PrefManager.keepScreenOn) {
            binder.viewerLayout.keepScreenOn = true
        }

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

        sheet = null
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
                getString(R.string.delete),
                getString(R.string.msg_delete_file, modPlayer.getModName())
            ) {
                if (modPlayer.deleteFile()) {
                    toast(R.string.msg_file_deleted)
                    setResult(RESULT_FIRST_USER)
                    mediaSession.controller.transportControls.skipToNext()
                } else {
                    toast(R.string.msg_cant_delete)
                }
            }
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

        logI("New intent")
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) {
            logI("Player started from history")
            fromHistory = true
        }

        var path: String? = null
        if (intent.data != null) {
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
            binder.viewerLayout.removeAllViews()
            when (currentViewer) {
                0 -> viewer = instrumentViewer
                1 -> viewer = channelViewer
                2 -> viewer = patternViewer
            }
            binder.viewerLayout.addView(viewer)
            viewer.setup(modVars)
            viewer.setRotation(playerDisplay.rotation)
        }
    }

    // Sidebar services
    fun toggleAllSequences(): Boolean {
        if (isBound) {
            return modPlayer.toggleAllSequences()
        }
        return false
    }

    private fun onLoopButton() {
        if (isBound) {
            logD("Loop button pressed")
            modPlayer.toggleLoop()
            binder.controlsSheet.buttonLoop.setImageResource(isLoopEnabled)
        }
    }

    private fun onPlayButton() {
        if (isBound) {
            val isPaused = modPlayer.isPaused()
            logD("Play/pause button pressed (paused=$isPaused)")
            if (isPaused) {
                mediaSession.controller.transportControls.play()
            } else {
                mediaSession.controller.transportControls.pause()
            }
        }
    }

    private fun onStopButton() {
        if (isBound) {
            logD("Stop button pressed")
            mediaSession.controller.transportControls.stop()
        }
    }

    private fun onBackButton() {
        if (isBound) {
            logD("Back button pressed")
            mediaSession.controller.transportControls.skipToPrevious()
            skipToPrevious = true
        }
    }

    private fun onForwardButton() {
        if (isBound) {
            logD("Next button pressed")
            mediaSession.controller.transportControls.skipToNext()
            skipToPrevious = false
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

    fun playNewSequence(num: Int) {
        if (isBound) {
            modPlayer.setSequence(num)
        }
    }

    private fun showNewSequence() {
        if (isBound) {
            Xmp.getModVars(modVars)

            val time = modVars[0]
            totalTime = time / 1000
            binder.controlsSheet.seekbar.progress = 0
            binder.controlsSheet.seekbar.max = time / 100
            toast(getString(R.string.msg_new_seq_duration, time / 60000, time / 1000 % 60))
            val sequence = modVars[7] // Current Sequence
            sheet?.selectSequence(sequence)
        }
    }

    private fun showNewMod() {
        logI("Show new module")

        Xmp.getModVars(modVars)
        Xmp.getSeqVars(seqVars)
        playTime = Xmp.time() / 100

        val time = modVars[0] // Sequence duration
        // val len = modVars[1] // Module length in patterns
        val pat = modVars[2] // Number of patterns
        val chn = modVars[3] // Tracks per pattern
        val ins = modVars[4] // Number of instruments
        val smp = modVars[5] // Number of samples
        val numSeq = modVars[6] // Number of valid sequences

        sheet?.let {
            it.setDetails(pat, ins, smp, chn, modPlayer.getAllSequences())
            it.clearSequences()
            for (i in 0 until numSeq) {
                it.addSequence(i, seqVars[i])
            }
            it.selectSequence(0)
        }

        binder.controlsSheet.buttonLoop.setImageResource(isLoopEnabled)

        totalTime = time / 1000
        binder.controlsSheet.seekbar.max = time / 100
        binder.controlsSheet.seekbar.progress = playTime
        flipperPage = (flipperPage + 1) % 2
        infoName[flipperPage].text = modPlayer.getModName()
        infoType[flipperPage].text = Xmp.getModType()

        if (skipToPrevious) {
            binder.titleFlipper.setInAnimation(this, R.anim.slide_in_left_slow)
            binder.titleFlipper.setOutAnimation(this, R.anim.slide_out_right_slow)
        } else {
            binder.titleFlipper.setInAnimation(this, R.anim.slide_in_right_slow)
            binder.titleFlipper.setOutAnimation(this, R.anim.slide_out_left_slow)
        }

        skipToPrevious = false
        binder.titleFlipper.showNext()
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

    private fun checkPlayState() {
        if (isBound) {
            binder.controlsSheet.buttonPlay.apply {
                if (modPlayer.isPaused()) {
                    setImageResource(R.drawable.anim_play_pause)
                } else {
                    setImageResource(R.drawable.anim_pause_play)
                }
            }

            animatePlayButton()
        }
    }

    private fun animatePlayButton() {
        binder.controlsSheet.buttonPlay.drawable.run {
            when (this) {
                is AnimatedVectorDrawable -> start()
                is AnimatedVectorDrawableCompat -> start()
            }
        }
    }

    private fun progressJob(): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            logI("Start progress thread")
            var frameStartTime: Long
            var frameTime: Long
            playTime = 0
            do {

                if (stopUpdate) {
                    logI("Stop update")
                    break
                }

                playTime = Xmp.time() / 100

                if (screenOn && isBound) {
                    if (!modPlayer.isPaused()) {
                        // update seekbar
                        if (!seeking && playTime >= 0) {
                            binder.controlsSheet.seekbar.progress = playTime
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
                            binder.controlsSheet.infoLayout.infoSpeed.text = s
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
                            binder.controlsSheet.infoLayout.infoBpm.text = s
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
                            binder.controlsSheet.infoLayout.infoPos.text = s
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
                            binder.controlsSheet.infoLayout.infoPat.text = s
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

                            binder.controlsSheet.timeNow.text = s
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

                            binder.controlsSheet.timeTotal.text = s
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
