package org.helllabs.android.xmp.service

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.helllabs.android.xmp.model.Playlist
import timber.log.Timber

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

    private val currentSongPath = MutableStateFlow("")

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
                    val list = getPlaylistChildren(playlistDir, moshiAdapter)
                    result.sendResult(list.toMutableList())
                }
            }
            else -> {
                // Most likely trying to browse a selected playlist.
                if (parentId.contains("playlist::")) {
                    coroutineScope.launch {
                        val list = getSelectedPlaylistChildren(parentId, playlistDir, moshiAdapter)
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

        mediaCallback.onStop()
        mediaSession.release()

        coroutineScope.cancel()
    }

    inner class MediaSessionCallback : MediaSessionCompat.Callback() {

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
            Timber.d("onPlayFromMediaId($mediaId) ($extras)")

            if (!modPlayer.isPrepared) {
                Timber.w("ModPlayer is not prepare to play")
                modPlayer.onPrepare()
            }

            modPlayer.onPlay()

            super.onPlayFromMediaId(mediaId, extras)
        }

        override fun onPlay() {
            Timber.d("onPlay()")

            if (!modPlayer.isPrepared) {
                Timber.w("ModPlayer is not prepared to play")
                modPlayer.onPrepare()
            }

            modPlayer.onPlay()

            super.onPlay()
        }

        override fun onPause() {
            Timber.d("onPause()")

            modPlayer.onPause()

            super.onPause()
        }

        override fun onStop() {
            Timber.d("onStop()")

            modPlayer.onStop()

            super.onStop()
        }

        override fun onSkipToNext() {
            Timber.d("onSkipToNext()")

            modPlayer.onForward()

            super.onSkipToNext()
        }

        override fun onSkipToPrevious() {
            Timber.d("onSkipToPrevious()")

            modPlayer.onPrevious()

            super.onSkipToPrevious()
        }

        override fun onSeekTo(pos: Long) {
            Timber.d("onSeekTo($pos)")

            modPlayer.onSeek(pos)

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
    }

    enum class XmpState {
        NONE, NEXT, PREV, STOP
    }

    // TODO:  Move this to libxmp module when working
    private inner class XmpPlayer {

        val isPrepared: Boolean = false

        fun onPrepare(): Boolean {
            return false
        }

        fun onPlay() {
        }

        fun onPause() {
        }

        fun onStop() {
        }

        fun onPrevious() {
        }

        fun onForward() {
        }

        fun onSeek(pos: Long) {
        }
    }
}
