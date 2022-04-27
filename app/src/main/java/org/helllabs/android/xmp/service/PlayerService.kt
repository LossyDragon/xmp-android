package org.helllabs.android.xmp.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import okio.buffer
import okio.source
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

    var isRunning = false
    private var isPlayerInitialized = false
    private val currentSong = MutableStateFlow<MediaMetadataCompat?>(null)

    private lateinit var mediaSession: MediaSessionCompat

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

    override fun onCreate() {
        super.onCreate()

        val intent = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        mediaSession = MediaSessionCompat(this, MEDIA_SESSION).apply {
            setSessionActivity(intent)
            isActive = true
        }

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
                if (parentId.contains("explorer::")) {
                    val currentDir = parentId.substring(
                        parentId.indexOf("{") + 1, parentId.indexOf("}")
                    )
                }
            }
        }

        if (!resultSend)
            result.detach()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // TODO stop and cleanup xmp
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO stop and cleanup xmp
        coroutineScope.cancel()
    }
}
