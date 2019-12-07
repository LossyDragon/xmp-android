package org.helllabs.android.xmp.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.Intent.ACTION_SCREEN_ON

/*
 * From "Handling Screen OFF and Screen ON Intents" by Jason Wei (jwei512)
 * http://thinkandroid.wordpress.com/2010/01/24/handling-screen-off-and-screen-on-intents/
 */

class ScreenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SCREEN_OFF -> wasScreenOn = false
            ACTION_SCREEN_ON -> wasScreenOn = true
        }
    }

    companion object {
        internal var wasScreenOn = true
    }
}
