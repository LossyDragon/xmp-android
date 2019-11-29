package org.helllabs.android.xmp.service.receiver

import android.content.Context
import android.content.Intent
import android.os.Build
import org.helllabs.android.xmp.service.PlayerService

// Since we have multiple broadcasts receivers, why not make the sendBroadcast an extension.
fun Context.sendBroadcast(action: String) {
    val intent = Intent(this, PlayerService::class.java)
    intent.action = action

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        this.startForegroundService(intent)
    else
        this.startService(intent)
}
