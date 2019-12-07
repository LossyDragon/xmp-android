package org.helllabs.android.xmp.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.view.KeyEvent
import android.view.KeyEvent.*
import org.helllabs.android.xmp.service.PlayerService

class RemoteControlReceiver : BroadcastReceiver() {
    private var context: Context? = null
    private var hookCount = 0
    private val hookHandler = Handler()
    private val hookRunnable = Runnable {
        when (hookCount) {
            1 -> context!!.sendBroadcast(PlayerService.XMP_PLAYER_PLAY_PAUSE)
            2 -> context!!.sendBroadcast(PlayerService.XMP_PLAYER_NEXT)
            else -> context!!.sendBroadcast(PlayerService.XMP_PLAYER_PREV)
        }
        hookCount = 0
    }

    override fun onReceive(context: Context, intent: Intent) {
        this.context = context
        if (intent.action == Intent.ACTION_MEDIA_BUTTON) {
            val event = intent.extras!!.get(Intent.EXTRA_KEY_EVENT) as KeyEvent
            if (event.action == ACTION_UP) {
                when (event.keyCode) {
                    KEYCODE_MEDIA_PLAY_PAUSE,
                    KEYCODE_MEDIA_PLAY,
                    KEYCODE_MEDIA_PAUSE ->
                        context.sendBroadcast(PlayerService.XMP_PLAYER_PLAY_PAUSE)
                    KEYCODE_MEDIA_NEXT ->
                        context.sendBroadcast(PlayerService.XMP_PLAYER_NEXT)
                    KEYCODE_MEDIA_PREVIOUS ->
                        context.sendBroadcast(PlayerService.XMP_PLAYER_PREV)
                    KEYCODE_HEADSETHOOK -> {
                        hookCount++
                        hookHandler.removeCallbacks(hookRunnable)
                        if (hookCount >= 3)
                            hookHandler.post(hookRunnable)
                        else
                            hookHandler.postDelayed(hookRunnable, 500)
                    }
                }
            }
        }
    }
}
