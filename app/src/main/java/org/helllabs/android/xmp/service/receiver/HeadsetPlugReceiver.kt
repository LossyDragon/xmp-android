package org.helllabs.android.xmp.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import org.helllabs.android.xmp.service.PlayerService


class HeadsetPlugReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Stop the initial broadcast, it pauses on play
        if (!isInitialStickyBroadcast) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                context.sendBroadcast(PlayerService.XMP_PLAYER_PAUSE)
            } else if (intent.action == Intent.ACTION_HEADSET_PLUG) {
                when (intent.getIntExtra("state", -1)) {
                    0 -> context.sendBroadcast(PlayerService.XMP_PLAYER_PAUSE)
                    1 -> context.sendBroadcast(PlayerService.XMP_PLAYER_PLAY)
                }
            }
        }
    }

//    companion object {
//        private val TAG = HeadsetPlugReceiver::class.java.simpleName
//    }
}
