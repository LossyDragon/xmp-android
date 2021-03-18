package org.helllabs.android.xmp.service

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
import android.support.v4.media.session.PlaybackStateCompat.*
import android.view.KeyEvent
import androidx.lifecycle.MutableLiveData
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.service.Notifier.Companion.TYPE_PAUSE
import org.helllabs.android.xmp.service.Notifier.Companion.TYPE_TICKER
import org.helllabs.android.xmp.service.receiver.ControllerReceiver
import org.helllabs.android.xmp.service.receiver.NoisyReceiver
import org.helllabs.android.xmp.service.utils.*
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.FileUtils.basename
import org.helllabs.android.xmp.util.InfoCache.delete
import org.helllabs.android.xmp.util.InfoCache.testModule

// Binder is leaking via leak canary  ¯\_(ツ)_/¯
// -- I guess it retains it for a very long time? (SO answers)

@AndroidEntryPoint
class PlayerService : Service(), OnAudioFocusChangeListener, Watchdog.OnTimeoutListener {

    /* Binder Stuff */
    private var binder: Binder? = PlayerBinder()

    inner class PlayerBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    @Inject
    lateinit var eventBus: EventBus

    private var audioFocusRequest: AudioFocusRequestCompat? = null
    private var audioManager: AudioManager? = null
    private var controllerReceiver: ControllerReceiver? = null
    private var currentFileName: String? = null
    private var mediaSession: MediaSessionCompat? = null
    private var noisyReceiver: NoisyReceiver? = null
    private var notifier: Notifier? = null
    private var playJob: Job? = null
    private var queue: QueueManager? = null
    private var watchdog: Watchdog? = null

    private var audioInitialized = false
    private var canRelease = false
    private var canResumePlay = false
    private var cmd = 0
    private var discardBuffer = false // don't play current buffer if changing module while paused
    private var ducking = false
    private var hasAudioFocus = false
    private var isPlayerPaused = false
    private var looped = false
    private var playerAllSequences = false
    private var restart = false
    private var sampleRate = 0
    private var sequenceNumber = 0
    private var startIndex = 0
    private var updateData = false
    private var volume = 0

    private var sessionCallback: MediaSessionCompat.Callback? =
        object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean =
                onMediaButton(mediaButtonEvent)

            override fun onPlay() {
                if (!hasAudioFocus) {
                    val focus = requestAudioFocus()
                    if (focus) {
                        hasAudioFocus = focus
                    } else {
                        logW("Failed to get audio focus on play!")
                    }
                }

                registerReceiver(
                    noisyReceiver,
                    IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                )
                isPlayerPaused = false

                // Do not play again if we're calling for stop.
                if (cmd != CMD_STOP) {
                    Xmp.restartAudio()
                }

                notifyPlayPause()
            }

            override fun onPause() {
                try {
                    unregisterReceiver(noisyReceiver)
                } catch (e: Exception) {
                    logW("NoisyReciever: ${e.message}")
                }

                Xmp.stopAudio()

                isPlayerPaused = true
                notifyPlayPause()
            }

            override fun onSkipToNext() {
                Xmp.stopModule()
                Xmp.dropAudio()
                discardBuffer = true
                cmd = CMD_NEXT

                if (isPlayerPaused) {
                    mediaSession!!.controller.transportControls.play()
                }

                updateNotification()
            }

            override fun onSkipToPrevious() {
                if (Xmp.time() > 2000) {
                    Xmp.seek(0)
                } else {
                    Xmp.stopModule()
                    Xmp.dropAudio()
                    discardBuffer = true
                    cmd = CMD_PREV
                }

                if (isPlayerPaused) {
                    mediaSession!!.controller.transportControls.play()
                }

                updateNotification()
            }

            override fun onFastForward() {
                mediaSession!!.controller.transportControls.skipToNext()
            }

            override fun onRewind() {
                mediaSession!!.controller.transportControls.skipToPrevious()
            }

            override fun onStop() {
                Xmp.stopModule()
                cmd = CMD_STOP
            }

            override fun onSeekTo(pos: Long) {
                Xmp.seek(pos.toInt())
                updateNotification()
            }
        }

    override fun onBind(intent: Intent): IBinder = binder!!

    override fun onCreate() {
        super.onCreate()
        logI("Create service")
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        hasAudioFocus = requestAudioFocus()
        if (!hasAudioFocus) {
            logE("Can't get audio focus")
        }

        var bufferMs = PrefManager.bufferMs
        if (bufferMs < MIN_BUFFER_MS) {
            bufferMs = MIN_BUFFER_MS
        } else if (bufferMs > MAX_BUFFER_MS) {
            bufferMs = MAX_BUFFER_MS
        }

        sampleRate = PrefManager.samplingRate.toInt()

        if (Xmp.init(sampleRate, bufferMs)) {
            audioInitialized = true
        } else {
            logE("error initializing audio")
        }

        volume = Xmp.getVolume()
        isPlayerAlive.postValue(false)
        isLoaded = false
        isPlayerPaused = false
        playerAllSequences = PrefManager.allSequences

        mediaSession = MediaSessionCompat(this, "PlayerService")
        mediaSession!!.setCallback(sessionCallback)
        @Suppress("DEPRECATION") // Needed anymore?
        mediaSession!!.setFlags(
            FLAG_HANDLES_TRANSPORT_CONTROLS and FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        notifier = Notifier(this, mediaSession!!)

        watchdog = Watchdog(10)
        watchdog!!.listener = this
        watchdog!!.start()

        controllerReceiver = ControllerReceiver(mediaSession!!)
        registerReceiver(
            controllerReceiver,
            IntentFilter().apply {
                addAction(Notifier.ACTION_STOP)
                addAction(Notifier.ACTION_PLAY)
                addAction(Notifier.ACTION_PAUSE)
                addAction(Notifier.ACTION_PREV)
                addAction(Notifier.ACTION_NEXT)
            }
        )

        noisyReceiver = NoisyReceiver(mediaSession!!)
        registerReceiver(
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(controllerReceiver)
            unregisterReceiver(noisyReceiver)
        } catch (e: Exception) {
            logW("Failed to unregister a receiver: ${e.message}")
        }

        watchdog!!.stop()
        notifier!!.cancel()

        mediaSession!!.isActive = false
        mediaSession!!.setCallback(null)
        mediaSession!!.release()

        if (audioInitialized) {
            end(if (hasAudioFocus) RESULT_OK else RESULT_NO_AUDIO_FOCUS)
        } else {
            end(RESULT_CANT_OPEN_AUDIO)
        }

        isPlayerAlive.postValue(false)

        watchdog!!.listener = null

        playJob!!.cancel()

        logI("Service destroyed")
        // Null everything out, reduces Binder leak from ~660kb to ~<10kb
        audioFocusRequest = null
        audioManager = null
        controllerReceiver = null
        currentFileName = null
        mediaSession = null
        noisyReceiver = null
        notifier = null
        playJob = null
        queue = null
        watchdog = null
        sessionCallback = null
        binder = null

        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaSession!!.controller.transportControls.stop()
    }

    override fun onTimeout() {
        end(RESULT_WATCHDOG)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        logI("Audio Focus: $focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                logD("AUDIOFOCUS_GAIN")
                // We have full audio focus
                if (hasAudioFocus && canResumePlay) {
                    mediaSession!!.controller.transportControls.play()
                    canResumePlay = false
                }
                Xmp.setVolume(volume)
                ducking = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                logD("AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK")
                // We're ducking audio for a short amount of time.
                volume = Xmp.getVolume()
                Xmp.setVolume(DUCK_VOLUME)
                ducking = true
            }
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                logD("AUDIOFOCUS_LOSS_TRANSIENT or EXCLUSIVE")
                // We lost audio focus for an known amount of time, pause.
                if (!isPlayerPaused)
                    canResumePlay = true

                mediaSession!!.controller.transportControls.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                logD("AUDIOFOCUS_LOSS")
                // We lost audio focus for an unknown amount of time, pause.
                mediaSession!!.controller.transportControls.pause()
                hasAudioFocus = false
                canResumePlay = false
            }
        }
    }

    fun play(
        fileList: List<String>,
        start: Int,
        shuffle: Boolean,
        loopList: Boolean,
        keepFirst: Boolean
    ) {
        if (!audioInitialized) {
            end(RESULT_CANT_OPEN_AUDIO)
            stopSelf()
            return
        }
        if (!hasAudioFocus) {
            end(RESULT_CANT_OPEN_AUDIO)
            stopSelf()
            return
        }

        queue = QueueManager(fileList, start, shuffle, loopList, keepFirst)
        notifier!!.setQueue(queue)
        cmd = CMD_NONE

        if (isPlayerAlive.value == true) {
            logI("Use existing player thread")
            restart = true
            startIndex = if (keepFirst) 0 else start
            mediaSession!!.controller.transportControls.skipToNext()
        } else {
            logI("Start player thread")
            playJob = playerJob()
        }

        isPlayerAlive.postValue(true)
    }

    fun add(fileList: List<String>) {
        queue!!.add(fileList)
        updateNotification()
    }

    fun toggleLoop(): Boolean {
        looped = !looped
        return looped
    }

    fun toggleAllSequences(): Boolean {
        playerAllSequences = playerAllSequences xor true
        return playerAllSequences
    }

    fun getLoop(): Boolean = looped

    fun getAllSequences(): Boolean = playerAllSequences

    fun isPaused(): Boolean = isPlayerPaused

    fun setSequence(seq: Int): Boolean {
        val ret = Xmp.setSequence(seq)
        if (ret) {
            sequenceNumber = seq
            notifyNewSequence()
        }
        return ret
    }

    fun allowRelease() {
        canRelease = true
    }

    // File management
    fun deleteFile(): Boolean {
        logI("Delete file $currentFileName")
        return delete(currentFileName!!)
    }

    // Get the Modules name, or the filename if empty or untitled
    fun getModName(): String {
        var name = Xmp.getModName()
        if (name.trim { it <= ' ' }.isEmpty() || name == "untitled") {
            name = basename(currentFileName!!)
        }
        return name
    }

    fun getUpdateData(): Boolean = updateData

    fun getMediaSession(): MediaSessionCompat = mediaSession!!

    private fun notifyPlayPause() {
        // Notify clients that we paused or played
        eventBus.post(PlayStateCallback())
        updateNotification()
    }

    private fun updateNotification() {
        CoroutineScope(Dispatchers.Main).launch {
            val status = if (isPlayerPaused) TYPE_PAUSE else TYPE_TICKER
            val playbackState = if (isPlayerPaused) STATE_PAUSED else STATE_PLAYING

            val stateBuilder = Builder()
                .setState(playbackState, Xmp.time().toLong(), 1F)
                .setActions(
                    ACTION_PLAY and ACTION_PLAY_PAUSE and
                        ACTION_PAUSE and ACTION_STOP and
                        ACTION_SKIP_TO_PREVIOUS and ACTION_SKIP_TO_NEXT and
                        ACTION_REWIND and ACTION_FAST_FORWARD
                )

            if (!isPlayerPaused)
                stateBuilder.setActions(ACTION_SEEK_TO)

            delay(SYNC_DELAY)
            mediaSession?.setPlaybackState(stateBuilder.build())

            notifier!!.notify(getModName(), Xmp.getModType(), queue!!.index, status)
        }
    }

    private fun notifyNewSequence() {
        eventBus.post(NewSequenceCallback())
        updateNotification()
    }

    private fun end(result: Int) {
        logI("End service with result: $result")
        mediaSession!!.controller.transportControls.stop()
        eventBus.post(EndPlayCallback(result))

        // Xmp.stopModule()

        stopSelf()
    }

    // https://android-developers.googleblog.com/2020/08/playing-nicely-with-media-controls.html
    private fun setMetaData(name: String, modType: String, time: Long) {
        val bitmap = getIconBitmap()
        val metaData = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, name)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, modType)
            putLong(MediaMetadataCompat.METADATA_KEY_DURATION, time)
            putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap)
            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
        }.build()

        mediaSession!!.setMetadata(metaData)
    }

    // Media Button Event Handler
    private fun onMediaButton(intent: Intent?): Boolean {
        intent?.let {
            val keyEvent = it.getParcelableExtra<Parcelable>(Intent.EXTRA_KEY_EVENT) as KeyEvent
            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                logI("Key Even Action received, keycode: ${keyEvent.keyCode}")
                with(mediaSession!!.controller.transportControls) {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY -> play()
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> pause()
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> skipToPrevious()
                        KeyEvent.KEYCODE_MEDIA_NEXT -> skipToNext()
                        KeyEvent.KEYCODE_MEDIA_STOP -> stop()
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> if (isPlayerPaused) play() else pause()
                    }
                    return true
                }
            }
        }
        logW("Unable to process onMediaButton(). $intent")
        return false
    }

    //region [REGION] Audio Focus Request
    private fun requestAudioFocus(): Boolean {
        audioFocusRequest =
            AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(
                    AudioAttributesCompat.Builder().run {
                        setUsage(AudioAttributesCompat.USAGE_MEDIA)
                        setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                        build()
                    }
                )
                setOnAudioFocusChangeListener(this@PlayerService)
                build()
            }
        val result: Int = AudioManagerCompat.requestAudioFocus(audioManager!!, audioFocusRequest!!)

        logD("Audio Focus was $result")
        return result == AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (isAtLeastO) {
            AudioManagerCompat.abandonAudioFocusRequest(audioManager!!, audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager!!.abandonAudioFocus(this)
        }
    }
    //endregion

    private fun playerJob(): Job {
        return CoroutineScope(Dispatchers.Default).launch(CoroutineName("Service Job")) {
            cmd = CMD_NONE
            val vars = IntArray(8)
            var lastRecognized = 0
            do {
                currentFileName = queue!!.filename // Used in reconnection

                // If this file is unrecognized, and we're going backwards, go to previous
                // If we're at the start of the list, go to the last recognized file
                if (currentFileName!!.isEmpty() || !testModule(currentFileName!!)) {
                    logW("$currentFileName: unrecognized format")
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
                val defpan = PrefManager.defaultPan
                logI("Set default pan to $defpan")
                Xmp.setPlayer(Xmp.PLAYER_DEFPAN, defpan)

                // Ditto if we can't load the module
                logI("Load $currentFileName")
                if (Xmp.loadModule(currentFileName!!) < 0) {
                    logE("Error loading $currentFileName")
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

                isLoaded = true

                val interpTypes = intArrayOf(
                    Xmp.INTERP_NEAREST,
                    Xmp.INTERP_LINEAR,
                    Xmp.INTERP_SPLINE
                )
                val temp = PrefManager.interpType.toInt()
                var interpType: Int
                interpType = if (temp in 1..2) {
                    interpTypes[temp]
                } else {
                    Xmp.INTERP_LINEAR
                }
                if (!PrefManager.interpolate) {
                    interpType = Xmp.INTERP_NEAREST
                }

                Xmp.startPlayer(sampleRate)

                if (ducking) {
                    Xmp.setPlayer(Xmp.PLAYER_VOLUME, DUCK_VOLUME)
                }

                // Unmute all channels
                for (i in 0..63) {
                    Xmp.mute(i, 0)
                }

                // Set player amplification
                Xmp.setPlayer(Xmp.PLAYER_AMP, PrefManager.volumeBoost.toInt())
                // Set player stereo mix
                Xmp.setPlayer(Xmp.PLAYER_MIX, PrefManager.stereoMix)
                // Interpolation type
                Xmp.setPlayer(Xmp.PLAYER_INTERP, interpType)
                // DSP lowpass filter
                Xmp.setPlayer(Xmp.PLAYER_DSP, Xmp.DSP_LOWPASS)

                var flags = Xmp.getPlayer(Xmp.PLAYER_CFLAGS)
                flags = if (PrefManager.amigaMixer) {
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
                eventBus.post(NewModCallback())

                // Do this last to avoid static popping.
                if (isPlayerPaused)
                    mediaSession!!.controller.transportControls.play()

                logI("Enter play loop")
                do {
                    // seq_duration, length, pattern, channel, instruments, sample, num_sequences, _sequence
                    Xmp.getModVars(vars)

                    setMetaData(getModName(), Xmp.getModType(), vars[0].toLong())
                    updateNotification()

                    while (cmd == CMD_NONE) {
                        discardBuffer = false

                        // Wait if paused
                        while (isPlayerPaused && cmd != CMD_STOP) {
                            delay(100)
                            watchdog!!.refresh()
                        }
                        if (discardBuffer) {
                            logD("discard buffer")
                            Xmp.dropAudio()
                            break
                        }

                        // Wait if no buffers available
                        while (!Xmp.hasFreeBuffer() && !isPlayerPaused && cmd == CMD_NONE) {
                            delay(40)
                        }

                        // Fill a new buffer
                        if (Xmp.fillBuffer(looped) < 0) {
                            break
                        }
                        watchdog!!.refresh()
                    }

                    // Subsong explorer
                    // Do all this if we've exited normally and explorer is active
                    playNewSequence = false
                    if (playerAllSequences && cmd == CMD_NONE) {
                        sequenceNumber++
                        logI("Play sequence $sequenceNumber")
                        if (Xmp.setSequence(sequenceNumber)) {
                            playNewSequence = true
                            notifyNewSequence()
                        }
                    }
                } while (playNewSequence)

                Xmp.endPlayer()
                isLoaded = false

                // notify end of module to our clients
                eventBus.post(EndModCallback())

                // Study the purpose of this
                // if we have clients, make sure we can release module
                var timeout = 0
                while (!canRelease && timeout < 20) {
                    delay(100)
                    timeout++
                }

                logI("Release module")
                Xmp.releaseModule()

                // Used when current files are replaced by a new set
                if (restart) {
                    logI("Restart")
                    queue!!.index = startIndex - 1
                    cmd = CMD_NONE
                    restart = false
                } else if (cmd == CMD_PREV) {
                    queue!!.previous()
                }
            } while (cmd != CMD_STOP && queue!!.next())

            updateData = false // stop getChannelData update

            Xmp.deinit()

            watchdog!!.stop()
            abandonAudioFocus()

            logI("Stop service")
            end(RESULT_OK)
        }
    }

    companion object {
        const val RESULT_OK = 0
        const val RESULT_CANT_OPEN_AUDIO = 1
        const val RESULT_NO_AUDIO_FOCUS = 2
        const val RESULT_WATCHDOG = 3

        private const val CMD_NONE = 0
        private const val CMD_NEXT = 1
        private const val CMD_PREV = 2
        private const val CMD_STOP = 3

        private const val MIN_BUFFER_MS = 80
        private const val MAX_BUFFER_MS = 1000
        private const val DUCK_VOLUME = 0x500

        // Keep this well under a second to keep activity and notification time in sync.
        private const val SYNC_DELAY = 350L // Millis

        @JvmField
        var isPlayerAlive: MutableLiveData<Boolean> = MutableLiveData(false)

        @JvmField
        var isLoaded = false
    }
}
