package org.helllabs.android.xmp.service.auto

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.STREAM_MUSIC
import android.media.MediaMetadata
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
import android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
import android.support.v4.media.session.PlaybackStateCompat.*
import android.support.v4.media.session.PlaybackStateCompat.Builder
import androidx.media.AudioManagerCompat.AUDIOFOCUS_GAIN
import androidx.media.MediaBrowserServiceCompat
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.Xmp.INTERP_LINEAR
import org.helllabs.android.xmp.Xmp.INTERP_NEAREST
import org.helllabs.android.xmp.Xmp.INTERP_SPLINE
import org.helllabs.android.xmp.browser.playlist.Playlist
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.listNoSuffix
import org.helllabs.android.xmp.extension.isAtLeastO
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.service.utils.Watchdog
import org.helllabs.android.xmp.util.InfoCache
import org.helllabs.android.xmp.util.Log

// TODO: [Guntech] Dune appears to not to play correctly.
//      I don't hear anything from channel 5?....
// TODO: Playlist / Queue
// TODO: Shuffle and Loop
// TODO: Cleanup this mess
// TODO: Must !MEET! criteria - https://developer.android.com/docs/quality-guidelines/car-app-quality
class AutoService : MediaBrowserServiceCompat(), AudioManager.OnAudioFocusChangeListener {

    private lateinit var becomingNoisyReceiver: BecomingNoisyReceiver

    private var session: MediaSessionCompat? = null

    private val metadatasPlaylist: MutableMap<String, MediaMetadataCompat> = mutableMapOf()
    private val metadatasItems: MutableMap<String, MediaMetadataCompat> = mutableMapOf()

    var isLoaded = false

    private var playThread: Thread? = null

    private var currentSongName: String? = null

    var isLoopEnabled = false
    var isShuffleEnabled = false

    @Volatile
    private var cmd: Int = 0

    private var playerFileName: String? = null
    private var sequenceNumber: Int = 0
    private var restart: Boolean = false
    private var discardBuffer: Boolean = false
    var isPlayerPaused: Boolean = false
    private var watchdog: Watchdog = Watchdog(10)

    val playlists = mutableListOf<String>()

    private var audioManager: AudioManager? = null

    @Volatile
    private var hasFocus = false

    var xmpVolume = 0

    var sampleRate =  0

    override fun onCreate() {
        super.onCreate()
        session = MediaSessionCompat(this, TAG).apply {
            @Suppress("DEPRECATION")
            setFlags(FLAG_HANDLES_MEDIA_BUTTONS or FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(callback)
            setSessionToken(sessionToken)
        }

        becomingNoisyReceiver = BecomingNoisyReceiver(this, sessionToken!!)

        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        /* XMP */
        sampleRate = PrefManager.samplingRate.toInt()

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
                    stopSelf()
                }
            })
        }

        listNoSuffix().forEach {
            playlists.add(it)
            println(it)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        deinit()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved")
        deinit()
    }

    private fun deinit() {
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
        Xmp.releaseModule()
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

//    @Suppress("SameParameterValue")
//    private fun retrievePlaylists(resourceName: String, flag: Int): MediaItem {
//        //println("resourceName $resourceName")
//        val mediaMetadata: MediaMetadataCompat = MediaMetadataCompat.Builder()
//                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, resourceName)
//                .putString(MediaMetadata.METADATA_KEY_TITLE, resourceName)
//                .build()
//        metadatasPlaylist[mediaMetadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)] = mediaMetadata
//        return MediaItem(mediaMetadata.description, flag)
//    }

    @Suppress("SameParameterValue")
    private fun retrievePlaylistItems(resourceName: PlaylistItem, flag: Int): MediaItem {
        val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_foreground)
        //println("resourceName $resourceName")
        val mediaMetadata: MediaMetadataCompat = MediaMetadataCompat.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, resourceName.name)
                .putString(MediaMetadata.METADATA_KEY_TITLE, resourceName.name)
                .putBitmap(METADATA_KEY_ALBUM_ART, bitmap)
                .putString("filepath", resourceName.file!!.absolutePath)
                .build()
        metadatasItems[mediaMetadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)] = mediaMetadata
        return MediaItem(mediaMetadata.description, flag)
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        println("onGetRoot")
        return BrowserRoot(ROOT, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaItem>>) {
        val children: ArrayList<MediaItem> = ArrayList()

        println("onLoadChildren $parentId")

        if (ROOT == parentId) {
            playlists.map { item ->
                children.add(MediaItem(MediaDescriptionCompat.Builder()
                        .setMediaId(item)
                        .setTitle(item)
                        .build(),
                        FLAG_BROWSABLE
                ))
            }
        } else if (parentId != ROOT) {
            val playlistItems = Playlist(this, parentId).list.toMutableList()
            playlistItems.forEach {
                children.add(retrievePlaylistItems(it, FLAG_PLAYABLE))
            }
        }

        if (children.isEmpty())
            result.detach()
        else
            result.sendResult(children)
    }

    private val callback = object : MediaSessionCompat.Callback() {
        private var currentStream: MediaMetadataCompat? = null

        private fun playCurrentStream() {
            isPlayerPaused = false
            Xmp.stopModule()

            val path = currentStream!!.getString("filepath").toString()

            println("playCurrentStream $path")

            if (!InfoCache.testModule(path)) {
                throw RuntimeException("Bad Module")
            }

            if (!hasFocus) {
                hasFocus = requestAudioFocus()
            }

            session!!.setMetadata(currentStream)

            playerFileName = path

            if (playThread == null) {
                playThread = Thread(PlayRunnable())
                playThread!!.start()
                watchdog.start()
            }

            if (!hasFocus)
                throw RuntimeException("playCurrentStream AudioFocus failed")

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
            println("onPlay " + currentStream!!.getString(METADATA_KEY_MEDIA_ID))
            if (isPlayerPaused && requestAudioFocus()) {
                Xmp.restartAudio()
                isPlayerPaused = false
                playbackState(STATE_PLAYING)
            }
        }

        override fun onPause() {
            becomingNoisyReceiver.unregister()
            println("onPause $isPlayerPaused")
            if (!isPlayerPaused) {
                Xmp.stopAudio()
                isPlayerPaused = true
                playbackState(STATE_PAUSED)
            }
        }

        override fun onSkipToNext() {
            for (index in playlists.indices) {
                if (currentStream!!.getString(METADATA_KEY_MEDIA_ID) == playlists[index]) {
                    currentStream = metadatasPlaylist[playlists[(index + 1) % playlists.size]]
                    break
                }
            }

            Xmp.stopModule()
            cmd = CMD_NEXT
            if (isPlayerPaused) {
                discardBuffer = true
            }
            playCurrentStream()
        }

        override fun onSkipToPrevious() {
            for (index in playlists.indices) {
                if (currentStream!!.getString(METADATA_KEY_MEDIA_ID) == playlists[index]) {
                    currentStream = metadatasPlaylist[playlists[playlists.size + (index - 1) % playlists.size]]
                    break
                }
            }

            Xmp.stopModule()
            cmd = CMD_PREV
            if (isPlayerPaused) {
                discardBuffer = true
            }

            playCurrentStream()
        }

        override fun onStop() {
            super.onStop()
            println("OnStop")

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
            onPlay()
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            println("Action: $action")

            if (action == "loop") {
                isLoopEnabled = !isLoopEnabled
                println("loop $isLoopEnabled")
            }

            if (action == "shuffle") {
                isShuffleEnabled = !isShuffleEnabled
                println("shuffle $isShuffleEnabled")
            }

            //We should update our action buttons
            playbackState()

            super.onCustomAction(action, extras)
        }
    }

    private fun requestAudioFocus(): Boolean {
        val request: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager!!.requestAudioFocus(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setAudioAttributes(
                                    AudioAttributes
                                            .Builder()
                                            .setLegacyStreamType(STREAM_MUSIC)
                                            .build())
                            .setOnAudioFocusChangeListener(this@AutoService)
                            .build())
        } else {
            @Suppress("DEPRECATION")
            audioManager!!.requestAudioFocus(this@AutoService, STREAM_MUSIC, AUDIOFOCUS_GAIN)
        }

        hasFocus = true
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

    //TODO: Could be better, but a playstate is always called first before a null update
    private var currentPlaybackState: Int? = null
    private fun playbackState(state: Int? = null) {
        if (state != null)
            currentPlaybackState = state

        // TODO Need a Shuffle-Off icon, colors don't work
        session!!.setPlaybackState(
                Builder()
                        .setActions(ACTION_PLAY or
                                ACTION_PAUSE or
                                ACTION_SKIP_TO_NEXT or
                                ACTION_SKIP_TO_PREVIOUS or
                                ACTION_SEEK_TO
                        )
                        .addCustomAction("loop", "Loop", if (isLoopEnabled) R.drawable.ic_repeat_on else R.drawable.ic_repeat_off)
                        .addCustomAction("shuffle", "Shuffle", if (isShuffleEnabled) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off)
                        .setState(currentPlaybackState!!, Xmp.time().toLong(), 1f, SystemClock.elapsedRealtime())
                        .build()
        )
    }

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
                    continue
                }

                // Set default pan before we load the module
                val defpan = PrefManager.defaultPan
                Log.i(TAG, "Set default pan to $defpan")
                Xmp.setPlayer(Xmp.PLAYER_DEFPAN, defpan)

                // Ditto if we can't load the module
                Log.i(TAG, "Load " + playerFileName!!)

                Xmp.loadModule(playerFileName!!)

                cmd = CMD_NONE

                isLoaded = true

                // Unmute all channels
                for (i in 0..63) {
                    Xmp.mute(i, 0)
                }

                val interpTypes = intArrayOf(INTERP_NEAREST, INTERP_LINEAR, INTERP_SPLINE)
                val temp = PrefManager.interpType.toInt()

                var interpType: Int
                interpType = if (temp in 1..2) {
                    interpTypes[temp]
                } else {
                    INTERP_LINEAR
                }

                if (!PrefManager.interpolate) {
                    interpType = INTERP_NEAREST
                }

                Xmp.startPlayer(sampleRate)

                val volBoost = PrefManager.volumeBoost
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

                val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_foreground)
                val metaData = MediaMetadataCompat.Builder().apply {
                    putString(METADATA_KEY_TITLE, Xmp.getModName())
                    putString(METADATA_KEY_ARTIST, Xmp.getModType())
                    putLong(METADATA_KEY_DURATION, vars[0].toLong())
                    putBitmap(METADATA_KEY_ALBUM_ART, bitmap)

                }.build()
                session!!.setMetadata(metaData)

                // TODO: Handle loading next song in Queue

                // TODO: Handle updating controls and stuff
                // Everything appears to be OKAY, set the playback state.
                playbackState(STATE_PLAYING)

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
                        if (Xmp.fillBuffer(false) < 0) {
                            break
                        }

                        watchdog.refresh()
                    }

                    playNewSequence = false


                } while (playNewSequence)

                Xmp.endPlayer()

                isLoaded = false

                Log.i(TAG, "Release module")
                Xmp.releaseModule()

            } while (cmd != CMD_STOP)
        }
    }

    companion object {
        private val TAG = AutoService::class.java.simpleName

        const val ROOT = "root"
        //const val TYPE_PLAYLISTS = "Playlists"

        private const val MIN_BUFFER_MS = 80
        private const val MAX_BUFFER_MS = 1000

        private const val CMD_NONE = 0
        private const val CMD_NEXT = 1
        private const val CMD_PREV = 2
        private const val CMD_STOP = 3

        private const val DUCK_VOLUME = 0x500
    }
}
