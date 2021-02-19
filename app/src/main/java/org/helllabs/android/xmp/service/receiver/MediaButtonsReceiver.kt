package org.helllabs.android.xmp.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import org.helllabs.android.xmp.util.Log.i

open class MediaButtonsReceiver : BroadcastReceiver() {

    protected var ordered = true

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        i(TAG, "Action $action")
        if (action == Intent.ACTION_MEDIA_BUTTON) {
            val event = intent.extras!![Intent.EXTRA_KEY_EVENT] as KeyEvent?
            if (event!!.action != KeyEvent.ACTION_DOWN) {
                return
            }
            var code: Int
            when (event.keyCode.also { code = it }) {
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                KeyEvent.KEYCODE_MEDIA_STOP,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    i(TAG, "Key code $code")
                    keyCode = code
                }
                else ->
                    i(TAG, "Unhandled key code $code")
            }
            if (ordered) {
                abortBroadcast()
            }
        }
    }

    companion object {
        private const val TAG = "MediaButtonsReceiver"
        const val NO_KEY = -1
        private var keyCode = NO_KEY

        fun getKeyCode(): Int = keyCode

        fun setKeyCode(keyCode: Int) {
            Companion.keyCode = keyCode
        }
    }
}
