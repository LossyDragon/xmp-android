package org.helllabs.android.xmp.service

import android.content.*
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.helllabs.xmp.Xmp
import timber.log.Timber

@Singleton
class ServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val AUDIO_NOISY_INTENT_FILTER =
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    }

    private val _connectionEvent = MutableSharedFlow<Resource<Boolean>>()
    val connectionEvent = _connectionEvent.asSharedFlow()

    private val _currentSong = MutableStateFlow<MediaMetadataCompat?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _playbackState = MutableStateFlow<PlaybackStateCompat?>(null)
    val playbackState = _playbackState.asStateFlow()

    private val connectionCallback = ConnectionCallback()

    private lateinit var mediaController: MediaControllerCompat

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var audioFocusHelper: AudioFocusHelper = AudioFocusHelper()

    private var audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var noisyReceiverRegistered = false

    private var playOnAudioFocus = false

    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(context, PlayerService::class.java),
        connectionCallback,
        null
    ).apply {
        connect()
    }

    private val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    private val audioNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                val state = playbackState.value!!
                val isPlaying = state.isPlaying || state.isPlayEnabled
                if (isPlaying) {
                    pause()
                }
            }
        }
    }

    private fun registerNoisyReceiver() {
        if (!noisyReceiverRegistered) {
            context.registerReceiver(audioNoisyReceiver, AUDIO_NOISY_INTENT_FILTER)
            noisyReceiverRegistered = true
        }
    }

    private fun unregisterNoisyReceiver() {
        if (noisyReceiverRegistered) {
            context.unregisterReceiver(audioNoisyReceiver)
            noisyReceiverRegistered = false
        }
    }

    fun subscribe(parentId: String, callbacks: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callbacks)
    }

    fun unsubscribe(parentId: String) {
        mediaBrowser.unsubscribe(parentId)
    }

    fun seekTo(pos: Long) = transportControls.seekTo(pos)

    fun play() {
        if (audioFocusHelper.requestFocus()) {
            registerNoisyReceiver()
            transportControls.play()
        }
    }

    fun pause() {
        if (!playOnAudioFocus) {
            audioFocusHelper.abandonFocus()
        }

        unregisterNoisyReceiver()
        transportControls.pause()
    }

    fun stopPlaying() {
        audioFocusHelper.abandonFocus()
        unregisterNoisyReceiver()
        transportControls.stop()
    }

    fun skipToNextTrack() = transportControls.skipToNext()

    fun skipToPrev() = transportControls.skipToPrevious()

    fun playFromMediaId(mediaId: String, extras: Bundle? = null) =
        transportControls.playFromMediaId(mediaId, extras)

    fun prepare() = transportControls.prepare()

    private inner class ConnectionCallback : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            Timber.d("onConnected")
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            coroutineScope.launch {
                _connectionEvent.emit(Resource.Success(true))
            }
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            coroutineScope.launch {
                _connectionEvent.emit(Resource.Error(message = "connection suspended"))
            }
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            coroutineScope.launch {
                _connectionEvent.emit(Resource.Error(message = "Failed to connect"))
            }
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            connectionCallback.onConnectionSuspended()
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            Timber.d("onSessionEvent: $event, $extras")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            coroutineScope.launch {
                _playbackState.emit(state)
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            coroutineScope.launch {
                _currentSong.emit(metadata)
            }
        }
    }

    private inner class AudioFocusHelper : AudioManager.OnAudioFocusChangeListener {
        val state: PlaybackStateCompat? = playbackState.value
        val isPlaying = state?.isPlaying ?: false || state?.isPlayEnabled ?: false

        fun requestFocus(): Boolean {
            val result = audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )

            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        fun abandonFocus() {
            audioManager.abandonAudioFocus(this)
        }

        override fun onAudioFocusChange(focusChange: Int) {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (playOnAudioFocus && !isPlaying) {
                        play()
                    } else if (isPlaying) {
                        Xmp.setVolume(Xmp.UNDUCK_VOLUME)
                    }

                    playOnAudioFocus = false
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                    Xmp.setVolume(Xmp.DUCK_VOLUME)
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
                    if (isPlaying) {
                        playOnAudioFocus = true
                        pause()
                    }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    audioManager.abandonAudioFocus(this)
                    playOnAudioFocus = false
                    stopPlaying()
                }
            }
        }
    }
}
