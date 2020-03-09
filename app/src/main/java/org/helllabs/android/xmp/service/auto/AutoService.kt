package org.helllabs.android.xmp.service.auto

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.STREAM_MUSIC
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat.*
import android.support.v4.media.MediaMetadataCompat.Builder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
import android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.media.AudioManagerCompat.AUDIOFOCUS_GAIN
import androidx.media.MediaBrowserServiceCompat
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.browser.playlist.Playlist
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.listNoSuffix
import org.helllabs.android.xmp.extension.isAtLeastO
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.service.utils.Watchdog
import org.helllabs.android.xmp.util.FileUtils
import org.helllabs.android.xmp.util.InfoCache
import org.helllabs.android.xmp.util.Log

// TODO: [Guntech] Dune appears to not to play correctly.
//      I don't hear anything from channel 5; Emu quirk or what?
//      Everything other song appears fine.
// TODO: Playlist / Queue
// TODO: Shuffle and Loop
// TODO: Cleanup this mess
// TODO: Must !MEET! criteria - https://developer.android.com/docs/quality-guidelines/car-app-quality
class AutoService : MediaBrowserServiceCompat(), AudioManager.OnAudioFocusChangeListener {

    companion object {
        private val TAG = AutoService::class.java.simpleName

        const val ROOT = "root"
        const val CUSTOM_ACTION_SHUFFLE = "shuffle"
        const val CUSTOM_ACTION_REPEAT = "repeat"
        const val CUSTOM_ACTION_SEQUENCES = "allSequences"

        private const val MIN_BUFFER_MS = 80
        private const val MAX_BUFFER_MS = 1000

        private const val CMD_NONE = 0
        private const val CMD_NEXT = 1
        private const val CMD_PREV = 2
        private const val CMD_STOP = 3

        private const val DUCK_VOLUME = 0x500

        const val EXTRA_MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"
    }

    private lateinit var becomingNoisyReceiver: NoisyReceiver

    private val metadatasPlaylist: MutableMap<String, MediaDescriptionCompat> = mutableMapOf()
    private val metadatasItems: MutableMap<String, MediaDescriptionCompat> = mutableMapOf()

    var xmpVolume = 0
    var sampleRate = 0
    var isLoaded = false
    var isAllSequence = false
    var isLoopEnabled = false
    var isShuffleEnabled = false

    private var sequenceNumber: Int = 0
    private var isPlayerPaused: Boolean = false
    private var discardBuffer: Boolean = false
    private var playThread: Thread? = null
    private var playerFileName: String? = null
    private var currentSongName: String? = null
    private var currentPlaybackState: Int? = null
    private var audioManager: AudioManager? = null
    private var session: MediaSessionCompat? = null
    private var watchdog: Watchdog = Watchdog(10)

    @Volatile
    private var cmd: Int = 0
    @Volatile
    private var hasFocus = false

    override fun onCreate() {
        super.onCreate()
        session = MediaSessionCompat(this, TAG).apply {
            @Suppress("DEPRECATION")
            setFlags(FLAG_HANDLES_MEDIA_BUTTONS or FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(callback)
            setSessionToken(sessionToken)
        }

        becomingNoisyReceiver = NoisyReceiver(this, sessionToken!!)

        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        /* XMP */
        sampleRate = PrefManager.samplingRate.toInt()
        isAllSequence = PrefManager.allSequences

        var bufferMs = PrefManager.bufferMS
        when {
            bufferMs < MIN_BUFFER_MS -> bufferMs = MIN_BUFFER_MS
            bufferMs > MAX_BUFFER_MS -> bufferMs = MAX_BUFFER_MS
        }

        when (Xmp.init(sampleRate, bufferMs)) {
            true -> println("audioInitialized = true")
            false -> Log.e(TAG, "error initializing audio")
        }

        watchdog.apply {
            setOnTimeoutListener(object : Watchdog.OnTimeoutListener {
                override fun onTimeout() {
                    Log.w(TAG, "Stopped by watchdog")
                    playThread?.interrupt()
                    playThread = null
                }
            })
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        deInit()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved")
        deInit()
    }

    private fun deInit() {
        isPlayerPaused = true

        cmd = CMD_STOP

        playThread?.interrupt()
        playThread = null

        session!!.isActive = false
        session?.release()

        becomingNoisyReceiver.unregister()
        abandonAudioFocus()

        watchdog.stop()

        Xmp.stopAudio()
        // Xmp.releaseModule()
        Xmp.deinit()

        stopSelf()
    }

    override fun onAudioFocusChange(focusChange: Int) {
        Log.d(TAG, "onAudioFocusChange: $focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                callback.onPause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                callback.onPause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Apparently API >= O can duck automatically, I wonder if XMP can needs this?
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    Xmp.setVolume(DUCK_VOLUME)
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Xmp.setVolume(xmpVolume)

                if (isPlayerPaused)
                    callback.onPlay()
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun retrieveRoot(
            title: String,
            bitmap: Bitmap,
            flag: Int
    ): MediaBrowserCompat.MediaItem {
        // println("resourceName $title")
        val mediaDescriptionBuilder = MediaDescriptionCompat.Builder().apply {
            setMediaId(title)
            setTitle(title)
            setIconBitmap(bitmap)
        }
        return MediaBrowserCompat.MediaItem(mediaDescriptionBuilder.build(), flag)
    }

    @Suppress("SameParameterValue")
    private fun retrievePlaylists(resourceName: String, flag: Int): MediaBrowserCompat.MediaItem {
        // println("resourceName $resourceName")
        val mediaDescriptionBuilder = MediaDescriptionCompat.Builder().apply {
            setMediaId(resourceName)
            setTitle(resourceName)
            setIconBitmap(getBitmapIcon())
        }.build()
        metadatasPlaylist[mediaDescriptionBuilder.mediaId.toString()] = mediaDescriptionBuilder
        return MediaBrowserCompat.MediaItem(mediaDescriptionBuilder, flag)
    }

    @Suppress("SameParameterValue")
    private fun retrievePlaylistItems(
            resourceName: PlaylistItem,
            flag: Int
    ): MediaBrowserCompat.MediaItem {
        // println("resourceName $resourceName")
        val mediaDescriptionBuilder = MediaDescriptionCompat.Builder().apply {
            setMediaId(resourceName.name)
            setTitle(resourceName.name)
            setIconBitmap(getBitmapIcon())
            setExtras(Bundle().apply {
                putString("filepath", resourceName.file!!.absolutePath)
            })
        }.build()
        metadatasItems[mediaDescriptionBuilder.mediaId.toString()] = mediaDescriptionBuilder
        return MediaBrowserCompat.MediaItem(mediaDescriptionBuilder, flag)
    }

    override fun onGetRoot(
            clientPackageName: String,
            clientUid: Int,
            rootHints: Bundle?
    ): BrowserRoot? {
        println("onGetRoot")
        println("clientPackageName $clientPackageName")
        println("clientUid $clientUid")
        println("rootHints $rootHints")

        val extras = Bundle().apply {
            putBoolean(EXTRA_MEDIA_SEARCH_SUPPORTED, true)
        }

        return BrowserRoot(ROOT, extras)
    }

    override fun onLoadChildren(
            parentId: String,
            result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        val children: ArrayList<MediaBrowserCompat.MediaItem> = arrayListOf()
        println("onLoadChildren $parentId")

        when (parentId) {
            ROOT -> {
                // Initial root listing as Browsable
                children.add(retrieveRoot("Playlists", getPlaylistsIcon(), FLAG_BROWSABLE))
                children.add(retrieveRoot("Explorer", getExplorerIcon(), FLAG_BROWSABLE))
            }
            "Playlists" -> {
                // Playlist listing as Playable
                listNoSuffix().map {
                    children.add(retrievePlaylists(it, FLAG_PLAYABLE))
                }
            }
            "Explorer" -> {
                // Playlist listing as Browsable
                listNoSuffix().map {
                    children.add(retrievePlaylists(it, FLAG_BROWSABLE))
                }
            }
            else -> {
                // Playlist Items from Browsable
                val playlistItems = Playlist(this, parentId).list
                playlistItems.forEach {
                    children.add(retrievePlaylistItems(it, FLAG_PLAYABLE))
                }
            }
        }

        result.sendResult(children)
    }

    override fun onSearch(
            query: String,
            extras: Bundle?,
            result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        println("onSearch")
        println("query $query")
        println("extras $extras")
        println("result $result")
        super.onSearch(query, extras, result)
    }

    private val callback = object : MediaSessionCompat.Callback() {
        private var currentStream: MediaDescriptionCompat? = null

        private fun playCurrentStream() {
            isPlayerPaused = false
            Xmp.stopModule()

            val path = currentStream!!.extras!!.getString("filepath")!!

            println("playCurrentStream $path")

            if (!InfoCache.testModule(path)) {
                throw RuntimeException("Bad Module")
            }

            hasFocus = requestAudioFocus()
            if (!hasFocus)
                throw RuntimeException("playCurrentStream AudioFocus failed")

            playerFileName = path

            if (playThread == null) {
                playThread = Thread(PlayRunnable())
                playThread!!.start()
                watchdog.start()
            }
        }

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle) {
            becomingNoisyReceiver.register()
            println("mediaId $mediaId")
            println("extras ${extras.getString("filename")}")

            currentStream = metadatasItems[mediaId]
            currentSongName = mediaId

            if (isLoaded) {
                // We need to reset the time if we're just hot swapping songs on the fly.
                // endPlayer() nullifies *fi which contains the time
                // Not sure about the other's importance, if needed
                Xmp.stopModule()
                Xmp.releaseModule()
                Xmp.endPlayer()
            }

            playCurrentStream()
        }

        override fun onPlay() {
            becomingNoisyReceiver.register()
            if (isPlayerPaused && requestAudioFocus()) {
                Xmp.restartAudio()
                isPlayerPaused = false
                playbackState(STATE_PLAYING)
            }
        }

        override fun onPause() {
            becomingNoisyReceiver.unregister()
            if (!isPlayerPaused) {
                Xmp.stopAudio()
                isPlayerPaused = true
                playbackState(STATE_PAUSED)
            }
        }

        override fun onSkipToNext() {
            Xmp.stopModule()
            cmd = CMD_NEXT
            if (isPlayerPaused) {
                discardBuffer = true
            }
            playCurrentStream()
        }

        override fun onSkipToPrevious() {
            Xmp.stopModule()
            cmd = CMD_PREV
            if (isPlayerPaused) {
                discardBuffer = true
            }

            playCurrentStream()
        }

        override fun onStop() {
            super.onStop()

            if (!isPlayerPaused) {
                cmd = CMD_STOP
                Xmp.stopAudio()
            }

            abandonAudioFocus()
            Xmp.releaseModule()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
            Xmp.seek(pos.toInt())
        }

        override fun onPlayFromSearch(query: String, extras: Bundle) {
            println("onPlayFromSearch")
            println("Query: $query")
            println("extras: $extras")
            onPlay()
        }

        override fun onCustomAction(action: String, extras: Bundle?) {
            println("Action: $action")

            if (action == CUSTOM_ACTION_REPEAT) {
                isLoopEnabled = !isLoopEnabled
                println("loop $isLoopEnabled")
            }

            if (action == CUSTOM_ACTION_SHUFFLE) {
                isShuffleEnabled = !isShuffleEnabled
                println("shuffle $isShuffleEnabled")
            }

            if (action == CUSTOM_ACTION_SEQUENCES) {
                isAllSequence = !isAllSequence
                println("allSeq $isAllSequence")
            }

            // We should update our action buttons
            playbackState()

            super.onCustomAction(action, extras)
        }
    }

    private fun requestAudioFocus(): Boolean {
        val request: Int = if (isAtLeastO()) {
            audioManager!!.requestAudioFocus(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                        setOnAudioFocusChangeListener(this@AutoService)
                        setAudioAttributes(AudioAttributes.Builder().run {
                            setUsage(AudioAttributes.USAGE_MEDIA)
                            setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            build()
                        })
                    }.build()
            )
        } else {
            @Suppress("DEPRECATION")
            audioManager!!.requestAudioFocus(this@AutoService, STREAM_MUSIC, AUDIOFOCUS_GAIN)
        }

        return request == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun abandonAudioFocus() {
        if (isAtLeastO()) {
            audioManager!!.abandonAudioFocusRequest(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setOnAudioFocusChangeListener(this@AutoService)
                            .build())
        } else {
            @Suppress("DEPRECATION")
            audioManager!!.abandonAudioFocus(this@AutoService)
        }
    }

    private fun playbackState(state: Int? = null) {
        if (state != null)
            currentPlaybackState = state

        session!!.setPlaybackState(
                PlaybackStateCompat.Builder()
                        .setActions(getPlaybackActions())
                        .addCustomAction(
                                CUSTOM_ACTION_REPEAT,
                                getString(R.string.button_toggle_loop),
                                if (isLoopEnabled) R.drawable.ic_auto_repeat_on
                                else R.drawable.ic_auto_repeat_off
                        )
                        .addCustomAction(
                                CUSTOM_ACTION_SHUFFLE,
                                getString(R.string.button_toggle_shuffle),
                                if (isShuffleEnabled) R.drawable.ic_auto_shuffle_on
                                else R.drawable.ic_auto_shuffle_off
                        )
                        .addCustomAction(
                                CUSTOM_ACTION_SEQUENCES,
                                getString(R.string.sheet_button_allseqs),
                                if (isAllSequence) R.drawable.ic_auto_sequence_on
                                else R.drawable.ic_auto_sequence_off
                        )
                        .setState(currentPlaybackState!!,
                                Xmp.time().toLong(),
                                1f,
                                SystemClock.elapsedRealtime()
                        )
                        .build()
        )
    }

    private fun getPlaybackActions(): Long = ACTION_PLAY or ACTION_PAUSE or ACTION_SKIP_TO_NEXT or
            ACTION_SKIP_TO_PREVIOUS or ACTION_SEEK_TO

    private fun getBitmapIcon(): Bitmap =
            AppCompatResources.getDrawable(
                    applicationContext, R.mipmap.ic_launcher_foreground)!!.toBitmap()

    private fun getExplorerIcon(): Bitmap =
            AppCompatResources.getDrawable(applicationContext, R.drawable.ic_folder)!!.toBitmap()

    private fun getPlaylistsIcon(): Bitmap =
            AppCompatResources.getDrawable(applicationContext, R.drawable.ic_file)!!.toBitmap()

    private inner class PlayRunnable : Runnable {
        @Synchronized
        override fun run() {
            cmd = CMD_NONE

            val vars = IntArray(8)

            xmpVolume = Xmp.getVolume()

            do {
                // If this file is unrecognized, and we're going backwards, go to previous
                // If we're at the start of the list, go to the last recognized file
                if (playerFileName == null || !InfoCache.testModule(playerFileName!!)) {
                    cmd = CMD_STOP
                    break
                    // continue
                }

                // Set default pan before we load the module
                Log.i(TAG, "Set default pan to ${PrefManager.defaultPan}")
                Xmp.setPlayer(Xmp.PLAYER_DEFPAN, PrefManager.defaultPan)

                // Ditto if we can't load the module
                Log.i(TAG, "Load " + playerFileName!!)

                Xmp.loadModule(playerFileName!!)

                cmd = CMD_NONE

                isLoaded = true

                val volBoost = PrefManager.volumeBoost
                val interpTypes =
                        intArrayOf(Xmp.INTERP_NEAREST, Xmp.INTERP_LINEAR, Xmp.INTERP_SPLINE)
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

                Xmp.setPlayer(Xmp.PLAYER_AMP, Integer.parseInt(volBoost))
                Xmp.setPlayer(Xmp.PLAYER_MIX, PrefManager.stereoMix)
                Xmp.setPlayer(Xmp.PLAYER_INTERP, interpType)
                Xmp.setPlayer(Xmp.PLAYER_DSP, Xmp.DSP_LOWPASS)

                var flags = Xmp.getPlayer(Xmp.PLAYER_CFLAGS)
                flags = if (PrefManager.amigaMixer) {
                    flags or Xmp.FLAGS_A500
                } else {
                    flags and Xmp.FLAGS_A500.inv()
                }
                Xmp.setPlayer(Xmp.PLAYER_CFLAGS, flags)

                sequenceNumber = 0
                var playNewSequence: Boolean
                Xmp.setSequence(sequenceNumber)
                Xmp.playAudio()
                Xmp.getModVars(vars)

                var modName = Xmp.getModName()
                if (modName.isEmpty())
                    modName = FileUtils.basename(playerFileName)
                val metaData = Builder().apply {
                    putString(METADATA_KEY_TITLE, modName)
                    putString(METADATA_KEY_ARTIST, Xmp.getModType())
                    putLong(METADATA_KEY_DURATION, vars[0].toLong())
                    putBitmap(METADATA_KEY_ALBUM_ART, getBitmapIcon())
                }.build()
                session!!.setMetadata(metaData)

                // TODO: Handle loading next song in Queue

                // TODO: Handle updating controls and stuff
                // Everything appears to be OKAY, set the playback state.
                playbackState(STATE_PLAYING)

                // Unmute all channels
                for (i in 0..63) {
                    Xmp.mute(i, 0)
                }

                Log.i(TAG, "Enter play loop")
                do {
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
                        if (Xmp.fillBuffer(isLoopEnabled) < 0) {
                            break
                        }

                        watchdog.refresh()
                    }

                    // Subsong explorer
                    // Do all this if we've exited normally and explorer is active
                    playNewSequence = false
                    if (isAllSequence && cmd == CMD_NONE) {
                        sequenceNumber++

                        Log.i(TAG, "Play sequence $sequenceNumber")
                        if (Xmp.setSequence(sequenceNumber)) {
                            playNewSequence = true
                        }
                    }
                } while (playNewSequence)

                Xmp.endPlayer()

                isLoaded = false

                var timeout = 0
                try {
                    while (timeout < 20) {
                        Thread.sleep(100)
                        timeout++
                    }
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Sleep interrupted: $e")
                }

                Log.i(TAG, "Release module")
                Xmp.releaseModule()
            } while (cmd != CMD_STOP)
        }
    }
}
