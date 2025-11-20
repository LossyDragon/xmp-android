package org.helllabs.android.xmp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.runtime.*
import androidx.core.app.NotificationCompat
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import androidx.media.session.MediaButtonReceiver
import java.lang.ref.WeakReference
import java.util.LinkedList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.FrameInfo
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.model.ModVars
import timber.log.Timber

typealias MediaStyle = androidx.media.app.NotificationCompat.MediaStyle

enum class EndPlayback(val code: Int) {
    OK(0),
    ERROR_FOCUS(-1),
    ERROR_AUDIO(-2),
    ERROR_WATCHDOG(-3),
    ERROR_INIT(-4)
}

@Stable
sealed class PlayerEvent {
    data class EndPlay(val result: EndPlayback) : PlayerEvent()
    data class ErrorMessage(val msg: String) : PlayerEvent()
    data class NewMod(val isPrevious: Boolean) : PlayerEvent()
    data object EndMod : PlayerEvent()
    data object NewSequence : PlayerEvent()
    data object Paused : PlayerEvent()
    data object Play : PlayerEvent()
}

class PlayerBinder(playerService: PlayerService) : Binder() {
    private val service = WeakReference(playerService)

    fun getService(): PlayerService? = service.get()
}

class PlayerService :
    Service(),
    AudioManager.OnAudioFocusChangeListener {

    companion object {
        private const val CHANNEL_ID = "xmp"

        private const val CMD_NONE = 0
        private const val CMD_NEXT = 1
        private const val CMD_PREV = 2
        private const val CMD_STOP = 3

        val isAlive = MutableStateFlow(false)
        val isPlaying = MutableStateFlow(false)
    }

    private val binder = PlayerBinder(this)
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private val _playerEvent = MutableSharedFlow<PlayerEvent>()
    val playerEvent = _playerEvent.asSharedFlow()

    private lateinit var audioFocusRequest: AudioFocusRequestCompat
    private lateinit var audioManager: AudioManager
    private lateinit var mediaSession: MediaSessionCompat

    private var playThread: Thread? = null
    private lateinit var watchdog: Watchdog

    lateinit var mediaController: MediaControllerCompat
        private set

    private val playlist: LinkedList<MediaSessionCompat.QueueItem> = LinkedList()
    private var isAudioFocused: Boolean = false

    private var playerRestart = false
    private var playerSequence: Int = 0
    private var playerVolume: Int = 0
    private var playlistPosition: Int = 0
    private var currentFileUri: Uri = Uri.EMPTY

    var isRepeating: Boolean = false
        private set
    var playAllSequences: Boolean = false
        private set

    private var cmd: Int = 0
    private var discardBuffer: Boolean = false

    lateinit var logo: Bitmap

    override fun onCreate() {
        super.onCreate()
        Timber.i("Create service")

        initializeMediaSession()
        initializeAudioFocus()
        createNotificationChannel()

        initializeXmpPlayer()

        logo = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_foreground)

        mediaController = MediaControllerCompat(this, mediaSession.sessionToken)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")

        cmd = CMD_STOP

        mediaController.transportControls.stop()

        watchdog.stop()

        mediaSession.isActive = false
        mediaSession.release()

        playThread = null
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_PLAY" -> mediaSession.controller.transportControls.play()
            "ACTION_PAUSE" -> mediaSession.controller.transportControls.pause()
            "ACTION_STOP" -> mediaSession.controller.transportControls.stop()
            "ACTION_NEXT" -> mediaSession.controller.transportControls.skipToNext()
            "ACTION_PREVIOUS" -> mediaSession.controller.transportControls.skipToPrevious()
        }
        return START_STICKY
    }

    private fun initializeAudioFocus() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        val audioAttributes = AudioAttributesCompat.Builder()
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
            .build()

        audioFocusRequest = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(this)
            .build()
    }

    override fun onAudioFocusChange(focusChange: Int) {
        Timber.d("onAudioFocusChange: $focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Resume playback
                Xmp.setVolume(playerVolume)
                if (!isPlaying.value) mediaController.transportControls.play()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower the volume
                Xmp.setVolume(Xmp.DUCK_VOLUME)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Pause playback
                mediaController.transportControls.pause()
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                // Stop playback
                mediaController.transportControls.stop()
            }
        }
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(this, "PlayerService")

        @Suppress("DEPRECATION")
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        mediaSession.setCallback(
            object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    Timber.d("MediaSessionCompat onPlay")

                    startService(Intent(applicationContext, PlayerService::class.java))
                    mediaSession.isActive = true

                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    showNotification()

                    if (!isPlaying.value) {
                        Xmp.restartAudio()
                    }

                    if (!isAudioFocused) {
                        requestAudioFocus()
                    }

                    isPlaying.value = true

                    serviceScope.launch {
                        _playerEvent.emit(PlayerEvent.Play)
                    }
                }

                override fun onPause() {
                    Timber.d("MediaSessionCompat onPause")

                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    showNotification()

                    if (isAudioFocused) {
                        abandonAudioFocus()
                    }

                    if (isPlaying.value) {
                        Xmp.stopAudio()
                    }

                    isPlaying.value = false

                    serviceScope.launch {
                        _playerEvent.emit(PlayerEvent.Paused)
                    }
                }

                override fun onStop() {
                    Timber.d("MediaSessionCompat onStop")
                    cmd = CMD_STOP
                }

                override fun onSkipToNext() {
                    Timber.d("MediaSessionCompat onSkipToNext")
                    Xmp.stopModule()
                    cmd = CMD_NEXT
                    discardBuffer = true
                    if (!isPlaying.value) {
                        mediaController.transportControls.play()
                    }
                }

                override fun onSkipToPrevious() {
                    Timber.d("MediaSessionCompat onSkipToPrevious")
                    if (Xmp.time() > 3000) {
                        Xmp.seek(0)
                    } else {
                        Xmp.stopModule()
                        cmd = CMD_PREV
                    }
                    discardBuffer = true
                    if (!isPlaying.value) {
                        mediaController.transportControls.play()
                    }
                }

                override fun onSeekTo(pos: Long) {
                    Timber.d("MediaSessionCompat onSeekTo $pos")
                    Xmp.seek(pos.toInt())
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                }
            }
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            NotificationChannel(CHANNEL_ID, "Player", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Player notification for controls"
            }.also(notificationManager::createNotificationChannel)
        }
    }

    private fun initializeXmpPlayer() {
        val bufferMs = PrefManager.bufferMs.coerceIn(Xmp.MIN_BUFFER_MS, Xmp.MAX_BUFFER_MS)

        if (!Xmp.init(PrefManager.samplingRate, bufferMs)) {
            Timber.e("Unable to init Xmp audio (OpenSLES)")

            serviceScope.launch {
                _playerEvent.emit(PlayerEvent.EndPlay(EndPlayback.ERROR_INIT))
            }

            stopSelf()
            return
        }

        playerVolume = Xmp.getVolume()
        playAllSequences = PrefManager.allSequences

        isAlive.value = false
        isPlaying.value = false

        playAllSequences = PrefManager.allSequences

        watchdog = Watchdog(10).apply {
            setOnTimeoutListener {
                Timber.w("Stopped by watchdog")

                serviceScope.launch {
                    _playerEvent.emit(PlayerEvent.EndPlay(EndPlayback.ERROR_WATCHDOG))
                }

                abandonAudioFocus()
                stopSelf()
            }
            start()
        }
    }

    private fun requestAudioFocus() {
        val result = AudioManagerCompat.requestAudioFocus(audioManager, audioFocusRequest)
        isAudioFocused = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        Timber.d("Request Audio Focus was: $isAudioFocused")
    }

    private fun abandonAudioFocus() {
        AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest)
        isAudioFocused = false
        Timber.d("Request Audio Focus Abandoned")
    }

    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, Xmp.time().toLong(), 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    private fun showNotification() {
        val controller = mediaSession.controller
        val mediaMetadata = controller.metadata

        fun action(icon: Int, title: String, action: String) = NotificationCompat.Action(
            icon,
            title,
            PendingIntent.getService(
                this,
                0,
                Intent(this, PlayerService::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        // We failed to get mediaMetadata somehow?!?!
        if (mediaMetadata == null) {
            Timber.e("mediaMetadata is null, falling back to a more basic notification")
            val title = StorageManager.getFileName(currentFileUri) ?: "<Unable to get module name>"
            val notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
                setContentTitle(title)
                setContentText("Click to open player")
                setSmallIcon(R.drawable.ic_notification)
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                setContentIntent(
                    PendingIntent.getActivity(
                        this@PlayerService,
                        0,
                        Intent(this@PlayerService, PlayerActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }.build()

            startForeground(1, notification)
            return
        }

        val description = mediaMetadata.description
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setContentTitle(description.title)
            setContentText(description.subtitle)
            setSubText(description.description)
            setLargeIcon(description.iconBitmap)
            setContentIntent(
                PendingIntent.getActivity(
                    this@PlayerService,
                    0,
                    Intent(this@PlayerService, PlayerActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            ) /* controller.sessionActivity */
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this@PlayerService,
                    PlaybackStateCompat.ACTION_STOP
                )
            )
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(R.drawable.ic_notification)
            addAction(action(R.drawable.ic_action_previous, "Previous", "ACTION_PREVIOUS"))
            addAction(
                if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                    action(R.drawable.ic_action_pause, "Pause", "ACTION_PAUSE")
                } else {
                    action(R.drawable.ic_action_play, "Play", "ACTION_PLAY")
                }
            )
            addAction(action(R.drawable.ic_action_next, "Next", "ACTION_NEXT"))
            setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        }.build()

        startForeground(1, notification)
    }

    fun toggleLoop(): Boolean {
        isRepeating = !isRepeating
        return isRepeating
    }

    fun toggleAllSequences(): Boolean {
        playAllSequences = !playAllSequences
        return playAllSequences
    }

    fun setSequence(sequence: Int): Int {
        val ret = Xmp.setSequence(sequence)
        if (ret) {
            playerSequence = sequence
            serviceScope.launch {
                _playerEvent.emit(PlayerEvent.NewSequence)
            }
        }
        return playerSequence
    }

    fun getFileName(): String = StorageManager.getFileName(currentFileUri) ?: "<Unknown Title>"

    fun play(
        fileList: List<Uri>,
        start: Int,
        shuffle: Boolean,
        loopList: Boolean,
        keepFirst: Boolean
    ) {
        if (fileList.isEmpty()) {
            return
        }

        playlist.clear()
        add(fileList)

        if (shuffle) {
            if (keepFirst) {
                Timber.d("play: Shuffle + KeepFirst")
                playlist.shuffleWithFirst(start)
                playlistPosition = 0
            } else {
                Timber.d("play: Shuffle + No KeepFirst ")
                playlist.shuffle()
            }
        } else {
            Timber.d("play: No Shuffle")
            playlistPosition = start
        }

        cmd = CMD_NONE

        Timber.d("Start: $playlistPosition")
        Timber.d("Size: ${playlist.size}")

        isRepeating = loopList

        if (isAlive.value) {
            Timber.i("Use existing player thread")
            playerRestart = true
            mediaController.transportControls.skipToNext()
        } else {
            Timber.i("Start player thread")
            playThread = Thread(PlayRunnable())
            playThread!!.start()
        }

        isAlive.value = true
    }

    fun add(list: List<Uri>) {
        if (list.isEmpty()) {
            return
        }

        val items = list.mapNotNull { item ->
            val modInfo = ModInfo()
            if (Xmp.testFromFd(item, modInfo)) {
                val desc = MediaDescriptionCompat.Builder()
                    .setTitle(modInfo.name.ifEmpty { item.lastPathSegment })
                    .setMediaUri(item)
                    .setSubtitle(modInfo.type)
                    .build()

                MediaSessionCompat.QueueItem(desc, desc.hashCode().toLong())
            } else {
                Timber.w("Item: $item was not a valid module")
                null
            }
        }

        playlist.addAll(items)
    }

    private fun <T> MutableList<T>.shuffleWithFirst(index: Int) {
        if (index !in indices) throw IndexOutOfBoundsException("Index out of bounds: $index")

        val firstItem = removeAt(index)
        shuffle()
        add(0, firstItem)
    }

    private inner class PlayRunnable : Runnable {
        override fun run() {
            cmd = CMD_NONE

            var lastRecognized = 0
            var oldPos = -1
            var skipToPrevious = false

            isPlaying.value = true

            do {
                val queueItem = playlist[playlistPosition]

                currentFileUri = queueItem.description.mediaUri!!

                // If this file is unrecognized, and we're going backwards, go to previous
                // If we're at the start of the list, go to the last recognized file
                val isValid = queueItem.description.mediaUri?.let { Xmp.testFromFd(it) } ?: false
                if (!isValid) {
                    Timber.w("$currentFileUri: unrecognized format")
                    serviceScope.launch {
                        val module = currentFileUri.lastPathSegment?.ifEmpty { "module was" }
                        _playerEvent.emit(
                            PlayerEvent.ErrorMessage(
                                "$module unrecognized. Skipping to next module"
                            )
                        )
                    }
                    if (cmd == CMD_PREV) {
                        if (playlistPosition <= 0) {
                            // -1 because we have queue.next() in the while condition
                            playlistPosition = lastRecognized - 1
                            continue
                        }
                        playlistPosition.minus(2).coerceAtLeast(0)
                    }
                    continue
                }

                // Set default pan before we load the module
                val defpan = PrefManager.defaultPan
                Timber.i("Set default pan to $defpan")
                Xmp.setPlayer(Xmp.PLAYER_DEFPAN, defpan)

                // Ditto if we can't load the module
                Timber.i("Load $currentFileUri")
                if (Xmp.loadFromFd(currentFileUri) < 0) {
                    Timber.e("Error loading $currentFileUri")
                    if (cmd == CMD_PREV) {
                        if (playlistPosition <= 0) {
                            playlistPosition = lastRecognized - 1
                            continue
                        }
                        playlistPosition.minus(2).coerceAtLeast(0)
                    }
                    continue
                }

                lastRecognized = playlistPosition
                cmd = CMD_NONE

                val volBoost = PrefManager.volumeBoost

                val interp = intArrayOf(Xmp.INTERP_NEAREST, Xmp.INTERP_LINEAR, Xmp.INTERP_SPLINE)
                    .getOrElse(PrefManager.interpType) {
                        if (!PrefManager.interpolate) {
                            Xmp.INTERP_NEAREST
                        } else {
                            Xmp.INTERP_LINEAR
                        }
                    }

                Xmp.startPlayer(PrefManager.samplingRate)

                // Unmute all channels
                for (i in 0 until Xmp.MAX_CHANNELS) {
                    Xmp.mute(i, 0)
                }

                val flags = if (PrefManager.amigaMixer) {
                    Xmp.getPlayer(Xmp.PLAYER_CFLAGS) or Xmp.FLAGS_A500
                } else {
                    Xmp.getPlayer(Xmp.PLAYER_CFLAGS) and Xmp.FLAGS_A500.inv()
                }

                Xmp.setPlayer(Xmp.PLAYER_AMP, volBoost)
                Xmp.setPlayer(Xmp.PLAYER_CFLAGS, flags)
                Xmp.setPlayer(Xmp.PLAYER_DSP, Xmp.DSP_LOWPASS)
                Xmp.setPlayer(Xmp.PLAYER_INTERP, interp)
                Xmp.setPlayer(Xmp.PLAYER_MIX, PrefManager.stereoMix)
                Xmp.setPlayer(Xmp.PLAYER_VOLUME, 100)

                playerSequence = 0

                var playNewSequence: Boolean

                Xmp.setSequence(playerSequence)
                Xmp.playAudio()

                serviceScope.launch {
                    _playerEvent.emit(PlayerEvent.NewMod(isPrevious = skipToPrevious))
                    skipToPrevious = false
                }

                Timber.i("Enter play loop")
                do {
                    val modVars = ModVars()
                    Xmp.getModVars(modVars)

                    val metaData = MediaMetadataCompat.Builder().apply {
                        putBitmap(
                            MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                            logo
                        )
                        putString(
                            MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                            queueItem.description.mediaId
                        )
                        putString(
                            MediaMetadataCompat.METADATA_KEY_TITLE,
                            queueItem.description.title.toString()
                        )
                        putString(
                            MediaMetadataCompat.METADATA_KEY_ARTIST,
                            queueItem.description.subtitle.toString()
                        )
                        putLong(
                            MediaMetadataCompat.METADATA_KEY_DURATION,
                            modVars.seqDuration.toLong()
                        )
                    }.build()

                    mediaSession.setMetadata(metaData)
                    mediaController.transportControls.play()

                    while (cmd == CMD_NONE) {
                        discardBuffer = false

                        // Wait if paused
                        while (!isPlaying.value) {
                            try {
                                if (cmd == CMD_STOP) {
                                    break
                                }
                                Timber.d("Paused...")
                                Thread.sleep(1000)
                            } catch (_: InterruptedException) {
                                break
                            }
                            watchdog.refresh()
                        }

                        if (discardBuffer) {
                            Timber.d("discard buffer")
                            Xmp.dropAudio()
                            break
                        }

                        // Wait if no buffers available
                        while (!Xmp.hasFreeBuffer() && isPlaying.value && cmd == CMD_NONE) {
                            try {
                                Thread.sleep(40)
                            } catch (_: InterruptedException) {
                                // Nothing
                            }
                        }

                        // Fill a new buffer
                        if (Xmp.fillBuffer(isRepeating) < 0) {
                            break
                        }

                        watchdog.refresh()

                        // Periodically update notification state
                        val fi = FrameInfo()
                        Xmp.getInfo(fi)
                        if (fi.pos != oldPos) {
                            oldPos = fi.pos
                            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                        }
                    }

                    // Subsong explorer
                    // Do all this if we've exited normally and explorer is active
                    playNewSequence = false

                    if (playAllSequences && cmd == CMD_NONE) {
                        playerSequence++
                        Timber.i("Play sequence $playerSequence")
                        if (Xmp.setSequence(playerSequence)) {
                            playNewSequence = true
                            serviceScope.launch {
                                _playerEvent.emit(PlayerEvent.NewSequence)
                            }
                        }
                    }
                } while (playNewSequence)

                Xmp.endPlayer()

                // notify end of module to our clients
                serviceScope.launch {
                    _playerEvent.emit(PlayerEvent.EndMod)
                }

                Timber.i("Release module")
                Xmp.releaseModule()

                // Used when current files are replaced by a new set
                if (playerRestart) {
                    Timber.i("Restart")
                    // playlistPosition = 0
                    playerRestart = false
                    cmd = CMD_NONE
                } else if (cmd == CMD_PREV) {
                    Timber.d("Command: Previous")
                    playlistPosition = playlistPosition.minus(1).coerceAtLeast(0)
                    skipToPrevious = true
                } else {
                    playlistPosition = playlistPosition.plus(1)
                }
            } while (cmd != CMD_STOP && playlistPosition < playlist.size)

            Timber.d("Exiting play loop")

            watchdog.stop()

            Thread.sleep(100) // Let the player finish getting data

            Xmp.stopModule()
            Xmp.deinit()

            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)

            if (isAudioFocused) {
                abandonAudioFocus()
            }

            serviceScope.launch {
                _playerEvent.emit(PlayerEvent.EndPlay(EndPlayback.OK))
            }

            isAlive.value = false

            Timber.i("Stop service")
            stopSelf()
        }
    }
}
