package org.helllabs.android.xmp.service

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
