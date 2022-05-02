package org.helllabs.android.xmp.service

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import okio.buffer
import okio.source
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.xmp.Watchdog
import org.helllabs.xmp.Xmp
import timber.log.Timber
import java.io.File
import javax.inject.Inject

const val MEDIA_ROOT_ID = "root_id"
const val PLAYLIST_ROOT_ID = "playlist_id"
const val EXPLORER_ROOT_ID = "explorer_id"

private const val MEDIA_SESSION = "XmpModPlayerSession"

@AndroidEntryPoint
class PlayerService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var playlistDir: File

    @Inject
    lateinit var moshiAdapter: JsonAdapter<Playlist>

    private val serviceJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val currentSong = MutableStateFlow<MediaMetadataCompat?>(null)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaCallback: MediaSessionCallback
    private lateinit var notificationManager: NotificationManager

    private var modPlayer: XmpPlayer = XmpPlayer()

    private val isAtLeastM: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    override fun onCreate() {
        super.onCreate()

        val flags = if (isAtLeastM) FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT else FLAG_UPDATE_CURRENT
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, flags)
        }

        mediaCallback = MediaSessionCallback()
        notificationManager = NotificationManager(this)
        mediaSession = MediaSessionCompat(this, MEDIA_SESSION)

        mediaSession.setSessionActivity(intent)
        mediaSession.setCallback(mediaCallback)

        @Suppress("DEPRECATION")
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        mediaSession.isActive = true

        sessionToken = mediaSession.sessionToken
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        val resultSend = false
        when (parentId) {
            MEDIA_ROOT_ID -> {
                val list = getRootChildren()
                result.sendResult(list.toMutableList())
            }
            PLAYLIST_ROOT_ID -> {
                Timber.d("Playlist Children")
                coroutineScope.launch {
                    val list = getPlaylistChildren()
                    result.sendResult(list.toMutableList())
                }
            }
            else -> {
                // Most likely trying to browse a selected playlist.
                if (parentId.contains("playlist::")) {
                    coroutineScope.launch {
                        val list = getSelectedPlaylistChildren(parentId)
                        result.sendResult(list.toMutableList())
                    }
                }
                // Most likely trying to browse via explorer
                // if (parentId.contains("explorer::")) {
                //     val currentDir = parentId.substring(
                //         parentId.indexOf("{") + 1, parentId.indexOf("}")
                //     )
                // }
            }
        }

        if (!resultSend)
            result.detach()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        // notificationManager.onDestroy()
        mediaCallback.onStop()
        mediaSession.release()

        coroutineScope.cancel()
    }

    // Separate function??
    private fun getPlaylistChildren(): List<MediaBrowserCompat.MediaItem> {
        val list = playlistDir.list().orEmpty()
        list.sortBy { it.lowercase() }

        return list.map {
            val buffer = File(playlistDir, it).source().buffer()
            val contents = moshiAdapter.fromJson(buffer)!!
            buffer.close()

            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder().apply {
                    setMediaId(contents.name)
                    setTitle(contents.name)
                    setDescription(contents.comment)
                }.build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            )
        }
    }

    // Separate function?
    private fun getSelectedPlaylistChildren(parentId: String): List<MediaBrowserCompat.MediaItem> {
        val selectedPlaylist = parentId.substringAfter("::")

        val buffer = File(playlistDir, "$selectedPlaylist.json").source().buffer()
        val contents = moshiAdapter.fromJson(buffer)!!
        buffer.close()

        return contents.data.sortedBy { it.id }.map { data ->
            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder().apply {
                    setMediaId(data.name)
                    setTitle(data.name)
                    setDescription(data.type)
                    setMediaUri(data.uriPath.toUri())
                }.build(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }
    }

    // Separate function?
    private fun getRootChildren(): List<MediaBrowserCompat.MediaItem> {
        return listOf(
            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder().apply {
                    setMediaId(EXPLORER_ROOT_ID)
                    setTitle("Module Explorer")
                    setDescription("Browse modules on the device")
                }.build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ),
            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder().apply {
                    setMediaId(PLAYLIST_ROOT_ID)
                    setTitle("Playlists")
                    setDescription("View saved playlists from your device")
                }.build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            )
        )
    }

    inner class MediaSessionCallback : MediaSessionCompat.Callback() {

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            Timber.d("onMediaButtonEvent($mediaButtonEvent)")
            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        override fun onAddQueueItem(description: MediaDescriptionCompat?) {
            super.onAddQueueItem(description)
            Timber.d("onAddQueueItem($description)")
        }

        override fun onRemoveQueueItem(description: MediaDescriptionCompat?) {
            super.onRemoveQueueItem(description)
            Timber.d("onRemoveQueueItem($description)")
        }

        override fun onPrepare() {
            Timber.d("onPrepare()")

            val isPrepared = modPlayer.onPrepare()
            if (!isPrepared) {
                Timber.e("ModPlayer failed to prepare and is not ready to play!")
                return
            }

            super.onPrepare()
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            Timber.d("onPlayFromMediaId($mediaId) (${extras.toString()})")

            if (!modPlayer.isPrepared) {
                Timber.e("ModPlayer is not prepare to play!")
                return
            }

            super.onPlayFromMediaId(mediaId, extras)
        }

        override fun onPlay() {
            Timber.d("onPlay()")

            if (!modPlayer.isPrepared) {
                Timber.e("ModPlayer is not prepare to play!")
                return
            }

            super.onPlay()
        }

        override fun onPause() {
            Timber.d("onPause()")
            super.onPause()
        }

        override fun onStop() {
            Timber.d("onStop()")

            modPlayer.onStop()

            super.onStop()
        }


        override fun onSkipToNext() {
            Timber.d("onSkipToNext()")
            super.onSkipToNext()
        }

        override fun onSkipToPrevious() {
            Timber.d("onSkipToPrevious()")
            super.onSkipToPrevious()
        }

        override fun onSeekTo(pos: Long) {
            Timber.d("onSeekTo($pos)")
            super.onSeekTo(pos)
        }

        // @PlaybackStateCompat.RepeatMode
        override fun onSetRepeatMode(repeatMode: Int) {
            Timber.d("onSetRepeatMode($repeatMode)")
            super.onSetRepeatMode(repeatMode)
        }

        // @PlaybackStateCompat.ShuffleMode
        override fun onSetShuffleMode(shuffleMode: Int) {
            Timber.d("onSetShuffleMode($shuffleMode)")
            super.onSetShuffleMode(shuffleMode)
        }
    }

    enum class XmpState {
        NONE, NEXT, PREV, STOP
    }

    enum class XmpResult {
        OK, CANT_OPEN, NO_FOCUS, WATCHDOG
    }

    // TODO:  Move this to libxmp module when working
    private inner class XmpPlayer : Watchdog.OnTimeoutListener {
        /* Constants */
        private val MIN_BUFFER_MS = 80
        private val MAX_BUFFER_MS = 1000

        /* Variables */
        var isPrepared = false
            private set

        private val vars = IntArray(8)
        private var isPaused = false
        private var playerState = XmpState.NONE
        private var watchdog: Watchdog? = null


        override fun onTimeout() {
            Timber.w("Watchdog called. Stopping player.")
            onStop()
            stopSelf()
        }

        fun onPrepare(): Boolean {
            var isPrepared = false

            var bufferMs = 400 // TODO: preference
            if (bufferMs < MIN_BUFFER_MS) bufferMs = MIN_BUFFER_MS
            if (bufferMs > MAX_BUFFER_MS) bufferMs = MAX_BUFFER_MS

            sampleRate = 44100 // TODO: preference
            playerAllSequences = false // TODO: preference

            val modPlayerInit = Xmp.init(sampleRate, bufferMs)
            if (modPlayerInit) {
                isPrepared = true

                watchdog = Watchdog(10)
                watchdog!!.listener = this
            }

            if (!modPlayerInit) {
                Timber.e("error initializing audio")
            }

            return isPrepared
        }

        fun onPlay() {
            isPaused = false
        }

        fun onPause() {
            isPaused = true
            Xmp.stopAudio()
        }

        fun onStop() {
            playerState = XmpState.STOP
        }

        fun onPrevious() {
            if (Xmp.time() > 2000) {
                Xmp.seek(0)
            } else {
                discardBuffer = true
                playerState = XmpState.PREV
            }

            // TODO play if paused
        }

        fun onForward() {
            discardBuffer = true
            playerState = XmpState.NEXT

            // TODO play if paused
        }

        fun onSeek(pos: Long) {
            Xmp.seek(pos.toInt())
        }


        /* OLD */
        private var discardBuffer = false
        private var isLooped = false
        private var playerAllSequences = false
        private var restart = false
        private var sampleRate = 0
        private var sequenceNumber = 0

        val thread = coroutineScope.launch(Dispatchers.Main, CoroutineStart.LAZY) {
            watchdog!!.start()

            do {
                val defpan = 50 // TODO: preference
                Timber.i("Set default pan to $defpan")
                Xmp.setPlayer(Xmp.PLAYER_DEFPAN, defpan)

                Timber.i("Load $filePath")
                if (Xmp.loadModule(filePath) < 0) {
                    Timber.w("Couldn't load module")
                    // TODO: Try next in queue
                }

                val temp = 1 // TODO: preference
                var interpType = when (temp) {
                    1 -> Xmp.INTERP_NEAREST
                    2 -> Xmp.INTERP_LINEAR
                    3 -> Xmp.INTERP_SPLINE
                    else -> Xmp.INTERP_LINEAR
                }

                val interpolate = false  // TODO: preference
                if (!interpolate) {
                    interpType = Xmp.INTERP_NEAREST
                }

                Xmp.startPlayer(sampleRate)

                // Unmute all channels
                for (i in 0..63) {
                    Xmp.mute(i, 0)
                }

                // Set player amplification
                val volumeBoost = 1 // TODO: preference
                Xmp.setPlayer(Xmp.PLAYER_AMP, volumeBoost)
                // Set player stereo mix
                val stereoMix = 100 // TODO: preference
                Xmp.setPlayer(Xmp.PLAYER_MIX, stereoMix)
                // Interpolation type
                Xmp.setPlayer(Xmp.PLAYER_INTERP, interpType)
                // DSP lowpass filter
                Xmp.setPlayer(Xmp.PLAYER_DSP, Xmp.DSP_LOWPASS)

                var flags = Xmp.getPlayer(Xmp.PLAYER_CFLAGS)
                val amigaMixer = false // TODO: preference
                flags = if (amigaMixer) {
                    flags or Xmp.FLAGS_A500
                } else {
                    flags and Xmp.FLAGS_A500.inv()
                }
                Xmp.setPlayer(Xmp.PLAYER_CFLAGS, flags)

                sequenceNumber = 0

                var playNewSequence: Boolean
                Xmp.setSequence(sequenceNumber)
                Xmp.playAudio()

                Timber.i("Enter play loop")
                do {
                    // seq_duration, length, pattern, channel, instruments, sample, num_sequences, _sequence
                    Xmp.getModVars(vars)

                    while (playerState == XmpState.NONE) {
                        discardBuffer = false

                        // Wait if paused
                        while (isPaused && playerState != XmpState.STOP) {
                            delay(100)
                            watchdog!!.refresh()
                        }

                        if (discardBuffer) {
                            Timber.d("discard buffer")
                            Xmp.stopModule()
                            Xmp.dropAudio()
                            break
                        }

                        // Wait if no buffers available
                        while (!Xmp.hasFreeBuffer() && !isPaused && playerState == XmpState.NONE) {
                            delay(40)
                        }

                        // Fill a new buffer
                        if (Xmp.fillBuffer(isLooped) < 0) {
                            break
                        }

                        watchdog!!.refresh()
                    }

                    // Subsong explorer
                    // Do all this if we've exited normally and explorer is active
                    playNewSequence = false
                    if (playerAllSequences && playerState == XmpState.NONE) {
                        sequenceNumber++
                        Timber.i("Play sequence $sequenceNumber")
                        if (Xmp.setSequence(sequenceNumber)) {
                            playNewSequence = true
                        }
                    }
                } while (playNewSequence)

                Xmp.endPlayer()

                Timber.i("Release module")
                Xmp.releaseModule()

                // Used when current files are replaced by a new set
                if (restart) {
                    Timber.i("Restart")
                    playerState = XmpState.NONE
                    restart = false
                } else if (playerState == XmpState.PREV) {
                    // go to previous item in queue
                }

            } while (!isActive || playerState != XmpState.STOP)

            Xmp.deinit()
            watchdog!!.stop()
            Timber.i("Xmp Player stopped")
        }
    }
}
