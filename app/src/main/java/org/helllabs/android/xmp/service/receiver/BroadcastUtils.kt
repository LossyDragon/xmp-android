package org.helllabs.android.xmp.service.receiver

import android.content.Context
import android.content.Intent
import org.helllabs.android.xmp.extension.isAtLeastO
import org.helllabs.android.xmp.service.PlayerService

// Since we have multiple broadcasts receivers, why not make the sendBroadcast an extension.
fun Context.sendBroadcast(action: String) {
    Intent(this, PlayerService::class.java).run {
        this.action = action

        if (isAtLeastO())
            startForegroundService(this)
        else
            startService(this)
    }
}
