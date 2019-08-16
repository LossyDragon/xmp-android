package org.helllabs.android.xmp.service

import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.service.notifier.LegacyNotifier
import org.helllabs.android.xmp.service.notifier.LollipopNotifier
import org.helllabs.android.xmp.service.notifier.Notifier
import org.helllabs.android.xmp.service.notifier.OreoNotifier
import org.helllabs.android.xmp.service.utils.QueueManager
import org.helllabs.android.xmp.service.utils.RemoteControl
import org.helllabs.android.xmp.service.utils.Watchdog
import org.helllabs.android.xmp.util.FileUtils
import org.helllabs.android.xmp.util.InfoCache
import org.helllabs.android.xmp.util.Log

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import androidx.preference.PreferenceManager

class PlayerService : Service(), OnAudioFocusChangeListener {

    private var audioManager: AudioManager? = null
    private var remoteControl: RemoteControl? = null
    private var hasAudioFocus: Boolean = false
    private var ducking: Boolean = false
    private var audioInitialized: Boolean = false

    //private MediaSessionCompat session;

    private var playThread: Thread? = null
    private var prefs: SharedPreferences? = null
    private var watchdog: Watchdog? = null
    private var sampleRate: Int = 0
    private var volume: Int = 0
    private var notifier: Notifier? = null
    private var cmd: Int = 0
    private var restart: Boolean = false
    private var canRelease: Boolean = false
    var isPlayerPaused: Boolean = false
        private set
    private var previousPaused: Boolean = false        // save previous pause state
    private var discardBuffer: Boolean = false      // don't play current buffer if changing module while paused
    private var looped: Boolean = false
    private var allPlayerSequences: Boolean = false
    private var startIndex: Int = 0
    private var updateData: Boolean = false
    private var playerFileName: String? = null            // currently playing file
    private var queue: QueueManager? = null
    private val callbacks = RemoteCallbackList<PlayerCallback>()
    private var sequenceNumber: Int = 0

    private var receiverHelper: ReceiverHelper? = null

    private val binder = object : ModInterface.Stub() {
        override fun getModName(): String = Xmp.modName

        override fun getModType(): String = Xmp.modType

        override fun isPaused(): Boolean = isPlayerPaused

        @Throws(RemoteException::class)
        override fun getLoop(): Boolean = looped

        @Throws(RemoteException::class)
        override fun getAllSequences(): Boolean = allPlayerSequences

        override fun getFileName(): String = playerFileName!!

        override fun getInstruments(): Array<String> = Xmp.instruments

        override fun play(fileList: MutableList<String>?, start: Int, shuffle: Boolean, loopList: Boolean, keepFirst: Boolean) {

            if (!audioInitialized || !hasAudioFocus) {
                stopSelf()
                return
            }

            queue = QueueManager(fileList!!, start, shuffle, loopList, keepFirst)
            notifier!!.setQueue(queue!!)
            //notifier.clean();
            cmd = CMD_NONE

            if (isPaused) {
                doPauseAndNotify()
            }

            if (isAlive) {
                Log.i(TAG, "Use existing player thread")
                restart = true
                startIndex = if (keepFirst) 0 else start
                nextSong()
            } else {
                Log.i(TAG, "Start player thread")
                playThread = Thread(PlayRunnable())
                playThread!!.start()
            }
            isAlive = true
        }

        override fun add(fileList: List<String>?) {
            queue!!.add(fileList!!)
            updateNotification()
            //notifier.notification("Added to play queue");
        }

        override fun stop() {
            actionStop()
        }

        override fun pause() {
            doPauseAndNotify()
            receiverHelper!!.isHeadsetPaused = false
        }

        override fun getInfo(values: IntArray?) {
            Xmp.getInfo(values!!)
        }

        override fun seek(seconds: Int) {
            Xmp.seek(seconds)
        }

        override fun time(): Int = Xmp.time()

        override fun getModVars(vars: IntArray?) {
            Xmp.getModVars(vars!!)
        }

        override fun getChannelData(volumes: IntArray?, finalvols: IntArray?, pans: IntArray?, instruments: IntArray?, keys: IntArray?, periods: IntArray?) {
            if (updateData) {
                synchronized(playThread!!) {
                    Xmp.getChannelData(volumes!!, finalvols!!, pans!!, instruments!!, keys!!, periods!!)
                }
            }
        }

        override fun getSampleData(trigger: Boolean, ins: Int, key: Int, period: Int, chn: Int, width: Int, buffer: ByteArray?) {
            if (updateData) {
                synchronized(playThread!!) {
                    Xmp.getSampleData(trigger, ins, key, period, chn, width, buffer!!)
                }
            }
        }

        override fun nextSong() {
            Xmp.stopModule()
            cmd = CMD_NEXT
            if (isPaused) {
                doPauseAndNotify()
            }
            discardBuffer = true
        }

        override fun prevSong() {
            Xmp.stopModule()
            cmd = CMD_PREV
            if (isPaused) {
                doPauseAndNotify()
            }
            discardBuffer = true
        }

        @Throws(RemoteException::class)
        override fun toggleLoop(): Boolean {
            looped = looped xor true
            return looped
        }

        @Throws(RemoteException::class)
        override fun toggleAllSequences(): Boolean {
            allPlayerSequences = allSequences xor true
            return allSequences
        }

        override fun setSequence(seq: Int): Boolean {
            val ret = Xmp.setSequence(seq)
            if (ret) {
                sequenceNumber = seq
                notifyNewSequence()
            }
            return ret
        }

        override fun allowRelease() {
            canRelease = true
        }

        override fun getSeqVars(vars: IntArray) {
            Xmp.getSeqVars(vars)
        }

        override fun getPatternRow(pat: Int, row: Int, rowNotes: ByteArray?, rowInstruments: ByteArray?) {
            if (isAlive) {
                Xmp.getPatternRow(pat, row, rowNotes!!, rowInstruments!!)
            }
        }

        override fun mute(chn: Int, status: Int): Int = Xmp.mute(chn, status)

        override fun hasComment(): Boolean = Xmp.comment.isEmpty()

        // File management
        override fun deleteFile(): Boolean {
            Log.i(TAG, "Delete file " + playerFileName!!)
            return InfoCache.delete(playerFileName!!)
        }

        // Callback
        override fun registerCallback(callback: PlayerCallback?) {
            if (callback != null) {
                callbacks.register(callback)
            }
        }

        override fun unregisterCallback(callback: PlayerCallback?) {
            if (callback != null) {
                callbacks.unregister(callback)
            }
        }
    }


    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "Create service")

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        remoteControl = RemoteControl(this, audioManager!!)

        hasAudioFocus = requestAudioFocus()
        if (!hasAudioFocus) {
            Log.e(TAG, "Can't get audio focus")
        }

        receiverHelper = ReceiverHelper(this)
        receiverHelper!!.registerReceivers()

        var bufferMs = prefs!!.getInt(Preferences.BUFFER_MS, DEFAULT_BUFFER_MS)
        if (bufferMs < MIN_BUFFER_MS) {
            bufferMs = MIN_BUFFER_MS
        } else if (bufferMs > MAX_BUFFER_MS) {
            bufferMs = MAX_BUFFER_MS
        }

        sampleRate = Integer.parseInt(prefs!!.getString(Preferences.SAMPLING_RATE, "44100")!!)

        if (Xmp.init(sampleRate, bufferMs)) {
            audioInitialized = true
        } else {
            Log.e(TAG, "error initializing audio")
        }

        volume = Xmp.volume

        isAlive = false
        isLoaded = false
        isPlayerPaused = false
        allPlayerSequences = prefs!!.getBoolean(Preferences.ALL_SEQUENCES, false)

        //session = new MediaSessionCompat(this, getPackageName());
        //session.setActive(true);

        when {
            Build.VERSION.SDK_INT >= 26 -> notifier = OreoNotifier(this)
            Build.VERSION.SDK_INT >= 21 -> notifier = LollipopNotifier(this)
            else -> notifier = LegacyNotifier(this)
        }

        watchdog = Watchdog(10)
        watchdog!!.setOnTimeoutListener(object : Watchdog.OnTimeoutListener {
            override fun onTimeout() {
                Log.e(TAG, "Stopped by watchdog")
                audioManager!!.abandonAudioFocus(this@PlayerService)
                stopSelf()
            }
        })
        watchdog!!.start()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        receiverHelper!!.unregisterReceivers()

        watchdog!!.stop()
        notifier!!.cancel()

        //session.setActive(false);

        if (audioInitialized) {
            end(if (hasAudioFocus) RESULT_OK else RESULT_NO_AUDIO_FOCUS)
        } else {
            end(RESULT_CANT_OPEN_AUDIO)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    private fun requestAudioFocus(): Boolean {
        return audioManager!!.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun updateNotification() {
        if (queue != null) {    // It seems that queue can be null if we're called from PhoneStateListener
            var name = Xmp.modName
            if (name.isEmpty()) {
                name = FileUtils.basename(queue!!.filename)
            }
            notifier!!.notify(name, Xmp.modType, queue!!.index, if (isPlayerPaused) Notifier.TYPE_PAUSE else 0)
        }
    }

    private fun doPauseAndNotify() {
        isPlayerPaused = isPlayerPaused xor true
        updateNotification()
        if (isPlayerPaused) {
            Xmp.stopAudio()
            remoteControl!!.setStatePaused()
        } else {
            remoteControl!!.setStatePlaying()
            Xmp.restartAudio()
        }
    }

    fun actionStop() {
        Xmp.stopModule()
        if (isPlayerPaused) {
            doPauseAndNotify()
        }
        cmd = CMD_STOP
    }

    fun actionPlayPause() {
        doPauseAndNotify()

        // Notify clients that we paused
        val numClients = callbacks.beginBroadcast()
        for (i in 0 until numClients) {
            try {
                callbacks.getBroadcastItem(i).pauseCallback()
            } catch (e: RemoteException) {
                Log.e(TAG, "Error notifying pause to client")
            }

        }
        callbacks.finishBroadcast()
    }

    fun actionPrev() {
        if (Xmp.time() > 2000) {
            Xmp.seek(0)
        } else {
            Xmp.stopModule()
            cmd = CMD_PREV
        }
        if (isPlayerPaused) {
            doPauseAndNotify()
            discardBuffer = true
        }
    }

    fun actionNext() {
        Xmp.stopModule()
        if (isPlayerPaused) {
            doPauseAndNotify()
            discardBuffer = true
        }
        cmd = CMD_NEXT
    }


    //	private int playFrame() {
    //		// Synchronize frame play with data gathering so we don't change playing variables
    //		// in the middle of e.g. sample data reading, which results in a segfault in C code
    //
    //		synchronized (playThread) {
    //			return Xmp.playBuffer();
    //		}
    //	}

    private fun notifyNewSequence() {
        val numClients = callbacks.beginBroadcast()
        for (j in 0 until numClients) {
            try {
                callbacks.getBroadcastItem(j).newSequenceCallback()
            } catch (e: RemoteException) {
                Log.e(TAG, "Error notifying end of module to client")
            }

        }
        callbacks.finishBroadcast()
    }

    private inner class PlayRunnable : Runnable {
        override fun run() {
            cmd = CMD_NONE

            val vars = IntArray(8)
            remoteControl!!.setStatePlaying()

            var lastRecognized = 0
            do {
                playerFileName = queue!!.filename        // Used in reconnection

                // If this file is unrecognized, and we're going backwards, go to previous
                // If we're at the start of the list, go to the last recognized file
                if (playerFileName == null || !InfoCache.testModule(playerFileName!!)) {
                    Log.w(TAG, playerFileName!! + ": unrecognized format")
                    if (cmd == CMD_PREV) {
                        if (queue!!.index <= 0) {
                            queue!!.index = lastRecognized - 1        // -1 because we have queue.next() in the while condition
                            continue
                        }
                        queue!!.previous()
                    }
                    continue
                }

                // Set default pan before we load the module
                val defpan = prefs!!.getInt(Preferences.DEFAULT_PAN, 50)
                Log.i(TAG, "Set default pan to $defpan")
                Xmp.setPlayer(Xmp.PLAYER_DEFPAN, defpan)

                // Ditto if we can't load the module
                Log.w(TAG, "Load " + playerFileName!!)
                if (Xmp.loadModule(playerFileName!!) < 0) {
                    Log.e(TAG, "Error loading " + playerFileName!!)
                    if (cmd == CMD_PREV) {
                        if (queue!!.index <= 0) {
                            queue!!.index = lastRecognized - 1
                            continue
                        }
                        queue!!.previous()
                    }
                    continue
                }

                lastRecognized = queue!!.index

                cmd = CMD_NONE

                var name = Xmp.modName
                if (name.isEmpty()) {
                    name = FileUtils.basename(playerFileName)
                }
                notifier!!.notify(name, Xmp.modType, queue!!.index, Notifier.TYPE_TICKER)
                isLoaded = true

                // Unmute all channels
                for (i in 0..63) {
                    Xmp.mute(i, 0)
                }

                val volBoost = prefs!!.getString(Preferences.VOL_BOOST, "1")

                val interpTypes = intArrayOf(Xmp.INTERP_NEAREST, Xmp.INTERP_LINEAR, Xmp.INTERP_SPLINE)
                val temp = Integer.parseInt(prefs!!.getString(Preferences.INTERP_TYPE, "1")!!)
                var interpType: Int
                interpType = if (temp in 1..2) {
                    interpTypes[temp]
                } else {
                    Xmp.INTERP_LINEAR
                }

                if (!prefs!!.getBoolean(Preferences.INTERPOLATE, true)) {
                    interpType = Xmp.INTERP_NEAREST
                }

                Xmp.startPlayer(sampleRate)

                synchronized(audioManager!!) {
                    if (ducking) {
                        Xmp.setPlayer(Xmp.PLAYER_VOLUME, DUCK_VOLUME)
                    }
                }

                var numClients = callbacks.beginBroadcast()
                for (j in 0 until numClients) {
                    try {
                        callbacks.getBroadcastItem(j).newModCallback()
                    } catch (e: RemoteException) {
                        Log.e(TAG, "Error notifying new module to client")
                    }

                }
                callbacks.finishBroadcast()

                Xmp.setPlayer(Xmp.PLAYER_AMP, Integer.parseInt(volBoost!!))
                Xmp.setPlayer(Xmp.PLAYER_MIX, prefs!!.getInt(Preferences.STEREO_MIX, 100))
                Xmp.setPlayer(Xmp.PLAYER_INTERP, interpType)
                Xmp.setPlayer(Xmp.PLAYER_DSP, Xmp.DSP_LOWPASS)

                var flags = Xmp.getPlayer(Xmp.PLAYER_CFLAGS)
                flags = if (prefs!!.getBoolean(Preferences.AMIGA_MIXER, false)) {
                    flags or Xmp.FLAGS_A500
                } else {
                    flags and Xmp.FLAGS_A500.inv()
                }
                Xmp.setPlayer(Xmp.PLAYER_CFLAGS, flags)

                updateData = true

                sequenceNumber = 0
                var playNewSequence: Boolean
                Xmp.setSequence(sequenceNumber)

                Xmp.playAudio()

                Log.i(TAG, "Enter play loop")

                do {
                    Xmp.getModVars(vars)
                    remoteControl!!.setMetadata(Xmp.modName, Xmp.modType, vars[0].toLong())

                    while (cmd == CMD_NONE) {
                        discardBuffer = false

                        // Wait if paused
                        while (isPlayerPaused) {
                            try {
                                Thread.sleep(100)
                            } catch (e: InterruptedException) {
                                break
                            }

                            watchdog!!.refresh()
                            receiverHelper!!.checkReceivers()
                        }

                        if (discardBuffer) {
                            Log.d(TAG, "discard buffer")
                            Xmp.dropAudio()
                            break
                        }

                        // Wait if no buffers available
                        while (!Xmp.hasFreeBuffer() && !isPlayerPaused && cmd == CMD_NONE) {
                            try {
                                Thread.sleep(40)
                            } catch (e: InterruptedException) {
                            }

                        }

                        // Fill a new buffer
                        if (Xmp.fillBuffer(looped) < 0) {
                            break
                        }

                        watchdog!!.refresh()
                        receiverHelper!!.checkReceivers()
                    }

                    // Subsong explorer
                    // Do all this if we've exited normally and explorer is active
                    playNewSequence = false
                    if (allPlayerSequences && cmd == CMD_NONE) {
                        sequenceNumber++

                        Log.w(TAG, "Play sequence $sequenceNumber")
                        if (Xmp.setSequence(sequenceNumber)) {
                            playNewSequence = true
                            notifyNewSequence()
                        }
                    }
                } while (playNewSequence)

                Xmp.endPlayer()

                isLoaded = false

                // notify end of module to our clients
                numClients = callbacks.beginBroadcast()
                if (numClients > 0) {
                    canRelease = false

                    for (j in 0 until numClients) {
                        try {
                            Log.w(TAG, "Call end of module callback")
                            callbacks.getBroadcastItem(j).endModCallback()
                        } catch (e: RemoteException) {
                            Log.e(TAG, "Error notifying end of module to client")
                        }

                    }
                    callbacks.finishBroadcast()

                    // if we have clients, make sure we can release module
                    var timeout = 0
                    try {
                        while (!canRelease && timeout < 20) {
                            Thread.sleep(100)
                            timeout++
                        }
                    } catch (e: InterruptedException) {
                        Log.e(TAG, "Sleep interrupted: $e")
                    }

                } else {
                    callbacks.finishBroadcast()
                }

                Log.w(TAG, "Release module")
                Xmp.releaseModule()

                //audio.stop();

                // Used when current files are replaced by a new set
                if (restart) {
                    Log.i(TAG, "Restart")
                    queue!!.index = startIndex - 1
                    cmd = CMD_NONE
                    restart = false
                } else if (cmd == CMD_PREV) {
                    queue!!.previous()
                    //returnToPrev = false;
                }
            } while (cmd != CMD_STOP && queue!!.next())

            synchronized(playThread!!) {
                updateData = false        // stop getChannelData update
            }
            watchdog!!.stop()
            notifier!!.cancel()

            remoteControl!!.setStateStopped()
            audioManager!!.abandonAudioFocus(this@PlayerService)

            //end();
            Log.i(TAG, "Stop service")
            stopSelf()
        }
    }

    private fun end(result: Int) {
        Log.i(TAG, "End service")
        val numClients = callbacks.beginBroadcast()
        for (i in 0 until numClients) {
            try {
                callbacks.getBroadcastItem(i).endPlayCallback(result)
            } catch (e: RemoteException) {
                Log.e(TAG, "Error notifying end of play to client")
            }

        }
        callbacks.finishBroadcast()

        isAlive = false
        Xmp.stopModule()
        if (isPlayerPaused) {
            doPauseAndNotify()
        }

        Xmp.deinit()
        //audio.release();
    }


    // for audio focus loss

    private fun autoPause(pause: Boolean): Boolean {
        Log.i(TAG, "Auto pause changed to " + pause + ", previously " + receiverHelper!!.isAutoPaused)
        if (pause) {
            previousPaused = isPlayerPaused
            receiverHelper!!.isAutoPaused = true
            isPlayerPaused = false                // set to complement, flip on doPause()
            doPauseAndNotify()
        } else {
            if (receiverHelper!!.isAutoPaused && !receiverHelper!!.isHeadsetPaused) {
                receiverHelper!!.isAutoPaused = false
                isPlayerPaused = !previousPaused    // set to complement, flip on doPause()
                doPauseAndNotify()
            }
        }

        return receiverHelper!!.isAutoPaused
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.w(TAG, "AUDIOFOCUS_LOSS_TRANSIENT")
                // Pause playback
                autoPause(true)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.w(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK")
                // Lower volume
                synchronized(audioManager!!) {
                    volume = Xmp.volume
                    Xmp.setVolume(DUCK_VOLUME)
                    ducking = true
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.w(TAG, "AUDIOFOCUS_GAIN")
                // Resume playback/raise volume
                autoPause(false)
                synchronized(audioManager!!) {
                    Xmp.setVolume(volume)
                    ducking = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.w(TAG, "AUDIOFOCUS_LOSS")
                // Stop playback
                actionStop()
            }
            else -> {
            }
        }
    }

    companion object {
        private const val TAG = "PlayerService"

        const val RESULT_OK = 0
        const val RESULT_CANT_OPEN_AUDIO = 1
        const val RESULT_NO_AUDIO_FOCUS = 2

        private const val CMD_NONE = 0
        private const val CMD_NEXT = 1
        private const val CMD_PREV = 2
        private const val CMD_STOP = 3

        private const val MIN_BUFFER_MS = 80
        private const val MAX_BUFFER_MS = 1000
        private const val DEFAULT_BUFFER_MS = 400

        private const val DUCK_VOLUME = 0x500

        var isAlive: Boolean = false
        var isLoaded: Boolean = false
    }
}
