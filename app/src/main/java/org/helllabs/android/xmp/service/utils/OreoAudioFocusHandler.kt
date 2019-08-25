package org.helllabs.android.xmp.service.utils

import android.annotation.TargetApi
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

@TargetApi(Build.VERSION_CODES.O)
class OreoAudioFocusHandler constructor(val context: Context) {
    private var audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequest

    fun abandonAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }

    fun requestAudioFocus(audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener) {
        val audioAttributes = AudioAttributes.Builder().apply {
            setUsage(AudioAttributes.USAGE_MEDIA)
            setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        }.build()

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).apply {
            setOnAudioFocusChangeListener(audioFocusChangeListener)
            setAudioAttributes(audioAttributes)
        }.build()

        audioManager.requestAudioFocus(audioFocusRequest)
    }
}