package org.helllabs.android.xmp.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MEDIA_BUTTON
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.AudioManager.*
import android.os.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import android.view.KeyEvent.*
import androidx.media.session.MediaButtonReceiver
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.service.notifier.LegacyNotifier
import org.helllabs.android.xmp.service.notifier.ModernNotifier
import org.helllabs.android.xmp.service.notifier.Notifier
import org.helllabs.android.xmp.service.receiver.NoisyReceiver
import org.helllabs.android.xmp.service.utils.OreoAudioFocusHandler
import org.helllabs.android.xmp.service.utils.QueueManager
import org.helllabs.android.xmp.service.utils.Watchdog
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.FileUtils

class PlayerService : Service(), OnAudioFocusChangeListener {

    /* Media Stuff */
    private var mediaSession: MediaSessionCompat? = null
    private var audioManager: AudioManager? = null

    private var oreoFocusHandler: OreoAudioFocusHandler? = null
    private var audioInitialized = false
    private var isDucking = false
    private var volume: Int = 0
    var isPlayerPaused: Boolean = false
        private set

    /* Headset Hook stuff */
    private var hookCount = 0
    private val hookHandler = Handler()
    private val hookRunnable = Runnable {
        when (hookCount) {
            1 -> onPlayPause()
            2 -> onNext()
            else -> onPrevious()
        }
        hookCount = 0
    }

    /* XMP Stuff */
    private val callbacks = RemoteCallbackList<PlayerCallback>()
    private var allPlayerSequences: Boolean = false
    private var canRelease: Boolean = false
    private var cmd: Int = 0
    // don't play current buffer if changing module while paused
    private var discardBuffer: Boolean = false
    private var looped: Boolean = false
    private var playerFileName: String? = null // currently playing file
    private var playThread: Thread? = null
    private var queue: QueueManager? = null
    private var restart: Boolean = false
    private var sampleRate: Int = 0
    private var sequenceNumber: Int = 0
    private var startIndex: Int = 0
    private var updateData: Boolean = false
    private var watchdog: Watchdog = Watchdog(10)

    /* Other Stuff */
    private var prefs: SharedPreferences? = null
    private lateinit var notifier: Notifier

    private var noisyReceiver = NoisyReceiver()

    private val binder = object : ModInterface.Stub() {
        override fun currentPlayTime(time: Int) {
            /*Nothing for now*/
        }

        override fun getModName(): String = Xmp.getModName()

        override fun getModType(): String = Xmp.getModType()

        override fun isPaused(): Boolean = isPlayerPaused

        @Throws(RemoteException::class)
        override fun getLoop(): Boolean = looped

        @Throws(RemoteException::class)
        override fun getAllSequences(): Boolean = allPlayerSequences

        override fun getFileName(): String = playerFileName!!

        override fun getInstruments(): Array<String> = Xmp.getInstruments()

        override fun play(
                fileList: MutableList<String>?,
                start: Int,
                shuffle: Boolean,
                loopList: Boolean,
                keepFirst: Boolean
        ) {
            if (!audioInitialized) {
                stopSelf()
                return
            }

            queue = QueueManager(fileList!!, start, shuffle, loopList, keepFirst)
            notifier.setQueue(queue!!)

            cmd = CMD_NONE

            if (isPlayerPaused)
                onPlayPause()

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
        }

        override fun stop() = onStop()

        override fun pause() = onPlayPause()

        override fun getInfo(values: IntArray?) = Xmp.getInfo(values!!)

        override fun seek(seconds: Int) {
            Xmp.seek(seconds)
        }

        override fun time(): Int = Xmp.time()

        override fun getModVars(vars: IntArray?) = Xmp.getModVars(vars!!)

        override fun getChannelData(
                volumes: IntArray?,
                finalvols: IntArray?,
                pans: IntArray?,
                instruments: IntArray?,
                keys: IntArray?,
                periods: IntArray?
        ) {
            if (updateData) {
                synchronized(playThread!!) {
                    Xmp.getChannelData(
                            volumes!!, finalvols!!, pans!!, instruments!!, keys!!, periods!!)
                }
            }
        }

        override fun getSampleData(
                trigger: Boolean,
                ins: Int,
                key: Int,
                period: Int,
                chn: Int,
                width: Int,
                buffer: ByteArray?
        ) {
            if (updateData) {
                synchronized(playThread!!) {
                    Xmp.getSampleData(trigger, ins, key, period, chn, width, buffer!!)
                }
            }
        }

        override fun nextSong() = onNext()

        override fun prevSong() = onPrevious()

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

        override fun getSeqVars(vars: IntArray) = Xmp.getSeqVars(vars)

        override fun getPatternRow(
                pat: Int,
                row: Int,
                rowNotes: ByteArray?,
                rowInstruments: ByteArray?
        ) {
            if (isAlive) Xmp.getPatternRow(pat, row, rowNotes!!, rowInstruments!!)
        }

        override fun mute(chn: Int, status: Int): Int = Xmp.mute(chn, status)

        override fun hasComment(): Boolean = Xmp.getComment().isEmpty()

        // File management
        override fun deleteFile(): Boolean {
            Log.i(TAG, "Delete file " + playerFileName!!)
            return InfoCache.delete(playerFileName!!)
        }

        // Callback
        override fun registerCallback(callback: PlayerCallback?) {
            if (callback != null) callbacks.register(callback)
        }

        override fun unregisterCallback(callback: PlayerCallback?) {
            if (callback != null) callbacks.unregister(callback)
        }
    }

    private inner class PlayRunnable : Runnable {
        override fun run() {
            cmd = CMD_NONE

            val vars = IntArray(8)

            var lastRecognized = 0
            do {
                // Used in reconnection
                playerFileName = queue!!.filename

                // If this file is unrecognized, and we're going backwards, go to previous
                // If we're at the start of the list, go to the last recognized file
                if (playerFileName == null || !InfoCache.testModule(playerFileName!!)) {
                    Log.w(TAG, playerFileName!! + ": unrecognized format")
                    if (cmd == CMD_PREV) {
                        if (queue!!.index <= 0) {
                            // -1 because we have queue.next() in the while condition
                            queue!!.index = lastRecognized - 1
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
                Log.i(TAG, "Load " + playerFileName!!)
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

                var name = Xmp.getModName()
                if (name.isEmpty()) {
                    name = FileUtils.basename(playerFileName)
                }
                notifier.notify(
                        name,
                        Xmp.getModType(),
                        queue!!.index,
                        Notifier.TYPE_TICKER
                )

                isLoaded = true

                // Unmute all channels
                for (i in 0..63) {
                    Xmp.mute(i, 0)
                }

                val volBoost = prefs!!.getString(Preferences.VOL_BOOST, "1")

                val interpTypes =
                        intArrayOf(Xmp.INTERP_NEAREST, Xmp.INTERP_LINEAR, Xmp.INTERP_SPLINE)
                val temp =
                        Integer.parseInt(prefs!!.getString(Preferences.INTERP_TYPE, "1")!!)
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

                    // MetaData necessary for mod trackers? - Yeah, for android wear.
                    val metaData = MediaMetadataCompat.Builder().apply {
                        putString(MediaMetadataCompat.METADATA_KEY_TITLE, Xmp.getModName())
                        putString(MediaMetadataCompat.METADATA_KEY_ARTIST, Xmp.getModType())
                        putLong(MediaMetadataCompat.METADATA_KEY_DURATION, vars[0].toLong())
                    }.build()
                    mediaSession?.setMetadata(metaData)

                    while (cmd == CMD_NONE) {
                        discardBuffer = false

                        // Wait if paused
                        while (isPlayerPaused) {
                            try {
                                Thread.sleep(100)
                            } catch (e: InterruptedException) {
                                break
                            }

                            watchdog.refresh()
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

                        watchdog.refresh()
                    }

                    // Subsong explorer
                    // Do all this if we've exited normally and explorer is active
                    playNewSequence = false
                    if (allPlayerSequences && cmd == CMD_NONE) {
                        sequenceNumber++

                        Log.i(TAG, "Play sequence $sequenceNumber")
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
                            Log.i(TAG, "Call end of module callback")
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

                Log.i(TAG, "Release module")
                Xmp.releaseModule()

                // Used when current files are replaced by a new set
                if (restart) {
                    Log.i(TAG, "Restart")
                    queue!!.index = startIndex - 1
                    cmd = CMD_NONE
                    restart = false
                } else if (cmd == CMD_PREV) {
                    queue!!.previous()
                }
            } while (cmd != CMD_STOP && queue!!.next())

            // Stop getChannelData update
            synchronized(playThread!!) {
                updateData = false
            }

            onServiceKill()
        }
    }

    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "Create service")

        // Init Prefs
        prefs = XmpApplication.instance!!.sharedPrefs
        sampleRate = prefs!!.getString(Preferences.SAMPLING_RATE, "44100")!!.toInt()

        // Init Media Session
        mediaSession = MediaSessionCompat(this, TAG)
        mediaSession!!.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
                mediaSessionButtons(mediaButtonEvent)
                return super.onMediaButtonEvent(mediaButtonEvent)
            }
        })
        mediaSession!!.isActive = true

        // Init Headphone pull receiver
        registerNoisyReceiver()

        // Init AudioManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (isAtLeastO())
            oreoFocusHandler = OreoAudioFocusHandler(applicationContext) // Kinda need this >.<

        var bufferMs = prefs!!.getInt(Preferences.BUFFER_MS, DEFAULT_BUFFER_MS)
        when {
            bufferMs < MIN_BUFFER_MS -> bufferMs = MIN_BUFFER_MS
            bufferMs > MAX_BUFFER_MS -> bufferMs = MAX_BUFFER_MS
        }

        when (Xmp.init(sampleRate, bufferMs)) {
            true -> audioInitialized = true
            false -> Log.e(TAG, "error initializing audio")
        }

        volume = Xmp.getVolume()
        isAlive = false
        isLoaded = false
        isPlayerPaused = false
        allPlayerSequences = prefs!!.getBoolean(Preferences.ALL_SEQUENCES, false)

        notifier = when (Build.VERSION.SDK_INT >= 21) {
            true -> ModernNotifier(this)
            false -> LegacyNotifier(this)
        }

        watchdog.apply {
            setOnTimeoutListener(object : Watchdog.OnTimeoutListener {
                override fun onTimeout() {
                    Log.w(TAG, "Stopped by watchdog")
                    stopSelf()
                }
            })
            start()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        when (intent.action) {
            XMP_PLAYER_NEXT -> onNext()
            XMP_PLAYER_PREV -> onPrevious()
            XMP_PLAYER_STOP -> onStop()
            XMP_PLAYER_PLAY -> onPlayPause()
            XMP_PLAYER_PAUSE -> onPlayPause()
            XMP_PLAYER_HOOK -> onPlayPause()
        }

        MediaButtonReceiver.handleIntent(mediaSession, intent)

        requestAudioFocus()

        return START_NOT_STICKY
    }

    override fun onDestroy() {

        onServiceKill()

        if (audioInitialized)
            end(RESULT_OK)
        else
            end(RESULT_CANT_OPEN_AUDIO)

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    private fun requestAudioFocus() {
        if (isAtLeastO()) {
            oreoFocusHandler?.requestAudioFocus(this)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(this, STREAM_MUSIC, AUDIOFOCUS_GAIN)
        }
    }

    @Suppress("DEPRECATION")
    private fun abandonAudioFocus() {
        if (isAtLeastO()) {
            oreoFocusHandler?.abandonAudioFocus()
        } else {
            audioManager?.abandonAudioFocus(this)
        }
    }

    private fun onServiceKill() {
        Log.i(TAG, "Stop service")

        mediaSession?.release()

        watchdog.stop()
        playThread?.interrupt()

        abandonAudioFocus()
        unregisterReceiver(noisyReceiver)

        stopForeground(true)
        stopSelf()
    }

    private fun updateNotification() {
        // It seems that queue can be null if we're called from PhoneStateListener
        if (queue != null) {
            var name = Xmp.getModName()

            if (name.isEmpty())
                name = FileUtils.basename(queue!!.filename)

            notifier.notify(
                    name,
                    Xmp.getModType(),
                    queue!!.index,
                    if (isPlayerPaused) Notifier.TYPE_PAUSE else 0
            )
        }
    }

    private fun onPlayPause() {
        isPlayerPaused = isPlayerPaused xor true
        updateNotification()
        notifyPause()

        if (isPlayerPaused) {
            Xmp.stopAudio()

            // Unregister NoisyReceiver on pause
            unregisterReceiver(noisyReceiver)
        } else {
            requestAudioFocus()
            Xmp.restartAudio()

            // Register NoisyReceiver on play
            registerNoisyReceiver()
        }
    }

    private fun onStop() {
        Xmp.stopModule()
        abandonAudioFocus()
        onServiceKill()
        mediaSession?.isActive = false
        cmd = CMD_STOP
    }

    private fun onPrevious() {
        when {
            Xmp.time() > 2000 -> Xmp.seek(0)
            else -> {
                Xmp.stopModule()
                cmd = CMD_PREV
            }
        }
        if (isPlayerPaused) {
            discardBuffer = true
            onPlayPause()
        }
    }

    private fun onNext() {
        Xmp.stopModule()
        cmd = CMD_NEXT
        if (isPlayerPaused) {
            discardBuffer = true
            onPlayPause()
        }
    }

    // Notify clients that we paused
    private fun notifyPause() {
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

    // Notify clients that have a new sequence
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
        Xmp.deinit()

        onServiceKill()
    }

    private fun registerNoisyReceiver() {
        val filter = IntentFilter().apply {
            addAction(if (isAtLeastL()) ACTION_HEADSET_PLUG else Intent.ACTION_HEADSET_PLUG)
            addAction(ACTION_AUDIO_BECOMING_NOISY)
        }
        registerReceiver(noisyReceiver, filter)
    }

    private fun onFocusGain() {
        when (isDucking) {
            true -> onFocusUnDuck()
            false -> onPlayPause()
        }
    }

    private fun onFocusLost() {
        onPlayPause()
    }

    private fun onFocusDuck() {
        isDucking = true
        volume = Xmp.getVolume()
        Xmp.setVolume(DUCK_VOLUME)
    }

    private fun onFocusUnDuck() {
        isDucking = false
        Xmp.setVolume(volume)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AUDIOFOCUS_GAIN -> onFocusGain() // Gain
            AUDIOFOCUS_LOSS, AUDIOFOCUS_LOSS_TRANSIENT -> onFocusLost() // Loss
            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onFocusDuck() // Duck
        }
    }

    // So... new API's use MediaSession instead of a Broadcast receiver.
    private fun mediaSessionButtons(mediaButtonEvent: Intent) {
        if (mediaButtonEvent.action == ACTION_MEDIA_BUTTON) {
            val event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as KeyEvent
            if (event.action == ACTION_UP) {
                when (event.keyCode) {
                    KEYCODE_MEDIA_PLAY_PAUSE,
                    KEYCODE_MEDIA_PLAY,
                    KEYCODE_MEDIA_PAUSE -> onPlayPause()
                    KEYCODE_MEDIA_NEXT -> onNext()
                    KEYCODE_MEDIA_PREVIOUS -> onPrevious()
                    KEYCODE_HEADSETHOOK -> {
                        hookCount++
                        hookHandler.removeCallbacks(hookRunnable)
                        if (hookCount >= 3)
                            hookHandler.post(hookRunnable)
                        else
                            hookHandler.postDelayed(hookRunnable, 500)
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = PlayerService::class.java.simpleName

        const val XMP_PLAYER_NEXT = "XMP_NEXT"
        const val XMP_PLAYER_PREV = "XMP_PREV"
        const val XMP_PLAYER_STOP = "XMP_STOP"
        const val XMP_PLAYER_PLAY = "XMP_PLAY"
        const val XMP_PLAYER_PAUSE = "XMP_PAUSE"
        const val XMP_PLAYER_HOOK = "XMP_HOOK"

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
