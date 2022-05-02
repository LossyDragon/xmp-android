package org.helllabs.android.xmp.service

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Singleton

@Singleton
class PlayerUseCase @Inject constructor(private val serviceConnection: ServiceConnection) {

    val currentSong = serviceConnection.currentSong
    val playbackState = serviceConnection.playbackState

    val timePassed = flow {
        while (true) {
            val duration = playbackState.value?.currentPlaybackPosition ?: 0
            emit(duration)
            delay(1000L)
        }
    }

    suspend fun subscribeToService(parentId: String): Resource<List<MediaBrowserCompat.MediaItem>> =
        suspendCoroutine {
            serviceConnection.subscribe(
                parentId,
                object : MediaBrowserCompat.SubscriptionCallback() {
                    override fun onChildrenLoaded(
                        parentId: String,
                        children: MutableList<MediaBrowserCompat.MediaItem>
                    ) {
                        super.onChildrenLoaded(parentId, children)
                        Timber.d("onChildrenLoaded($children)")
                        it.resume(Resource.Success(children))
                    }

                    override fun onError(parentId: String) {
                        super.onError(parentId)
                        Timber.d("onError($parentId)")
                        it.resume(Resource.Error(message = "Failed to subscribe"))
                    }
                }
            )
        }

    fun unsubscribeToService(id: String? = null) {
        serviceConnection.unsubscribe(id ?: MEDIA_ROOT_ID)
    }

    fun skipToNextTrack() = serviceConnection.skipToNextTrack()

    fun skipToPrevTrack() = serviceConnection.skipToPrev()

    fun seekTo(pos: Long) = serviceConnection.seekTo(pos)

    fun stopPlaying() = serviceConnection.stopPlaying()

    fun playFromMediaId(mediaId: String, extras: Bundle? = null) =
        serviceConnection.playFromMediaId(mediaId, extras)

    fun prepare() = serviceConnection.prepare()

    fun isMusicPlayingOrPaused() = serviceConnection.playbackState.value?.let {
        return@let it.isPlaying || it.isPlayEnabled
    } ?: false

    fun playPause(musicId: String, toggle: Boolean = false) {
        val isPrepared = playbackState.value?.isPrepared ?: false
        if (isPrepared && musicId ==
            currentSong.value?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
        )
            playPauseCurrentSong(toggle)
        else playFromMediaId(musicId)
    }

    private fun playPauseCurrentSong(toggle: Boolean) {
        playbackState.value?.let {
            when {
                it.isPlaying -> if (toggle) serviceConnection.pause()
                it.isPlayEnabled -> serviceConnection.play()
                else -> Unit
            }
        }
    }
}
