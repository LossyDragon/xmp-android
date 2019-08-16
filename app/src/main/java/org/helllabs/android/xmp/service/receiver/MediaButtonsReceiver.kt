package org.helllabs.android.xmp.service.receiver

import org.helllabs.android.xmp.util.Log

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent


open class MediaButtonsReceiver : BroadcastReceiver() {
    protected var ordered = true

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Action " + action!!)
        if (action == Intent.ACTION_MEDIA_BUTTON) {
            val event = intent.extras!!.get(Intent.EXTRA_KEY_EVENT) as KeyEvent

            if (event.action != KeyEvent.ACTION_DOWN) {
                return
            }

            when (val code: Int = event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    Log.i(TAG, "Key code $code")
                    keyCode = code
                }
                else -> Log.i(TAG, "Unhandled key code $code")
            }

            if (ordered) {
                abortBroadcast()
            }
        }
    }

    companion object {
        private const val TAG = "MediaButtonsReceiver"
        const val NO_KEY = -1
        var keyCode = NO_KEY
    }
}