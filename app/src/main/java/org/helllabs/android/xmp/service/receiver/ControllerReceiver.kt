package org.helllabs.android.xmp.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaControllerCompat.TransportControls
import org.helllabs.android.xmp.service.notifier.Notifier

/**
 * Notification BroadcastReceiver
 */
class ControllerReceiver(private val controller: TransportControls) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.action.let {
            when (it) {
                Notifier.ACTION_PLAY -> controller.play()
                Notifier.ACTION_PAUSE -> controller.pause()
                Notifier.ACTION_STOP -> controller.stop()
                Notifier.ACTION_PREV -> controller.skipToPrevious()
                Notifier.ACTION_NEXT -> controller.skipToNext()
            }
        }
    }
}
