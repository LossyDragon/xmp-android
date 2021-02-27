package org.helllabs.android.xmp.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.support.v4.media.session.MediaControllerCompat.TransportControls

/**
 * BecomingNoisyReceiver BroadcastReceiver
 */
class NoisyReceiver(private val controller: TransportControls) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.action.let {
            when (it) {
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> controller.pause()
            }
        }
    }
}
