package org.helllabs.android.xmp.player

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayList

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
import org.helllabs.android.xmp.util.Message

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import android.preference.PreferenceManager
import android.util.TypedValue
import android.view.Display
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.ViewFlipper


class PlayerActivity : Activity() {

    private var modPlayer: ModInterface? = null    /* actual mod player */
    private var playButton: ImageButton? = null
    private var loopButton: ImageButton? = null
    private var seekBar: SeekBar? = null
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
    private var infoStatus: TextView? = null
    private var elapsedTime: TextView? = null
    private var titleFlipper: ViewFlipper? = null
    private var flipperPage: Int = 0
    private var fileList: MutableList<String>? = null
    private var start: Int = 0
    private var prefs: SharedPreferences? = null
    private var viewerLayout: FrameLayout? = null
    private val handler = Handler()
    private var totalTime: Int = 0
    private var screenOn: Boolean = false
    private var activity: Activity? = null
    private var screenReceiver: BroadcastReceiver? = null
    private var viewer: Viewer? = null
    private var info: Viewer.Info? = null
    private val modVars = IntArray(10)
    private val seqVars = IntArray(16)        // this is MAX_SEQUENCES defined in common.h
    private var currentViewer: Int = 0
    private var display: Display? = null
    private var instrumentViewer: Viewer? = null
    private var channelViewer: Viewer? = null
    private var patternViewer: Viewer? = null
    private var playTime: Int = 0
    private val playerLock = Any()        // for sync
    private var sidebar: Sidebar? = null

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

                if (fileList != null && !fileList!!.isEmpty()) {
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
                //modPlayer = null;
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
                            Message.toast(this@PlayerActivity, R.string.error_opensl)
                        } else if (result == PlayerService.RESULT_NO_AUDIO_FOCUS) {
                            Message.toast(this@PlayerActivity, R.string.error_audiofocus)
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

                            modPlayer!!.getChannelData(info!!.volumes, info!!.finalVols, info!!.pans,
                                    info!!.instruments, info!!.keys, info!!.periods)
                        } catch (e: RemoteException) {
                            // fail silently
                        }

                    }
                }

                // display frame info
                if (info!!.values[5] != oldSpd || info!!.values[6] != oldBpm
                        || info!!.values[0] != oldPos || info!!.values[1] != oldPat) {
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
            synchronized(viewerLayout!!) {
                viewer!!.update(info!!, p)
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
        seekBar!!.progress = 0
        seekBar!!.max = time / 100
        Message.toast(activity!!, "New sequence duration: " + String.format("%d:%02d", time / 60000, time / 1000 % 60))

        val sequence = modVars[7]
        sidebar!!.selectSequence(sequence)
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

                infoName[flipperPage]!!.setText(name)
                infoType[flipperPage]!!.setText(type)

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
                            modPlayer!!.allowRelease()        // finished playing, we can release the module
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
        var reconnect = false
        var fromHistory = false

        Log.i(TAG, "New intent")

        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) {
            Log.i(TAG, "Player started from history")
            fromHistory = true
        }

        var path: String? = null
        if (intent.data != null) {
            if (intent.action == Intent.ACTION_VIEW) {
                path = handleIntentAction(intent)
            } else {
                path = intent.data!!.path
            }
        }

        //fileArray = null;

        if (path != null) {        // from intent filter
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
                //fileArray = extras.getStringArray("files");
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
            Log.i(TAG, "Start service")
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
        val outFile = File(this.externalCacheDir, fileName)

        //Lets delete the file to ensure a clean copy.
        outFile.delete()

        contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(outFile).use { output ->
                input!!.copyTo(output)
            }
        }

        Log.d(TAG, "Captured Intent ${outFile.path}")
        return outFile.path
    }

    //private void setFont(final TextView name, final String path, final int res) {
    //    final Typeface typeface = Typeface.createFromAsset(this.getAssets(), path);
    //    name.setTypeface(typeface);
    //}

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
                    Log.e(TAG, "Can't toggle all sequences status")
                }

            }
            return false
        }
    }


    // Click listeners

    fun loopButtonListener(view: View) {
        synchronized(playerLock) {
            if (modPlayer != null) {
                try {
                    if (modPlayer!!.toggleLoop()) {
                        loopButton!!.setImageResource(R.drawable.loop_on)
                    } else {
                        loopButton!!.setImageResource(R.drawable.loop_off)
                    }
                } catch (e: RemoteException) {
                    Log.e(TAG, "Can't get loop status")
                }

            }
        }
    }

    fun playButtonListener(view: View) {
        //Debug.startMethodTracing("xmp");
        synchronized(this) {
            Log.d(TAG, "Play/pause button pressed (paused=$paused)")
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
        }
    }

    fun stopButtonListener(view: View) {
        //Debug.stopMethodTracing();

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

        //		if (progressThread != null && progressThread.isAlive()) {
        //			try {
        //				progressThread.join();
        //			} catch (InterruptedException e) { }
        //		}
    }

    fun backButtonListener(view: View) {
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

    fun forwardButtonListener(view: View) {
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

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.player_main)

        sidebar = Sidebar(this)

        activity = this
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

        setResult(Activity.RESULT_OK)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val showInfoLine = prefs!!.getBoolean(Preferences.SHOW_INFO_LINE, true)
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

        if (prefs!!.getBoolean(Preferences.KEEP_SCREEN_ON, false)) {
            titleFlipper!!.keepScreenOn = true
        }

        val font = Typeface.createFromAsset(this.assets, "fonts/Michroma.ttf")

        for (i in 0..1) {
            infoName[i]!!.setTypeface(font)
            infoName[i]!!.setIncludeFontPadding(false)
            infoType[i]!!.setTypeface(font)
            infoType[i]!!.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        }

        if (!showInfoLine) {
            infoStatus!!.visibility = LinearLayout.GONE
            elapsedTime!!.visibility = LinearLayout.GONE
        }

        playButton = findViewById<View>(R.id.play) as ImageButton
        loopButton = findViewById<View>(R.id.loop) as ImageButton

        loopButton!!.setImageResource(R.drawable.loop_off)

        elapsedTime!!.setOnClickListener { showElapsed = showElapsed xor true }

        seekBar = findViewById<View>(R.id.seek) as SeekBar
        seekBar!!.progress = 0

        seekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
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
                        Log.w(TAG, "Write all sequences preference")
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
        //if (deleteDialog != null) {
        //	deleteDialog.cancel();
        //}

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

    /*
	 * Stop screen updates when screen is off
	 */
    override fun onPause() {
        // Screen is about to turn off
        if (ScreenReceiver.wasScreenOn) {
            screenOn = false
        } //else {
        // Screen state not changed
        //}
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
        //if (deleteDialog != null) {
        //	deleteDialog.cancel();
        //}
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
            val inflater = menuInflater
            inflater.inflate(R.menu.player_menu, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_delete) {
            Message.yesNoDialog(activity!!, "Delete", "Are you sure to delete this file?", Runnable {
                try {
                    if (modPlayer!!.deleteFile()) {
                        Message.toast(activity!!, "File deleted")
                        setResult(Activity.RESULT_FIRST_USER)
                        modPlayer!!.nextSong()
                    } else {
                        Message.toast(activity!!, "Can\'t delete file")
                    }
                } catch (e: RemoteException) {
                    Message.toast(activity!!, "Can\'t connect service")
                }
            })
        }
        return true
    }

    companion object {
        private val TAG = "PlayerActivity"

        val PARM_SHUFFLE = "shuffle"
        val PARM_LOOP = "loop"
        val PARM_START = "start"
        val PARM_KEEPFIRST = "keepFirst"
        private val FRAME_RATE = 25
        private var stopUpdate: Boolean = false                // this MUST be static (volatile doesn't work!)
        private var canChangeViewer: Boolean = false
    }
}
