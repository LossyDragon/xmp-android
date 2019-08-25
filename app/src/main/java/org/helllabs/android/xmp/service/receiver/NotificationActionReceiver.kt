package org.helllabs.android.xmp.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.helllabs.android.xmp.service.PlayerService

// Intents for the Action buttons on notification shade
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (val action = intent.action) {
            PlayerService.XMP_PLAYER_STOP,
            PlayerService.XMP_PLAYER_PREV,
            PlayerService.XMP_PLAYER_NEXT,
            PlayerService.XMP_PLAYER_PLAY,
            PlayerService.XMP_PLAYER_PAUSE -> context.sendBroadcast(action)
        }
    }

//    companion object {
//        private val TAG = NotificationActionReceiver::class.java.simpleName
//    }
}
