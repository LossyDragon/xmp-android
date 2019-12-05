package org.helllabs.android.xmp.service.utils

import android.annotation.TargetApi
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

// https://developer.android.com/guide/topics/media-apps/audio-focus#audio-focus-8-0

@TargetApi(Build.VERSION_CODES.O)
class OreoAudioFocusHandler(context: Context) {
    private var audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequest

    fun abandonAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }

    fun requestAudioFocus(audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener): Int {
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
            setOnAudioFocusChangeListener(audioFocusChangeListener)
            setAudioAttributes(AudioAttributes.Builder().run {
                setUsage(AudioAttributes.USAGE_MEDIA)
                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                build()
            })
        }.build()

        return audioManager.requestAudioFocus(audioFocusRequest)
    }
}
