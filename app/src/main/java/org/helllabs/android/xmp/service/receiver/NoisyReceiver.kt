package org.helllabs.android.xmp.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import org.helllabs.android.xmp.service.PlayerService

// Pause the service when a headset is suddenly disconnected.
class NoisyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Stop the initial broadcast, it pauses on play
        if (!isInitialStickyBroadcast) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                context.sendBroadcast(PlayerService.XMP_PLAYER_PAUSE)
            }
        }
    }
}
