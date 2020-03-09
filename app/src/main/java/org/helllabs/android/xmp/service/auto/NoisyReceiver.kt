package org.helllabs.android.xmp.service.auto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat.Token

class NoisyReceiver(
        private val context: Context,
        sessionToken: Token
) : BroadcastReceiver() {

    private val noisyIntentFilter = IntentFilter(ACTION_AUDIO_BECOMING_NOISY)
    private val controller = MediaControllerCompat(context, sessionToken)

    private var isRegistered = false

    fun register() {
        if (!isRegistered) {
            context.registerReceiver(this, noisyIntentFilter)
            isRegistered = true
        }
    }

    fun unregister() {
        if (isRegistered) {
            context.unregisterReceiver(this)
            isRegistered = false
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_AUDIO_BECOMING_NOISY) {
            controller.transportControls.pause()
        }
    }
}
