package org.helllabs.android.xmp.service

import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import org.helllabs.android.xmp.model.Module

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