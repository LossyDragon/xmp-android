package org.helllabs.android.xmp.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import org.helllabs.android.xmp.service.Notifier

/**
 * Notification BroadcastReceiver
 */
class ControllerReceiver(private val mediaSession: MediaSessionCompat) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.action.let {
            with(mediaSession.controller.transportControls) {
                when (it) {
                    Notifier.ACTION_PLAY -> play()
                    Notifier.ACTION_PAUSE -> pause()
                    Notifier.ACTION_STOP -> stop()
                    Notifier.ACTION_PREV -> skipToPrevious()
                    Notifier.ACTION_NEXT -> skipToNext()
                }
            }
        }
    }
}
