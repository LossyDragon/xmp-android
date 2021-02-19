package org.helllabs.android.xmp.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.helllabs.android.xmp.service.notifier.Notifier
import org.helllabs.android.xmp.util.logI

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        logI("Action $action")
        when (action) {
            Notifier.ACTION_STOP -> keyCode = STOP
            Notifier.ACTION_PAUSE -> keyCode = PAUSE
            Notifier.ACTION_NEXT -> keyCode = NEXT
            Notifier.ACTION_PREV -> keyCode = PREV
        }
    }

    companion object {
        const val NO_KEY = -1
        const val STOP = 1
        const val PAUSE = 2
        const val NEXT = 3
        const val PREV = 4
        private var keyCode = NO_KEY

        fun getKeyCode(): Int = keyCode

        fun setKeyCode(keyCode: Int) {
            Companion.keyCode = keyCode
        }
    }
}
