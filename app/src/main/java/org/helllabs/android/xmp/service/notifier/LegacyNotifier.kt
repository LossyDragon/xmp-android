package org.helllabs.android.xmp.service.notifier

import org.helllabs.android.xmp.R

import android.app.Service
import androidx.core.app.NotificationCompat


class LegacyNotifier(service: Service) : Notifier(service) {

    private val `when`: Long

    init {
        `when` = System.currentTimeMillis()
    }

    override fun notify(title: String, info: String, index: Int, type: Int) {
        var title = title

        if (title != null && title.trim { it <= ' ' }.isEmpty()) {
            title = "<untitled>"
        }

        val indexText = formatIndex(index)

        val builder = NotificationCompat.Builder(service)
                .setContentTitle(title)
                .setContentText(info)
                .setContentInfo(indexText)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(icon)
                .setOngoing(true)
                .setWhen(`when`)
                .addAction(R.drawable.ic_action_stop, "Stop", stopIntent)

        if (type == Notifier.TYPE_PAUSE) {
            builder.addAction(R.drawable.ic_action_play, "Play", pauseIntent)
            builder.setContentText("(paused)")
        } else {
            builder.addAction(R.drawable.ic_action_pause, "Pause", pauseIntent)
        }

        builder.addAction(R.drawable.ic_action_next, "Next", nextIntent)

        if (type == Notifier.TYPE_TICKER) {
            if (queueManager.size() > 1) {
                builder.setTicker("$title ($indexText)")
            } else {
                builder.setTicker(title)
            }
        }

        service.startForeground(Notifier.NOTIFY_ID, builder.build())
    }

}
