package org.helllabs.android.xmp.player

import android.content.*
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import javax.inject.Inject
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.browser.PlaylistMenu
import org.helllabs.android.xmp.databinding.ActivityPlayerBinding
import org.helllabs.android.xmp.player.viewer.ChannelViewer
import org.helllabs.android.xmp.player.viewer.InstrumentViewer
import org.helllabs.android.xmp.player.viewer.PatternViewer
import org.helllabs.android.xmp.player.viewer.Viewer
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.service.utils.*
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.FileUtils.basename

// TODO: Animate Play/Pause button

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    internal lateinit var binder: ActivityPlayerBinding
    private lateinit var modPlayer: PlayerService
    private var isBound = false

    @Inject
    lateinit var eventBus: EventBus

    /* Views */
    private lateinit var channelViewer: Viewer
    private lateinit var instrumentViewer: Viewer
    private lateinit var loopButton: AppCompatImageButton
    private lateinit var nextButton: AppCompatImageButton
    private lateinit var patternViewer: Viewer
    private lateinit var playButton: AppCompatImageButton
    private lateinit var playerFlipper: ViewFlipper
    private lateinit var playerLayout: FrameLayout
    private lateinit var prevButton: AppCompatImageButton
    private lateinit var seekBar: AppCompatSeekBar
    private lateinit var sheet: PlayerSheet
    private lateinit var stopButton: AppCompatImageButton
    private lateinit var timeNow: MaterialTextView
    private lateinit var timeTotal: MaterialTextView
    private lateinit var viewer: Viewer
    private val infoName = arrayOfNulls<TextView>(2)
    private val infoType = arrayOfNulls<TextView>(2)

    private val handler = Handler()
    private val modVars = IntArray(10)
    private val playerLock = Any() // for sync
    private val seqVars = IntArray(16) // this is MAX_SEQUENCES defined in common.h
    private var currentViewer = 0
    private var display: Display? = null
    private var fileList: List<String>? = null
    private var flipperPage = 0
    private var info: Viewer.Info? = null
    private var keepFirst = false
    private var loopListMode = false
    private var paused = false
    private var playTime = 0
    private var progressThread: Thread? = null
    private var screenOn = false
    private var screenReceiver: BroadcastReceiver? = null
    private var seeking = false
    private var shuffleMode = false
    private var skipToPrevious = false
    private var start = 0
    private var totalTime = 0

    private var showHex = PrefManager.showInfoLineHex

    // Update Runnable Loops
    private var oldSpd = -1
    private var oldBpm = -1
    private var oldPos = -1
    private var oldPat = -1
    private var oldTime = -1
    private var oldTotalTime = -1
    private val c = CharArray(2)
    private val s = StringBuilder()

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            logI("Service connected")
            synchronized(playerLock) {
                modPlayer = (service as PlayerService.PlayerBinder).service
                isBound = true
                flipperPage = 0
                if (fileList != null && fileList!!.isNotEmpty()) {
                    // Start new queue
                    playNewMod(fileList!!, start)
                } else {
                    // Reconnect to existing service
                    try {
                        showNewMod()
                        if (modPlayer.isPaused()) {
                            pause()
                        } else {
                            unpause()
                        }
                    } catch (e: RemoteException) {
                        logE("Can't get module file name")
                    }
                }
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            saveAllSeqPreference()
            synchronized(playerLock) {
                stopUpdate = true
                logI("Service disconnected")
                finish()
            }
        }
    }

    // region [REGION] EventBus Listeners
    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun newModEvent(event: NewModCallback) {
        synchronized(playerLock) {
            logD("newModCallback: show module data")
            showNewMod()
            canChangeViewer = true
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun endModEvent(event: EndModCallback) {
        synchronized(playerLock) {
            logD("endModCallback: end of module")
            stopUpdate = true
            canChangeViewer = false
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun endPlayEvent(event: EndPlayCallback) {
        synchronized(playerLock) {
            logD("endPlayCallback: End progress thread")
            stopUpdate = true
            if (event.result != PlayerService.RESULT_OK) {
                when (event.result) {
                    PlayerService.RESULT_CANT_OPEN_AUDIO -> toast(R.string.error_opensl)
                    PlayerService.RESULT_NO_AUDIO_FOCUS -> toast(R.string.error_audiofocus)
                }
            }
            if (progressThread != null && progressThread!!.isAlive) {
                try {
                    progressThread!!.join()
                } catch (e: InterruptedException) {
                    /* no-op */
                }
            }
            if (!isFinishing) {
                finish()
            }
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun pauseEvent(event: PlayStateCallback) {
        logD("pauseCallback")
        handler.post {
            synchronized(playerLock) {
                try {
                    modPlayer.let { if (it.isPaused()) pause() else unpause() }
                } catch (e: RemoteException) {
                    logE("Can't get pause status")
                }
            }
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun newSequenceEvent(event: NewSequenceCallback) {
        logD("newSequenceCallback: show new sequence")
        showNewSequence()
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onServiceFinishedCallback(event: OnServiceStopped) {
        synchronized(playerLock) {
            logI("Service finished event")
            saveAllSeqPreference()
            stopUpdate = true
            finish()
        }
    }
    // endregion

    private val updateInfoRunnable: Runnable = Runnable {
        if (!paused) {
            // update seekbar
            if (!seeking && playTime >= 0) {
                seekBar.progress = playTime
            }

            // get current frame info
            synchronized(playerLock) {
                try {
                    modPlayer.let {
                        modPlayer.getInfo(info!!.values)
                        info!!.time = modPlayer.time() / 1000
                        modPlayer.getChannelData(
                            info!!.volumes,
                            info!!.finalVols,
                            info!!.pans,
                            info!!.instruments,
                            info!!.keys,
                            info!!.periods
                        )
                    }
                } catch (e: RemoteException) {
                    // fail silently
                }
            }

            /* Display frame info */
            // Frame Info - Speed
            if (info!!.values[5] != oldSpd) {
                s.delete(0, s.length)
                if (showHex) {
                    Util.to02X(c, info!!.values[5])
                    s.append(c)
                } else {
                    s.append(info!!.values[5])
                }
                findViewById<TextView>(R.id.infoSpeed).text = s
                oldSpd = info!!.values[5]
            }

            // Frame Info - BPM
            if (info!!.values[6] != oldBpm) {
                s.delete(0, s.length)
                if (showHex) {
                    Util.to02X(c, info!!.values[6])
                    s.append(c)
                } else {
                    s.append(info!!.values[6])
                }
                findViewById<TextView>(R.id.infoBpm).text = s
                oldBpm = info!!.values[6]
            }

            // Frame Info - Position
            if (info!!.values[0] != oldPos) {
                s.delete(0, s.length)
                if (showHex) {
                    Util.to02X(c, info!!.values[0])
                    s.append(c)
                } else {
                    s.append(info!!.values[0])
                }
                findViewById<TextView>(R.id.infoPos).text = s
                oldPos = info!!.values[0]
            }

            // Frame Info - Pattern
            if (info!!.values[1] != oldPat) {
                s.delete(0, s.length)
                if (showHex) {
                    Util.to02X(c, info!!.values[1])
                    s.append(c)
                } else {
                    s.append(info!!.values[1])
                }
                findViewById<TextView>(R.id.infoPat).text = s
                oldPat = info!!.values[1]
            }

            // display playback time
            if (info!!.time != oldTime) {
                var t = info!!.time
                if (t < 0) {
                    t = 0
                }
                s.delete(0, s.length)
                Util.to2d(c, t / 60)
                s.append(c)
                s.append(':')
                Util.to02d(c, t % 60)
                s.append(c)

                timeNow.text = s
                oldTime = info!!.time
            }

            // display total playback time
            if (totalTime != oldTotalTime) {
                s.delete(0, s.length)
                Util.to2d(c, totalTime / 60)
                s.append(c)
                s.append(':')
                Util.to02d(c, totalTime % 60)
                s.append(c)

                timeTotal.text = s
                oldTotalTime = totalTime
            }
        }

        // always call viewer update (for scrolls during pause)
        synchronized(playerLayout) {
            viewer.update(info, paused)
        }
    }

//    val allSequences: Boolean
//        get() {
//            if (modPlayer != null) {
//                try {
//                    return modPlayer!!.allSequences
//                } catch (e: RemoteException) {
//                    logE("Can't get all sequences status")
//                }
//            }
//            return false
//        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_player)
        display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
        logI("Create player interface")

        eventBus.register(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ResourcesCompat.getColor(resources, R.color.primary, null)

        // INITIALIZE RECEIVER by jwei512
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        screenReceiver = ScreenReceiver()
        registerReceiver(screenReceiver, filter)
        screenOn = true

        if (PlayerService.isLoaded) {
            canChangeViewer = true
        }

        onNewIntent(intent)

        infoName[0] = findViewById(R.id.player_info_name_0)
        infoType[0] = findViewById(R.id.player_info_type_0)
        infoName[1] = findViewById(R.id.player_info_name_1)
        infoType[1] = findViewById(R.id.player_info_type_1)

        playerLayout = findViewById(R.id.player_layout)
        timeNow = findViewById(R.id.player_time_now)
        timeTotal = findViewById(R.id.player_time_total)
        seekBar = findViewById(R.id.player_seekbar)
        prevButton = findViewById(R.id.player_button_prev)
        nextButton = findViewById(R.id.player_button_forward)
        playButton = findViewById(R.id.player_button_play)
        stopButton = findViewById(R.id.player_button_stop)
        loopButton = findViewById(R.id.player_button_loop)
        playerFlipper = findViewById(R.id.player_title_flipper)

        playButton.setImageResource(R.drawable.ic_pause) // To be removed when animated.

        prevButton.click { onBackButton() }
        nextButton.click { onForwardButton() }
        playButton.click { onPlayButton() }
        stopButton.click { onStopButton() }
        loopButton.click { onLoopButton() }

        sheet = PlayerSheet(this)

        // Get the background color of the activity.
        var color: Int = Color.parseColor("#FF000000")
        val background = window.decorView.background
        if (background is ColorDrawable) color = background.color

        instrumentViewer = InstrumentViewer(this, color)
        channelViewer = ChannelViewer(this, color)
        patternViewer = PatternViewer(this, color)

        viewer = instrumentViewer

        playerLayout.addView(viewer)
        playerLayout.setOnClickListener {
            synchronized(playerLock) {
                if (canChangeViewer) {
                    changeViewer()
                }
            }
        }

        if (PrefManager.keepScreenOn) {
            playerLayout.keepScreenOn = true
        }

        val font = ResourcesCompat.getFont(applicationContext, R.font.font_michroma)
        for (i in 0..1) {
            infoName[i]!!.typeface = font
            infoName[i]!!.includeFontPadding = false
            infoType[i]!!.typeface = font
            infoType[i]!!.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        }

        if (!PrefManager.showInfoLine) {
            timeNow.hide()
            timeTotal.hide()
            findViewById<LinearLayout>(R.id.player_info_line).hide()
        }

        seekBar.progress = 0
        seekBar.setOnSeekBarChangeListener(
            onStartTrackingTouch = { seeking = true },
            onStopTrackingTouch = {
                if (isBound) {
                    modPlayer.seek(it!!.progress * 100)
                    playTime = modPlayer.time() / 100
                }
                seeking = false
            }
        )

        setResult(RESULT_OK)
    }

    public override fun onDestroy() {
        saveAllSeqPreference()

        eventBus.unregister(this)
        unregisterReceiver(screenReceiver)

        try {
            unbindService(connection)
            logI("Unbind service")
        } catch (e: IllegalArgumentException) {
            logI("Can't unbind unregistered service")
        }

        stopUpdate = true
        if (progressThread != null && progressThread!!.isAlive) {
            progressThread!!.interrupt()
            progressThread = null
        }

        super.onDestroy()
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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (PrefManager.enableDelete) {
            menuInflater.inflate(R.menu.menu_delete, menu)
            return true
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_delete) {
            yesNoDialog("Delete", "Are you sure to delete this file?") {
                try {
                    if (modPlayer.deleteFile()) {
                        toast("File deleted")
                        setResult(RESULT_FIRST_USER)
                        modPlayer.nextSong()
                    } else {
                        toast("Can't delete file")
                    }
                } catch (e: RemoteException) {
                    toast("Can't connect service")
                }
            }
        }
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewer.setRotation(display!!.rotation)
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
                handleIntentAction(intent)
            } else {
                intent.data!!.path
            }
        }

        // fileArray = null;
        if (path != null) {
            // from intent filter
            logI("Player started from intent filter")
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
                // fileArray = extras.getStringArray("files");
                val app = application as XmpApplication
                fileList = app.fileList
                shuffleMode = extras.getBoolean(PARM_SHUFFLE)
                loopListMode = extras.getBoolean(PARM_LOOP)
                keepFirst = extras.getBoolean(PARM_KEEPFIRST)
                start = extras.getInt(PARM_START)
                app.fileList = null
            } else {
                reconnect = true
            }
        }

        val service = Intent(this, PlayerService::class.java)
        if (!reconnect) {
            logI("Start service")
            startService(service)
        }
        if (!bindService(service, connection, 0)) {
            logE("Can't bind to service")
            finish()
        }
    }

    private fun pause() {
        paused = true
        playButton.setImageResource(R.drawable.ic_play)
    }

    private fun unpause() {
        paused = false
        playButton.setImageResource(R.drawable.ic_pause)
    }

    private fun handleIntentAction(intent: Intent): String? {
        logD("Handing incoming intent")
        val uri = intent.data
        val uriString: String = (uri?.toString() ?: return null)
        val fileName = "temp." + uriString.substring(uriString.lastIndexOf('.') + 1)
        val output = File(this.externalCacheDir, fileName)

        // Lets delete  the temp file to ensure a clean copy.
        if (output.exists()) {
            if (output.delete()) {
                logD("Temp file deleted.")
            } else {
                logE("Failed to delete temp file!")
            }
        }
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val outputStream: OutputStream = FileOutputStream(output)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream!!.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.close()
            inputStream.close()
        } catch (e: IOException) {
            logE("Error creating temp file --Check Trace--")
            e.printStackTrace()
            return null
        }
        return output.path
    }

    private fun changeViewer() {
        currentViewer++
        currentViewer %= 3
        synchronized(playerLayout) {
            synchronized(playerLock) {
                if (isBound) {
                    playerLayout.removeAllViews()
                    when (currentViewer) {
                        0 -> viewer = instrumentViewer
                        1 -> viewer = channelViewer
                        2 -> viewer = patternViewer
                    }
                    playerLayout.addView(viewer)
                    viewer.setup(modPlayer, modVars)
                    viewer.setRotation(display!!.rotation)
                }
            }
        }
    }

    // Sidebar services
    fun toggleAllSequences(): Boolean {
        synchronized(playerLock) {
            if (isBound) {
                return modPlayer.toggleAllSequences()
            }
            return false
        }
    }

    private fun onLoopButton() {
        synchronized(playerLock) {
            if (isBound) {
                if (modPlayer.toggleLoop()) {
                    loopButton.setImageResource(R.drawable.ic_repeat_one_on)
                } else {
                    loopButton.setImageResource(R.drawable.ic_repeat_one_off)
                }
            }
        }
    }

    private fun onPlayButton() {
        synchronized(playerLock) {
            logD("Play/pause button pressed (paused=$paused)")
            if (isBound) {
                modPlayer.pause()
                if (paused) unpause() else pause()
            }
        }
    }

    private fun onStopButton() {
        synchronized(playerLock) {
            logD("Stop button pressed")
            if (isBound) {
                modPlayer.stop()
            }
        }
        paused = false
    }

    private fun onBackButton() {
        synchronized(playerLock) {
            logD("Back button pressed")
            if (isBound) {
                if (modPlayer.time() > 3000) {
                    modPlayer.seek(0)
                    if (paused) {
                        modPlayer.pause()
                    }
                } else {
                    modPlayer.prevSong()
                    skipToPrevious = true
                }
                unpause()
            }
        }
    }

    private fun onForwardButton() {
        synchronized(playerLock) {
            logD("Next button pressed")
            if (isBound) {
                modPlayer.nextSong()
                unpause()
            }
        }
    }

    private fun saveAllSeqPreference() {
        synchronized(playerLock) {
            // Write our all sequences button status to shared prefs
            val allSeq = modPlayer.getAllSequences()
            if (allSeq != PrefManager.allSequences) {
                logD("Write all sequences preference")
                PrefManager.allSequences = allSeq
            }
        }
    }

    fun playNewSequence(num: Int) {
        synchronized(playerLock) {
            if (isBound)
                modPlayer.setSequence(num)
        }
    }

    private fun showNewSequence() {
        synchronized(playerLock) {
            if (isBound)
                modPlayer.getModVars(modVars)

            handler.post {
                val time = modVars[0]
                totalTime = time / 1000
                seekBar.progress = 0
                seekBar.max = time / 100
                val formattedTime = String.format("%d:%02d", time / 60000, time / 1000 % 60)
                toast("New sequence duration: $formattedTime")
                val sequence = modVars[7]
                sheet.selectSequence(sequence)
            }
        }
    }

    private fun showNewMod() {
        handler.post {
            logI("Show new module")
            synchronized(playerLock) {
                playTime = try {
                    modPlayer.getModVars(modVars)
                    modPlayer.getSeqVars(seqVars)
                    modPlayer.time() / 100
                } catch (e: RemoteException) {
                    logE("Can't get module data")
                    return@post
                }

                var name: String
                var type: String
                var allSeq: Boolean
                var loop: Boolean
                try {
                    name = modPlayer.getModName()
                    type = modPlayer.getModType()
                    allSeq = modPlayer.getAllSequences()
                    loop = modPlayer.getLoop()
                    if (name.trim { it <= ' ' }.isEmpty()) {
                        name = basename(modPlayer.getFileName())
                    }
                } catch (e: RemoteException) {
                    name = ""
                    type = ""
                    allSeq = false
                    loop = false
                    logE("Can't get module name and type")
                }

                val time = modVars[0]
                /* val len = vars[1] */
                val pat = modVars[2]
                val chn = modVars[3]
                val ins = modVars[4]
                val smp = modVars[5]
                val numSeq = modVars[6]

                sheet.run {
                    setDetails(pat, ins, smp, chn, allSeq)
                    clearSequences()
                    for (i in 0 until numSeq) {
                        addSequence(i, seqVars[i])
                    }
                    selectSequence(0)
                }

                loopButton.setImageResource(
                    if (loop) R.drawable.ic_repeat_one_on else R.drawable.ic_repeat_one_off
                )

                totalTime = time / 1000
                seekBar.max = time / 100
                seekBar.progress = playTime
                flipperPage = (flipperPage + 1) % 2
                infoName[flipperPage]!!.text = name
                infoType[flipperPage]!!.text = type

                if (skipToPrevious) {
                    playerFlipper.setInAnimation(this, R.anim.slide_in_left_slow)
                    playerFlipper.setOutAnimation(this, R.anim.slide_out_right_slow)
                } else {
                    playerFlipper.setInAnimation(this, R.anim.slide_in_right_slow)
                    playerFlipper.setOutAnimation(this, R.anim.slide_out_left_slow)
                }

                skipToPrevious = false
                playerFlipper.showNext()
                viewer.setup(modPlayer, modVars)
                viewer.setRotation(display!!.rotation)

                info = Viewer.Info()
                stopUpdate = false

                if (progressThread == null || !progressThread!!.isAlive) {
                    progressThread = ProgressThread()
                    progressThread!!.start()
                }
            }
        }
    }

    private fun playNewMod(fileList: List<String>, start: Int) {
        synchronized(playerLock) {
            if (isBound)
                modPlayer.play(fileList, start, shuffleMode, loopListMode, keepFirst)
        }
    }

    private inner class ProgressThread : Thread("Xmp Progress Thread") {
        var frameStartTime: Long = 0L
        var frameTime: Long = 0L

        override fun run() {
            logI("Start progress thread")
            playTime = 0
            do {
                if (stopUpdate) {
                    logI("Stop update")
                    break
                }

                synchronized(playerLock) {
                    if (isBound)
                        playTime = modPlayer.time() / 100
                }

                if (screenOn) {
                    handler.post(updateInfoRunnable)
                }

                frameStartTime = System.nanoTime()
                frameTime = (System.nanoTime() - frameStartTime) / 1000000

                if (frameTime < FRAME_RATE && !stopUpdate) {
                    try {
                        sleep(FRAME_RATE - frameTime)
                    } catch (e: InterruptedException) {
                        // Ignore
                    }
                }
            } while (playTime >= 0)

            handler.removeCallbacksAndMessages(null)
            handler.post {
                synchronized(playerLock) {
                    logI("Flush interface update")
                    // finished playing, we can release the module
                    if (isBound)
                        modPlayer.allowRelease()
                }
            }
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
