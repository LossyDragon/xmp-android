package org.helllabs.android.xmp.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/*
 * From "Handling Screen OFF and Screen ON Intents" by jwei512
 * http://thinkandroid.wordpress.com/2010/01/24/handling-screen-off-and-screen-on-intents/
 */
class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            // DO WHATEVER YOU NEED TO DO HERE
            wasScreenOn = false
        } else if (intent.action == Intent.ACTION_SCREEN_ON) {
            // AND DO WHATEVER YOU NEED TO DO HERE
            wasScreenOn = true
        }
    }

    companion object {
        // THANKS JASON
        var wasScreenOn = true
    }
}
