package org.helllabs.android.xmp.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.support.v4.media.session.MediaSessionCompat

/**
 * BecomingNoisyReceiver BroadcastReceiver
 */
class NoisyReceiver(private val mediaSession: MediaSessionCompat) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.action.let {
            with(mediaSession.controller.transportControls) {
                when (it) {
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> pause()
                }
            }
        }
    }
}
