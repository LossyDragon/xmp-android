package org.helllabs.android.xmp.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import org.helllabs.android.xmp.service.PlayerService

// Pause the service when a headset is suddenly disconnected.
// Also combining HeadsetPlugReceiver into this
// We only want to pause on disconnect
class NoisyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!isInitialStickyBroadcast && intent.action == Intent.ACTION_HEADSET_PLUG) {
            if (intent.getIntExtra("state", -1) == 0)
                context.sendBroadcast(PlayerService.XMP_PLAYER_PLAY_PAUSE)
        } else if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            context.sendBroadcast(PlayerService.XMP_PLAYER_PLAY_PAUSE)
        }
    }
}
