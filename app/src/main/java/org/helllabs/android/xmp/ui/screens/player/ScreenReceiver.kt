package org.helllabs.android.xmp.ui.screens.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import timber.log.Timber

class ScreenReceiver(private val onScreenEvent: (Boolean) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Screen event was: ${intent.action}")
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> onScreenEvent(true)
            Intent.ACTION_SCREEN_OFF -> onScreenEvent(false)
        }
    }

    fun register(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(this, filter)
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(this)
    }
}
