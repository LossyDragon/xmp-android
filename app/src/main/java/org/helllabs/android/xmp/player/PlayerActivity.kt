package org.helllabs.android.xmp.player

import android.content.*
import android.content.res.Configuration
import android.os.*
import android.util.TypedValue
import android.view.*
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.browser.PlaylistMenu
import org.helllabs.android.xmp.player.viewer.ChannelViewer
import org.helllabs.android.xmp.player.viewer.InstrumentViewer
import org.helllabs.android.xmp.player.viewer.PatternViewer
import org.helllabs.android.xmp.player.viewer.Viewer
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.service.ModInterface
import org.helllabs.android.xmp.service.PlayerCallback
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.FileUtils.basename
import org.helllabs.android.xmp.util.Message.yesNoDialog
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.logE
import org.helllabs.android.xmp.util.logI
import org.helllabs.android.xmp.util.toast

class PlayerActivity : AppCompatActivity() {
    /* actual mod player */
    private var modPlayer: ModInterface? = null

    private var playButton: ImageButton? = null
    private var loopButton: ImageButton? = null
    private var seekBar: SeekBar? = null
    private var progressThread: Thread? = null
    private var seeking = false
    private var shuffleMode = false
    private var loopListMode = false
    private var keepFirst = false
    private var paused = false
    private var showElapsed = false
    private var skipToPrevious = false
    private val infoName = arrayOfNulls<TextView>(2)
    private val infoType = arrayOfNulls<TextView>(2)
    private var infoStatus: TextView? = null
    private var elapsedTime: TextView? = null
    private var titleFlipper: ViewFlipper? = null
    private var flipperPage = 0
    private var fileList: List<String>? = null
    private var start = 0
    private var viewerLayout: FrameLayout? = null
    private val handler = Handler()
    private var totalTime = 0
    private var screenOn = false
    private var screenReceiver: BroadcastReceiver? = null
    private var viewer: Viewer? = null
    private var info: Viewer.Info? = null
    private val modVars = IntArray(10)
    private val seqVars = IntArray(16) // this is MAX_SEQUENCES defined in common.h
    private var currentViewer = 0
    private var display: Display? = null
    private var instrumentViewer: Viewer? = null
    private var channelViewer: Viewer? = null
    private var patternViewer: Viewer? = null
    private var playTime = 0
    private val playerLock = Any() // for sync
    private var sidebar: Sidebar? = null

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            logI("Service connected")
            synchronized(playerLock) {
                modPlayer = ModInterface.Stub.asInterface(service)
                flipperPage = 0
                try {
                    modPlayer?.registerCallback(playerCallback)
                } catch (e: RemoteException) {
                    logE("Can't register player callback")
                }
                if (fileList != null && fileList!!.isNotEmpty()) {
                    // Start new queue
                    playNewMod(fileList!!, start)
                } else {
                    // Reconnect to existing service
                    try {
                        showNewMod()
                        if (modPlayer!!.isPaused) {
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
                // modPlayer = null;
                logI("Service disconnected")
                finish()
            }
        }
    }

    private val playerCallback: PlayerCallback = object : PlayerCallback.Stub() {
        @Throws(RemoteException::class)
        override fun newModCallback() {
            synchronized(playerLock) {
                logD("newModCallback: show module data")
                showNewMod()
                canChangeViewer = true
            }
        }

        @Throws(RemoteException::class)
        override fun endModCallback() {
            synchronized(playerLock) {
                logD("endModCallback: end of module")
                stopUpdate = true
                canChangeViewer = false
            }
        }

        @Throws(RemoteException::class)
        override fun endPlayCallback(result: Int) {
            synchronized(playerLock) {
                logD("endPlayCallback: End progress thread")
                stopUpdate = true
                if (result != PlayerService.RESULT_OK) {
                    runOnUiThread {
                        if (result == PlayerService.RESULT_CANT_OPEN_AUDIO) {
                            toast(R.string.error_opensl)
                        } else if (result == PlayerService.RESULT_NO_AUDIO_FOCUS) {
                            toast(R.string.error_audiofocus)
                        }
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

        @Throws(RemoteException::class)
        override fun pauseCallback() {
            logD("pauseCallback")
            handler.post(setPauseStateRunnable)
        }

        @Throws(RemoteException::class)
        override fun newSequenceCallback() {
            synchronized(playerLock) {
                logD("newSequenceCallback: show new sequence")
                showNewSequence()
            }
        }
    }

    private val setPauseStateRunnable = Runnable {
        synchronized(playerLock) {
            if (modPlayer != null) {
                try {
                    // Set pause status according to external state
                    if (modPlayer!!.isPaused) {
                        pause()
                    } else {
                        unpause()
                    }
                } catch (e: RemoteException) {
                    logE("Can't get pause status")
                }
            }
        }
    }

    private val updateInfoRunnable: Runnable = object : Runnable {
        private var oldSpd = -1
        private var oldBpm = -1
        private var oldPos = -1
        private var oldPat = -1
        private var oldTime = -1
        private var oldShowElapsed = false
        private val c = CharArray(2)
        private val s = StringBuilder()
        override fun run() {
            val p = paused
            if (!p) {
                // update seekbar
                if (!seeking && playTime >= 0) {
                    seekBar!!.progress = playTime
                }

                // get current frame info
                synchronized(playerLock) {
                    if (modPlayer != null) {
                        try {
                            modPlayer!!.getInfo(info!!.values)
                            info!!.time = modPlayer!!.time() / 1000
                            modPlayer!!.getChannelData(
                                info!!.volumes,
                                info!!.finalvols,
                                info!!.pans,
                                info!!.instruments,
                                info!!.keys,
                                info!!.periods
                            )
                        } catch (e: RemoteException) {
                            // fail silently
                        }
                    }
                }

                // display frame info
                if (info!!.values[5] != oldSpd ||
                    info!!.values[6] != oldBpm ||
                    info!!.values[0] != oldPos ||
                    info!!.values[1] != oldPat
                ) {
                    // Ugly code to avoid expensive String.format()
                    s.delete(0, s.length)
                    s.append("Speed:")
                    Util.to02X(c, info!!.values[5])
                    s.append(c)
                    s.append(" BPM:")
                    Util.to02X(c, info!!.values[6])
                    s.append(c)
                    s.append(" Pos:")
                    Util.to02X(c, info!!.values[0])
                    s.append(c)
                    s.append(" Pat:")
                    Util.to02X(c, info!!.values[1])
                    s.append(c)
                    infoStatus!!.text = s
                    oldSpd = info!!.values[5]
                    oldBpm = info!!.values[6]
                    oldPos = info!!.values[0]
                    oldPat = info!!.values[1]
                }

                // display playback time
                if (info!!.time != oldTime || showElapsed != oldShowElapsed) {
                    var t = info!!.time
                    if (t < 0) {
                        t = 0
                    }
                    s.delete(0, s.length)
                    if (showElapsed) {
                        Util.to2d(c, t / 60)
                        s.append(c)
                        s.append(':')
                        Util.to02d(c, t % 60)
                        s.append(c)
                        elapsedTime!!.text = s
                    } else {
                        t = totalTime - t
                        s.append('-')
                        Util.to2d(c, t / 60)
                        s.append(c)
                        s.append(':')
                        Util.to02d(c, t % 60)
                        s.append(c)
                        elapsedTime!!.text = s
                    }
                    oldTime = info!!.time
                    oldShowElapsed = showElapsed
                }
            } // !p

            // always call viewer update (for scrolls during pause)
            synchronized(viewerLayout!!) { viewer!!.update(info, p) }
        }
    }

    private inner class ProgressThread : Thread() {
        override fun run() {
            logI("Start progress thread")
            val frameTime = (1000000000 / FRAME_RATE).toLong()
            var lastTimer = System.nanoTime()
            var now: Long
            playTime = 0
            do {
                if (stopUpdate) {
                    logI("Stop update")
                    break
                }
                synchronized(playerLock) {
                    if (modPlayer != null) {
                        try {
                            playTime = modPlayer!!.time() / 100
                        } catch (e: RemoteException) {
                            // fail silently
                        }
                    }
                }
                if (screenOn) {
                    handler.post(updateInfoRunnable)
                }
                try {
                    while (
                        System.nanoTime().also { now = it } - lastTimer < frameTime && !stopUpdate
                    ) {
                        sleep(10)
                    }
                    lastTimer = now
                } catch (e: InterruptedException) {
                    /* no-op */
                }
            } while (playTime >= 0)
            handler.removeCallbacksAndMessages(null)
            handler.post {
                synchronized(playerLock) {
                    if (modPlayer != null) {
                        logI("Flush interface update")
                        try {
                            // finished playing, we can release the module
                            modPlayer!!.allowRelease()
                        } catch (e: RemoteException) {
                            logE("Can't allow module release")
                        }
                    }
                }
            }
        }
    }

    private fun pause() {
        paused = true
        playButton!!.setImageResource(R.drawable.play)
    }

    private fun unpause() {
        paused = false
        playButton!!.setImageResource(R.drawable.pause)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (viewer != null) {
            viewer!!.setRotation(display!!.rotation)
        }
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
                app.clearFileList()
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

    private fun handleIntentAction(intent: Intent): String? {
        logD("Handing incoming intent")
        val uri = intent.data
        val uriString: String
        uriString = (uri?.toString() ?: return null)
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

    // private void setFont(final TextView name, final String path, final int res) {
    //    final Typeface typeface = Typeface.createFromAsset(this.getAssets(), path);
    //    name.setTypeface(typeface);
    // }
    private fun changeViewer() {
        currentViewer++
        currentViewer %= 3
        synchronized(viewerLayout!!) {
            synchronized(playerLock) {
                if (modPlayer != null) {
                    viewerLayout!!.removeAllViews()
                    when (currentViewer) {
                        0 -> viewer = instrumentViewer
                        1 -> viewer = channelViewer
                        2 -> viewer = patternViewer
                    }
                    viewerLayout!!.addView(viewer)
                    viewer!!.setup(modPlayer!!, modVars)
                    viewer!!.setRotation(display!!.rotation)
                }
            }
        }
    }

    // Sidebar services
    fun toggleAllSequences(): Boolean {
        synchronized(playerLock) {
            if (modPlayer != null) {
                try {
                    return modPlayer!!.toggleAllSequences()
                } catch (e: RemoteException) {
                    logE("Can't toggle all sequences status")
                }
            }
            return false
        }
    }

    val allSequences: Boolean
        get() {
            if (modPlayer != null) {
                try {
                    return modPlayer!!.allSequences
                } catch (e: RemoteException) {
                    logE("Can't get all sequences status")
                }
            }
            return false
        }

    // Click listeners
    fun loopButtonListener(view: View?) {
        synchronized(playerLock) {
            if (modPlayer != null) {
                try {
                    if (modPlayer!!.toggleLoop()) {
                        loopButton!!.setImageResource(R.drawable.loop_on)
                    } else {
                        loopButton!!.setImageResource(R.drawable.loop_off)
                    }
                } catch (e: RemoteException) {
                    logE("Can't get loop status")
                }
            }
        }
    }

    fun playButtonListener(view: View?) {
        // Debug.startMethodTracing("xmp");
        synchronized(playerLock) {
            logD("Play/pause button pressed (paused=$paused)")
            if (modPlayer != null) {
                try {
                    modPlayer!!.pause()
                    if (paused) {
                        unpause()
                    } else {
                        pause()
                    }
                } catch (e: RemoteException) {
                    logE("Can't pause/unpause module")
                }
            }
        }
    }

    fun stopButtonListener(view: View?) {
        // Debug.stopMethodTracing();
        synchronized(playerLock) {
            logD("Stop button pressed")
            if (modPlayer != null) {
                try {
                    modPlayer!!.stop()
                } catch (e1: RemoteException) {
                    logE("Can't stop module")
                }
            }
        }
        paused = false

        // if (progressThread != null && progressThread.isAlive()) {
        // 	try {
        // 		progressThread.join();
        // 	} catch (InterruptedException e) { }
        // }
    }

    fun backButtonListener(view: View?) {
        synchronized(playerLock) {
            logD("Back button pressed")
            if (modPlayer != null) {
                try {
                    if (modPlayer!!.time() > 3000) {
                        modPlayer!!.seek(0)
                        if (paused) {
                            modPlayer!!.pause()
                        }
                    } else {
                        modPlayer!!.prevSong()
                        skipToPrevious = true
                    }
                    unpause()
                } catch (e: RemoteException) {
                    logE("Can't go to previous module")
                }
            }
        }
    }

    fun forwardButtonListener(view: View?) {
        synchronized(playerLock) {
            logD("Next button pressed")
            if (modPlayer != null) {
                try {
                    modPlayer!!.nextSong()
                    unpause()
                } catch (e: RemoteException) {
                    logE("Can't go to next module")
                }
            }
        }
    }

    // Life cycle
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.player_main)
        sidebar = Sidebar(this)
        display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
        logI("Create player interface")

        // INITIALIZE RECEIVER by jwei512
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        screenReceiver = ScreenReceiver()
        registerReceiver(screenReceiver, filter)
        screenOn = true
        if (PlayerService.isLoaded) {
            canChangeViewer = true
        }
        setResult(RESULT_OK)
        val showInfoLine = PrefManager.showInfoLine
        showElapsed = true
        onNewIntent(intent)
        infoName[0] = findViewById<View>(R.id.info_name_0) as TextView
        infoType[0] = findViewById<View>(R.id.info_type_0) as TextView
        infoName[1] = findViewById<View>(R.id.info_name_1) as TextView
        infoType[1] = findViewById<View>(R.id.info_type_1) as TextView
        infoStatus = findViewById<View>(R.id.info_status) as TextView
        elapsedTime = findViewById<View>(R.id.elapsed_time) as TextView
        titleFlipper = findViewById<View>(R.id.title_flipper) as ViewFlipper
        viewerLayout = findViewById<View>(R.id.viewer_layout) as FrameLayout
        viewer = InstrumentViewer(this)
        viewerLayout!!.addView(viewer)
        viewerLayout!!.setOnClickListener {
            synchronized(playerLock) {
                if (canChangeViewer) {
                    changeViewer()
                }
            }
        }
        if (PrefManager.keepScreenOn) {
            titleFlipper!!.keepScreenOn = true
        }

        val font = ResourcesCompat.getFont(applicationContext, R.font.font_michroma)
        for (i in 0..1) {
            infoName[i]!!.typeface = font
            infoName[i]!!.includeFontPadding = false
            infoType[i]!!.typeface = font
            infoType[i]!!.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        }
        if (!showInfoLine) {
            infoStatus!!.visibility = LinearLayout.GONE
            elapsedTime!!.visibility = LinearLayout.GONE
        }
        playButton = findViewById<View>(R.id.play) as ImageButton
        loopButton = findViewById<View>(R.id.loop) as ImageButton
        loopButton!!.setImageResource(R.drawable.loop_off)
        elapsedTime!!.setOnClickListener {
            showElapsed = showElapsed xor true
        }
        seekBar = findViewById<View>(R.id.seek) as SeekBar
        seekBar!!.progress = 0
        seekBar!!.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, b: Boolean) {
                    // do nothing
                }

                override fun onStartTrackingTouch(s: SeekBar) {
                    seeking = true
                }

                override fun onStopTrackingTouch(s: SeekBar) {
                    if (modPlayer != null) {
                        try {
                            modPlayer!!.seek(s.progress * 100)
                            playTime = modPlayer!!.time() / 100
                        } catch (e: RemoteException) {
                            logE("Can't seek to time")
                        }
                    }
                    seeking = false
                }
            }
        )
        instrumentViewer = InstrumentViewer(this)
        channelViewer = ChannelViewer(this)
        patternViewer = PatternViewer(this)
    }

    private fun saveAllSeqPreference() {
        synchronized(playerLock) {
            if (modPlayer != null) {
                try {
                    // Write our all sequences button status to shared prefs
                    val allSeq = modPlayer!!.allSequences
                    if (allSeq != PrefManager.allSequences) {
                        logD("Write all sequences preference")
                        PrefManager.allSequences = allSeq
                    }
                } catch (e: RemoteException) {
                    logE("Can't save all sequences preference")
                }
            }
        }
    }

    public override fun onDestroy() {
        // if (deleteDialog != null) {
        // 	deleteDialog.cancel();
        // }
        saveAllSeqPreference()
        synchronized(playerLock) {
            if (modPlayer != null) {
                try {
                    modPlayer!!.unregisterCallback(playerCallback)
                } catch (e: RemoteException) {
                    logE("Can't unregister player callback")
                }
            }
        }
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

    /*
     * Stop screen updates when screen is off
     */
    override fun onPause() {
        // Screen is about to turn off
        if (ScreenReceiver.wasScreenOn) {
            screenOn = false
        } // else {
        // Screen state not changed
        // }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        screenOn = true
    }

    fun playNewSequence(num: Int) {
        synchronized(playerLock) {
            if (modPlayer != null) {
                try {
                    logI("Set sequence $num")
                    modPlayer!!.setSequence(num)
                } catch (e: RemoteException) {
                    logE("Can't set sequence $num")
                }
            }
        }
    }

    private fun showNewSequence() {
        synchronized(playerLock) {
            if (modPlayer != null) {
                try {
                    modPlayer!!.getModVars(modVars)
                } catch (e: RemoteException) {
                    logE("Can't get new sequence data")
                }
                handler.post(showNewSequenceRunnable)
            }
        }
    }

    private val showNewSequenceRunnable = Runnable {
        val time = modVars[0]
        totalTime = time / 1000
        seekBar!!.progress = 0
        seekBar!!.max = time / 100
        val formattedTime = String.format("%d:%02d", time / 60000, time / 1000 % 60)
        toast("New sequence duration: $formattedTime")
        val sequence = modVars[7]
        sidebar!!.selectSequence(sequence)
    }

    private fun showNewMod() {
        // if (deleteDialog != null) {
        // 	deleteDialog.cancel();
        // }
        handler.post(showNewModRunnable)
    }

    private val showNewModRunnable = Runnable {
        logI("Show new module")
        synchronized(playerLock) {
            if (modPlayer != null) {
                playTime = try {
                    modPlayer!!.getModVars(modVars)
                    modPlayer!!.getSeqVars(seqVars)
                    modPlayer!!.time() / 100
                } catch (e: RemoteException) {
                    logE("Can't get module data")
                    return@Runnable
                }
                var name: String
                var type: String?
                var allSeq: Boolean
                var loop: Boolean
                try {
                    name = modPlayer!!.modName
                    type = modPlayer!!.modType
                    allSeq = modPlayer!!.allSequences
                    loop = modPlayer!!.loop
                    if (name.trim { it <= ' ' }.isEmpty()) {
                        name = basename(modPlayer!!.fileName)
                    }
                } catch (e: RemoteException) {
                    name = ""
                    type = ""
                    allSeq = false
                    loop = false
                    logE("Can't get module name and type")
                }
                val time = modVars[0]
                /*int len = vars[1]; */
                val pat = modVars[2]
                val chn = modVars[3]
                val ins = modVars[4]
                val smp = modVars[5]
                val numSeq = modVars[6]
                sidebar!!.setDetails(pat, ins, smp, chn, allSeq)
                sidebar!!.clearSequences()
                for (i in 0 until numSeq) {
                    sidebar!!.addSequence(i, seqVars[i])
                }
                sidebar!!.selectSequence(0)
                loopButton!!.setImageResource(if (loop) R.drawable.loop_on else R.drawable.loop_off)
                totalTime = time / 1000
                seekBar!!.max = time / 100
                seekBar!!.progress = playTime
                flipperPage = (flipperPage + 1) % 2
                infoName[flipperPage]!!.text = name
                infoType[flipperPage]!!.text = type
                if (skipToPrevious) {
                    titleFlipper!!.setInAnimation(this@PlayerActivity, R.anim.slide_in_left_slow)
                    titleFlipper!!.setOutAnimation(this@PlayerActivity, R.anim.slide_out_right_slow)
                } else {
                    titleFlipper!!.setInAnimation(this@PlayerActivity, R.anim.slide_in_right_slow)
                    titleFlipper!!.setOutAnimation(this@PlayerActivity, R.anim.slide_out_left_slow)
                }
                skipToPrevious = false
                titleFlipper!!.showNext()
                viewer!!.setup(modPlayer!!, modVars)
                viewer!!.setRotation(display!!.rotation)

                /*infoMod.setText(String.format("Channels: %d\n" +
		       			"Length: %d, Patterns: %d\n" +
		       			"Instruments: %d, Samples: %d\n" +
		       			"Estimated play time: %dmin%02ds",
		       			chn, len, pat, ins, smp,
		       			((time + 500) / 60000), ((time + 500) / 1000) % 60));*/

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
            if (modPlayer != null) {
                try {
                    modPlayer!!.play(fileList, start, shuffleMode, loopListMode, keepFirst)
                } catch (e: RemoteException) {
                    logE("Can't play module")
                }
            }
        }
    }

    // Menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (PrefManager.enableDelete) {
            menuInflater.inflate(R.menu.menu_delete, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_delete) {
            yesNoDialog(this, "Delete", "Are you sure to delete this file?") {
                try {
                    if (modPlayer!!.deleteFile()) {
                        toast("File deleted")
                        setResult(RESULT_FIRST_USER)
                        modPlayer!!.nextSong()
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

    companion object {
        const val PARM_SHUFFLE = "shuffle"
        const val PARM_LOOP = "loop"
        const val PARM_START = "start"
        const val PARM_KEEPFIRST = "keepFirst"
        private const val FRAME_RATE = 25

        // this MUST be static (volatile doesn't work!)
        private var stopUpdate = false
        private var canChangeViewer = false
    }
}
