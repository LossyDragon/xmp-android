package org.helllabs.android.xmp.service.receiver

import org.helllabs.android.xmp.service.notifier.Notifier
import org.helllabs.android.xmp.util.Log

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Action " + action!!)

        when (action) {
            Notifier.ACTION_STOP -> keyCode = STOP
            Notifier.ACTION_PAUSE -> keyCode = PAUSE
            Notifier.ACTION_NEXT -> keyCode = NEXT
            Notifier.ACTION_PREV -> keyCode = PREV
        }
    }

    companion object {
        private const val TAG = "NotificationActionReceiver"
        const val NO_KEY = -1
        const val STOP = 1
        const val PAUSE = 2
        const val NEXT = 3
        const val PREV = 4
        var keyCode = NO_KEY
    }

}
