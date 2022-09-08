package org.helllabs.android.xmp.service

import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.net.toUri
import com.squareup.moshi.JsonAdapter
import java.io.File
import okio.buffer
import okio.source
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.Playlist

enum class PlayingState {
    PLAYING, PAUSED, NONE
}

fun MediaMetadataCompat.getModule(): Module = Module(
    id = description.mediaId ?: "",
    title = description.title.toString(),
    type = description.description.toString(),
    duration = getLong(MediaMetadataCompat.METADATA_KEY_DURATION),
    pathUri = description.mediaUri.toString()
)

fun PlaybackStateCompat.getMusicState(): PlayingState = when {
    isPlaying -> PlayingState.PLAYING
    isPrepared -> PlayingState.PAUSED
    else -> PlayingState.NONE
}

inline val PlaybackStateCompat.isPrepared
    get() = state == PlaybackStateCompat.STATE_BUFFERING ||
        state == PlaybackStateCompat.STATE_PLAYING ||
        state == PlaybackStateCompat.STATE_PAUSED

inline val PlaybackStateCompat.isPlaying
    get() = state == PlaybackStateCompat.STATE_BUFFERING ||
        state == PlaybackStateCompat.STATE_PLAYING

inline val PlaybackStateCompat.isPlayEnabled
    get() = actions and PlaybackStateCompat.ACTION_PLAY != 0L ||
        (
            actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L &&
                state == PlaybackStateCompat.STATE_PAUSED
            )

inline val PlaybackStateCompat.currentPlaybackPosition: Long
    get() = if (state == PlaybackStateCompat.STATE_PLAYING) {
        val timeDelta = SystemClock.elapsedRealtime() - lastPositionUpdateTime
        (position + (timeDelta * playbackSpeed)).toLong()
    } else position

// Separate function??
fun getPlaylistChildren(
    playlistDir: File,
    moshiAdapter: JsonAdapter<Playlist>
): List<MediaBrowserCompat.MediaItem> {
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
fun getSelectedPlaylistChildren(
    parentId: String,
    playlistDir: File,
    moshiAdapter: JsonAdapter<Playlist>
): List<MediaBrowserCompat.MediaItem> {
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
fun getRootChildren(): List<MediaBrowserCompat.MediaItem> {
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
