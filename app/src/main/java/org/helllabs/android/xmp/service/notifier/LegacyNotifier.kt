package org.helllabs.android.xmp.service.notifier

import android.app.Service
import androidx.core.app.NotificationCompat
import org.helllabs.android.xmp.R

@Deprecated("min API 21")
class LegacyNotifier(service: Service) : Notifier(service) {

    private val `when`: Long = System.currentTimeMillis()

    override fun notify(title: String, info: String, index: Int, type: Int) {
        var notifyTitle = title

        if (notifyTitle.trim { it <= ' ' }.isEmpty()) {
            notifyTitle = "<untitled>"
        }

        val indexText = formatIndex(index)
        val builder = NotificationCompat.Builder(service)
            .setContentTitle(notifyTitle)
            .setContentText(info)
            .setContentInfo(indexText)
            .setContentIntent(contentIntent)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(icon)
            .setOngoing(true)
            .setWhen(`when`)
            .addAction(R.drawable.ic_stop, "Stop", stopIntent)

        if (type == TYPE_PAUSE) {
            builder.addAction(R.drawable.ic_play, "Play", pauseIntent)
            builder.setContentText("(paused)")
        } else {
            builder.addAction(R.drawable.ic_pause, "Pause", pauseIntent)
        }

        builder.addAction(R.drawable.ic_forward, "Next", nextIntent)

        if (type == TYPE_TICKER) {
            if (queueManager!!.size() > 1) {
                builder.setTicker("$notifyTitle ($indexText)")
            } else {
                builder.setTicker(notifyTitle)
            }
        }

        service.startForeground(NOTIFY_ID, builder.build())
    }
}
