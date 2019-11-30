package org.helllabs.android.xmp.player

import android.content.*
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.*
import android.util.TypedValue
import android.view.Display
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.layout_player.*
import kotlinx.android.synthetic.main.layout_player_controls.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.browser.PlaylistMenu
import org.helllabs.android.xmp.player.viewer.ChannelViewer
import org.helllabs.android.xmp.player.viewer.InstrumentViewer
import org.helllabs.android.xmp.player.viewer.PatternViewer
import org.helllabs.android.xmp.player.viewer.Viewer
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.service.ModInterface
import org.helllabs.android.xmp.service.PlayerCallback
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.FileUtils
import org.helllabs.android.xmp.util.Log
import org.helllabs.android.xmp.util.toast
import org.helllabs.android.xmp.util.yesNoDialog
import java.io.File
import java.io.FileOutputStream
import java.util.*

class PlayerActivity : AppCompatActivity() {

    /* actual mod player */
    private var modPlayer: ModInterface? = null

    private var progressThread: Thread? = null
    private var seeking: Boolean = false
    private var shuffleMode: Boolean = false
    private var loopListMode: Boolean = false
    private var keepFirst: Boolean = false
    private var paused: Boolean = false
    private var showElapsed: Boolean = false
    private var skipToPrevious: Boolean = false
    private val infoName = arrayOfNulls<TextView>(2)
    private val infoType = arrayOfNulls<TextView>(2)
    private var flipperPage: Int = 0
    private var fileList: MutableList<String>? = null
    private var start: Int = 0
    private var prefs: SharedPreferences? = null
    private val handler = Handler()
    private var totalTime: Int = 0
    private var screenOn: Boolean = false
    private var screenReceiver: BroadcastReceiver? = null
    private var viewer: Viewer? = null
    private var info: Viewer.Info? = null
    private val modVars = IntArray(10)
    private val seqVars = IntArray(16) // this is MAX_SEQUENCES defined in common.h
    private var currentViewer: Int = 0
    private var display: Display? = null
    private var instrumentViewer: Viewer? = null
    private var channelViewer: Viewer? = null
    private var patternViewer: Viewer? = null
    private var playTime: Int = 0
    private val playerLock = Any() // for sync
    private var sheet: Sheet? = null

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.i(TAG, "Service connected")

            synchronized(playerLock) {
                modPlayer = ModInterface.Stub.asInterface(service)
                flipperPage = 0

                try {
                    modPlayer!!.registerCallback(playerCallback)
                } catch (e: RemoteException) {
                    Log.e(TAG, "Can't register player callback")
                }

                if (fileList != null && fileList!!.isNotEmpty()) {
                    // Start new queue
                    playNewMod(fileList, start)
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
                        Log.e(TAG, "Can't get module file name")
                    }
                }
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            saveAllSeqPreference()

            synchronized(playerLock) {
                stopUpdate = true
                // modPlayer = null;
                Log.i(TAG, "Service disconnected")
                finish()
            }
        }
    }

    private val playerCallback = object : PlayerCallback.Stub() {

        @Throws(RemoteException::class)
        override fun newModCallback() {
            synchronized(playerLock) {
                Log.d(TAG, "newModCallback: show module data")
                showNewMod()
                canChangeViewer = true
            }
        }

        @Throws(RemoteException::class)
        override fun endModCallback() {
            synchronized(playerLock) {
                Log.d(TAG, "endModCallback: end of module")
                stopUpdate = true
                canChangeViewer = false
            }
        }

        @Throws(RemoteException::class)
        override fun endPlayCallback(result: Int) {
            synchronized(playerLock) {
                Log.d(TAG, "endPlayCallback: End progress thread")
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
                    }
                }
                if (!isFinishing) {
                    finish()
                }
            }
        }

        @Throws(RemoteException::class)
        override fun pauseCallback() {
            Log.d(TAG, "pauseCallback")
            handler.post(setPauseStateRunnable)
        }

        @Throws(RemoteException::class)
        override fun newSequenceCallback() {
            synchronized(playerLock) {
                Log.d(TAG, "newSequenceCallback: show new sequence")
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
                    Log.e(TAG, "Can't get pause status")
                }
            }
        }
    }

    private val updateInfoRunnable = object : Runnable {
        private var oldSpd = -1
        private var oldBpm = -1
        private var oldPos = -1
        private var oldPat = -1
        private var oldTime = -1
        private var oldShowElapsed: Boolean = false
        private val c = CharArray(2)
        private val s = StringBuilder()

        override fun run() {
            val isPaused = paused

            if (!isPaused) {
                // update seekbar
                if (!seeking && playTime >= 0) {
                    control_player_seek.progress = playTime
                }

                // get current frame info
                synchronized(playerLock) {
                    if (modPlayer != null) {
                        try {
                            modPlayer!!.getInfo(info!!.values)
                            info!!.time = modPlayer!!.time() / 1000

                            modPlayer!!.getChannelData(
                                    info!!.volumes,
                                    info!!.finalVols,
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
                if (info!!.values[5] != oldSpd || info!!.values[6] != oldBpm ||
                        info!!.values[0] != oldPos || info!!.values[1] != oldPat) {
                    // Ugly code to avoid expensive String.format()

                    s.delete(0, s.length)

                    s.append("Speed:")
                    to02X(c, info!!.values[5])
                    s.append(c)

                    s.append(" BPM:")
                    to02X(c, info!!.values[6])
                    s.append(c)

                    s.append(" Pos:")
                    to02X(c, info!!.values[0])
                    s.append(c)

                    s.append(" Pat:")
                    to02X(c, info!!.values[1])
                    s.append(c)

                    control_player_info.text = s

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
                        to2d(c, t / 60)
                        s.append(c)
                        s.append(':')
                        to02d(c, t % 60)
                        s.append(c)

                        control_player_time.text = s
                    } else {
                        t = totalTime - t

                        s.append('-')
                        to2d(c, t / 60)
                        s.append(c)
                        s.append(':')
                        to02d(c, t % 60)
                        s.append(c)

                        control_player_time.text = s
                    }

                    modPlayer!!.currentPlayTime(t)

                    oldTime = info!!.time
                    oldShowElapsed = showElapsed
                }
            }

            // always call viewer update (for scrolls during pause)
            synchronized(viewer_layout!!) {
                viewer?.update(info!!, isPaused)
            }
        }
    }

    val allSequences: Boolean
        get() {
            if (modPlayer != null) {
                try {
                    return modPlayer!!.allSequences
                } catch (e: RemoteException) {
                    Log.e(TAG, "Can't get all sequences status")
                }
            }
            return false
        }

    private val showNewSequenceRunnable = Runnable {
        val time = modVars[0]
        totalTime = time / 1000
        control_player_seek.progress = 0
        control_player_seek.max = time / 100

        toast(text = String.format(
                getString(R.string.msg_new_sequence), time / 60000, time / 1000 % 60))

        sheet!!.selectSequence(modVars[7])
    }

    private val showNewModRunnable = Runnable {
        Log.i(TAG, "Show new module")

        synchronized(playerLock) {
            if (modPlayer != null) {
                try {
                    modPlayer!!.getModVars(modVars)
                    modPlayer!!.getSeqVars(seqVars)
                    playTime = modPlayer!!.time() / 100
                } catch (e: RemoteException) {
                    Log.e(TAG, "Can't get module data")
                    return@Runnable
                }

                var name: String
                var type: String
                var allSeq: Boolean
                var loop: Boolean
                try {
                    name = modPlayer!!.modName
                    type = modPlayer!!.modType
                    allSeq = modPlayer!!.allSequences
                    loop = modPlayer!!.loop

                    if (name.trim { it <= ' ' }.isEmpty()) {
                        name = FileUtils.basename(modPlayer!!.fileName)
                    }
                } catch (e: RemoteException) {
                    name = ""
                    type = ""
                    allSeq = false
                    loop = false
                    Log.e(TAG, "Can't get module name and type")
                }

                val time = modVars[0]
                /*int len = vars[1]; */
                val pat = modVars[2]
                val chn = modVars[3]
                val ins = modVars[4]
                val smp = modVars[5]
                val numSeq = modVars[6]

                sheet!!.setDetails(pat, ins, smp, chn, allSeq)
                sheet!!.clearSequences()
                for (i in 0 until numSeq) {
                    sheet!!.addSequence(i, seqVars[i])
                }
                sheet!!.selectSequence(0)

                control_player_loop.setImageResource(
                        if (loop) R.drawable.ic_repeat_on else R.drawable.ic_repeat_off)

                totalTime = time / 1000
                control_player_seek.max = time / 100
                control_player_seek.progress = playTime

                flipperPage = (flipperPage + 1) % 2

                infoName[flipperPage]!!.text = name
                infoType[flipperPage]!!.text = type

                if (skipToPrevious) {
                    title_flipper.setInAnimation(this, R.anim.slide_in_left_slow)
                    title_flipper.setOutAnimation(this, R.anim.slide_out_right_slow)
                } else {
                    title_flipper.setInAnimation(this, R.anim.slide_in_right_slow)
                    title_flipper.setOutAnimation(this, R.anim.slide_out_left_slow)
                }
                skipToPrevious = false

                title_flipper.showNext()

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

    private inner class ProgressThread : Thread() {

        override fun run() {
            Log.i(TAG, "Start progress thread")

            val frameTime = (1000000000 / FRAME_RATE).toLong()
            var lastTimer = System.nanoTime()
            var now: Long

            playTime = 0

            do {
                if (stopUpdate) {
                    Log.i(TAG, "Stop update")
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
                    do {
                        now = System.nanoTime()
                        sleep(10)
                    } while (now - lastTimer < frameTime && !stopUpdate)

                    lastTimer = now
                } catch (e: InterruptedException) {
                }
            } while (playTime >= 0)

            handler.removeCallbacksAndMessages(null)
            handler.post {
                synchronized(playerLock) {
                    if (modPlayer != null) {
                        Log.i(TAG, "Flush interface update")
                        try {
                            // finished playing, we can release the module
                            modPlayer!!.allowRelease()
                        } catch (e: RemoteException) {
                            Log.e(TAG, "Can't allow module release")
                        }
                    }
                }
            }
        }
    }

    private fun pause() {
        paused = true
        control_player_play.setImageResource(R.drawable.ic_play)
    }

    private fun unpause() {
        paused = false
        control_player_play.setImageResource(R.drawable.ic_pause)
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

        Log.d(TAG, "New intent")

        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) {
            Log.i(TAG, "Player started from history")
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

        // from intent filter
        if (path != null) {
            Log.i(TAG, "Player started from intent filter")
            fileList = ArrayList()
            fileList!!.add(path)
            shuffleMode = false
            loopListMode = false
            keepFirst = false
            start = 0
        } else if (fromHistory) {
            // Oops. We don't want to start service if launched from history and service is not running
            // so run the browser instead.
            Log.i(TAG, "Start file browser")
            val browserIntent = Intent(this, PlaylistMenu::class.java)
            browserIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(browserIntent)
            finish()
            return
        } else {
            val extras = intent.extras
            if (extras != null) {
                // fileArray = extras.getStringArray("files");
                fileList = XmpApplication.instance!!.fileList
                shuffleMode = extras.getBoolean(PARM_SHUFFLE)
                loopListMode = extras.getBoolean(PARM_LOOP)
                keepFirst = extras.getBoolean(PARM_KEEPFIRST)
                start = extras.getInt(PARM_START)
                XmpApplication.instance!!.fileList = null
            } else {
                reconnect = true
            }
        }

        val service = Intent(this, PlayerService::class.java)
        if (!reconnect) {
            Log.i(TAG, "Start service")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(service)
            else
                startService(service)
        }

        if (!bindService(service, connection, 0)) {
            Log.e(TAG, "Can't bind to service")
            finish()
        }
    }

    // Handle Intents provided from other applications.
    private fun handleIntentAction(intent: Intent): String {
        val uri = intent.data!!
        val uriSting = uri.toString()

        Log.d(TAG, "Intent Path $uri")

        val fileName: String = "temp." + uriSting.substring(uriSting.lastIndexOf('.') + 1)
        val outFile = File(externalCacheDir, fileName)

        // Lets delete the file to ensure a clean copy.
        outFile.delete()

        contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(outFile).use { output ->
                input!!.copyTo(output)
            }
        }

        Log.d(TAG, "Captured Intent ${outFile.path}")
        return outFile.path
    }

    private fun changeViewer() {
        currentViewer++
        currentViewer %= 3

        synchronized(viewer_layout!!) {
            synchronized(playerLock) {
                if (modPlayer != null) {
                    viewer_layout!!.removeAllViews()
                    when (currentViewer) {
                        0 -> viewer = instrumentViewer
                        1 -> viewer = channelViewer
                        2 -> viewer = patternViewer
                    }

                    viewer_layout!!.addView(viewer)
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
                    Log.e(TAG, "Can't toggle all sequences status")
                }
            }

            return false
        }
    }

    // Click listeners
    private fun lockButtonListener() {
        synchronized(playerLock) {
            if (modPlayer != null) {
                var dragLock = prefs!!.getBoolean(Preferences.PLAYER_DRAG_LOCK, false)
                val dragStay = prefs!!.getBoolean(Preferences.PLAYER_DRAG_STAY, false)

                dragLock = dragLock xor true
                control_player_lock.setImageResource(
                        if (dragLock) R.drawable.ic_lock else R.drawable.ic_unlock)

                val editor = prefs!!.edit()
                editor.putBoolean(Preferences.PLAYER_DRAG_LOCK, dragLock)
                editor.apply()

                sheet!!.setDragLock(dragLock, dragStay)
            }
        }
    }

    private fun loopButtonListener() {
        synchronized(playerLock) {
            if (modPlayer != null) {
                try {
                    val toggle = modPlayer!!.toggleLoop()
                    control_player_loop.setImageResource(
                            if (toggle) R.drawable.ic_repeat_on else R.drawable.ic_repeat_off)
                } catch (e: RemoteException) {
                    Log.e(TAG, "Can't get loop status")
                }
            }
        }
    }

    private fun playButtonListener() {
        // Debug.startMethodTracing("xmp");
        synchronized(playerLock) {
            if (modPlayer != null) {
                try {
                    modPlayer!!.pause()

                    if (paused) {
                        unpause()
                    } else {
                        pause()
                    }
                } catch (e: RemoteException) {
                    Log.e(TAG, "Can't pause/unpause module")
                }
            }
            Log.d(TAG, "Play/pause button pressed (isPaused=$paused)")
        }
    }

    private fun stopButtonListener() {
        // Debug.stopMethodTracing();
        synchronized(playerLock) {
            Log.d(TAG, "Stop button pressed")
            if (modPlayer != null) {
                try {
                    modPlayer!!.stop()
                } catch (e1: RemoteException) {
                    Log.e(TAG, "Can't stop module")
                }
            }
        }

        paused = false
    }

    private fun backButtonListener() {
        synchronized(playerLock) {
            Log.d(TAG, "Back button pressed")
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
                    Log.e(TAG, "Can't go to previous module")
                }
            }
        }
    }

    private fun forwardButtonListener() {
        synchronized(playerLock) {
            Log.d(TAG, "Next button pressed")
            if (modPlayer != null) {
                try {
                    modPlayer!!.nextSong()
                    unpause()
                } catch (e: RemoteException) {
                    Log.e(TAG, "Can't go to next module")
                }
            }
        }
    }

    // Life cycle
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        sheet = Sheet(this)

        display = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

        Log.i(TAG, "Create player interface")

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
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val showInfoLine = prefs!!.getBoolean(Preferences.SHOW_INFO_LINE, true)
        showElapsed = true

        onNewIntent(intent)

        infoName[0] = info_name_0
        infoType[0] = info_type_0
        infoName[1] = info_name_1
        infoType[1] = info_type_1

        viewer = InstrumentViewer(this)
        viewer_layout!!.addView(viewer)
        viewer_layout!!.setOnClickListener {
            synchronized(playerLock) {
                if (canChangeViewer) {
                    changeViewer()
                }
            }
        }

        if (prefs!!.getBoolean(Preferences.KEEP_SCREEN_ON, false)) {
            title_flipper.keepScreenOn = true
        }

        val font = Typeface.createFromAsset(this.assets, "fonts/Michroma.ttf")

        for (i in 0..1) {
            infoName[i]!!.typeface = font
            infoName[i]!!.includeFontPadding = false
            infoType[i]!!.typeface = font
            infoType[i]!!.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        }

        if (!showInfoLine) {
            control_player_info.visibility = LinearLayout.GONE
            control_player_time.visibility = LinearLayout.GONE
        }

        control_player_play!!.setOnClickListener {
            playButtonListener()
        }

        control_player_loop.apply {
            setImageResource(R.drawable.ic_repeat_off)
            setOnClickListener {
                loopButtonListener()
            }
        }

        val dragLock = prefs!!.getBoolean(Preferences.PLAYER_DRAG_LOCK, false)
        val dragStay = prefs!!.getBoolean(Preferences.PLAYER_DRAG_STAY, false)

        control_player_lock.setImageResource(
                if (dragLock) R.drawable.ic_lock else R.drawable.ic_unlock)
        sheet!!.setDragLock(dragLock, dragStay)
        control_player_lock.setOnClickListener {
            lockButtonListener()
        }

        control_player_stop.setOnClickListener {
            stopButtonListener()
        }

        control_player_back.setOnClickListener {
            backButtonListener()
        }

        control_player_forward.setOnClickListener {
            forwardButtonListener()
        }

        control_player_time.setOnClickListener { showElapsed = showElapsed xor true }

        control_player_seek.progress = 0
        control_player_seek.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {

            override fun onProgressChanged(s: SeekBar, p: Int, b: Boolean) {
                // Do nothing
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
                        Log.e(TAG, "Can't seek to time")
                    }
                }
                seeking = false
            }
        })

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
                    if (allSeq != prefs!!.getBoolean(Preferences.ALL_SEQUENCES, false)) {
                        Log.i(TAG, "Write all sequences preference")
                        val editor = prefs!!.edit()
                        editor.putBoolean(Preferences.ALL_SEQUENCES, allSeq)
                        editor.apply()
                    }
                } catch (e: RemoteException) {
                    Log.e(TAG, "Can't save all sequences preference")
                }
            }
        }
    }

    public override fun onDestroy() {
        saveAllSeqPreference()

        synchronized(playerLock) {
            if (modPlayer != null) {
                try {
                    modPlayer!!.unregisterCallback(playerCallback)
                } catch (e: RemoteException) {
                    Log.e(TAG, "Can't unregister player callback")
                }
            }
        }

        unregisterReceiver(screenReceiver)

        try {
            unbindService(connection)
            Log.i(TAG, "Unbind service")
        } catch (e: IllegalArgumentException) {
            Log.i(TAG, "Can't unbind unregistered service")
        }

        super.onDestroy()
    }

    // Stop screen updates when screen is off
    override fun onPause() {
        // Screen is pref_item_about to turn off
        if (ScreenReceiver.wasScreenOn)
            screenOn = false

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
                    Log.i(TAG, "Set sequence $num")
                    modPlayer!!.setSequence(num)
                } catch (e: RemoteException) {
                    Log.e(TAG, "Can't set sequence $num")
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
                    Log.e(TAG, "Can't get new sequence data")
                }

                handler.post(showNewSequenceRunnable)
            }
        }
    }

    private fun showNewMod() {
        handler.post(showNewModRunnable)
    }

    private fun playNewMod(fileList: List<String>?, start: Int) {
        synchronized(playerLock) {
            if (modPlayer != null) {
                try {
                    modPlayer!!.play(fileList, start, shuffleMode, loopListMode, keepFirst)
                } catch (e: RemoteException) {
                    Log.e(TAG, "Can't play module")
                }
            }
        }
    }

    // Menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (prefs!!.getBoolean(Preferences.ENABLE_DELETE, false)) {
            menuInflater.inflate(R.menu.menu_delete, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_delete) {
            val title = getString(R.string.menu_delete)
            val message = String.format(getString(R.string.msg_delete_file), modPlayer!!.fileName)
            yesNoDialog(title, message) { result ->
                if (result) {
                    try {
                        if (modPlayer!!.deleteFile()) {
                            toast(R.string.msg_file_deleted)
                            setResult(RESULT_FIRST_USER)
                            modPlayer!!.nextSong()
                        } else {
                            toast(R.string.msg_cant_delete)
                        }
                    } catch (e: RemoteException) {
                        toast(R.string.msg_cant_connect_service)
                    }
                }
            }
        }
        return true
    }

    companion object {
        private val TAG = PlayerActivity::class.java.simpleName

        const val PARM_SHUFFLE = "shuffle"
        const val PARM_LOOP = "loop"
        const val PARM_START = "start"
        const val PARM_KEEPFIRST = "keepFirst"
        private const val FRAME_RATE = 25
        private var stopUpdate: Boolean = false
        private var canChangeViewer: Boolean = false
    }
}
