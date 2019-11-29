package org.helllabs.android.xmp.service.notifier

import android.app.Service
import androidx.core.app.NotificationCompat
import org.helllabs.android.xmp.R

class LegacyNotifier(service: Service) : Notifier(service) {

    private val sinceWhen: Long = System.currentTimeMillis()

    override fun notify(title: String, info: String, index: Int, type: Int) {
        var notifyTitle = title

        if (notifyTitle.trim { it <= ' ' }.isEmpty()) {
            notifyTitle = "<" + service.getString(R.string.notif_untitled) + ">"
        }

        val indexText = formatIndex(index)

        val builder = NotificationCompat.Builder(service, NOTIFY_ID.toString())
                .setContentTitle(notifyTitle)
                .setContentText(info)
                .setContentInfo(indexText)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(icon)
                .setOngoing(true)
                .setWhen(sinceWhen)
                .addAction(R.drawable.ic_stop, service.getString(R.string.notif_stop), stopIntent)

        if (type == TYPE_PAUSE) {
            builder.addAction(
                    R.drawable.ic_play, service.getString(R.string.notif_play), playIntent)
            builder.setContentText(service.getString(R.string.notif_paused))
        } else {
            builder.addAction(
                    R.drawable.ic_pause, service.getString(R.string.notif_pause), pauseIntent)
        }

        builder.addAction(R.drawable.ic_forward, service.getString(R.string.notif_next), nextIntent)

        if (type == TYPE_TICKER) {
            if (queueManager.size() > 1) {
                builder.setTicker("$notifyTitle ($indexText)")
            } else {
                builder.setTicker(notifyTitle)
            }
        }

        service.startForeground(NOTIFY_ID, builder.build())
    }
}
