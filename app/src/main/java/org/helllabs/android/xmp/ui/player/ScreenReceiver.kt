package org.helllabs.android.xmp.ui.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/*
 * From "Handling Screen OFF and Screen ON Intents" by jwei512
 * http://thinkandroid.wordpress.com/2010/01/24/handling-screen-off-and-screen-on-intents/
 */
class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> wasScreenOn = false
            Intent.ACTION_SCREEN_ON -> wasScreenOn = true
        }
    }

    companion object {
        var wasScreenOn = true
    }
}
