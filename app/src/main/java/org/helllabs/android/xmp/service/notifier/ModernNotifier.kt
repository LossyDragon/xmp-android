package org.helllabs.android.xmp.service.notifier

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.os.Build
import androidx.core.app.NotificationCompat
import org.helllabs.android.xmp.R
import androidx.media.app.NotificationCompat as NotiCompatMedia

class ModernNotifier(service: Service) : Notifier(service) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(service)
        }
    }

    @TargetApi(26)
    private fun createNotificationChannel(service: Service) {
        val channel = NotificationChannel(CHANNEL_ID,
                service.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_LOW)
        channel.description = service.getString(R.string.notif_channel_desc)
        val notificationManager = service.getSystemService(NotificationManager::class.java)
        notificationManager!!.createNotificationChannel(channel)
    }

    @TargetApi(26)
    override fun notify(title: String, info: String, index: Int, type: Int) {
        var notifyTitle = title
        var notifyInfo = info

        if (notifyTitle.trim { it <= ' ' }.isEmpty()) {
            notifyTitle = "<untitled>"
        }

        val indexText = formatIndex(index)

        if (type == TYPE_PAUSE) {
            notifyInfo = "(paused)"
        }

        val builder = NotificationCompat.Builder(service, CHANNEL_ID)
                .setContentTitle(notifyTitle)
                .setContentText(notifyInfo)
                .setContentInfo(indexText)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setLargeIcon(icon)
                .setOngoing(true)
                .setShowWhen(true)
                .setWhen(0)
                .setChannelId(CHANNEL_ID)
                .setStyle(NotiCompatMedia.MediaStyle().setShowActionsInCompactView(1, 2, 3))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        //Action Builders
        builder.addAction(R.drawable.ic_previous, "Prev", prevIntent)       // 0
        builder.addAction(R.drawable.ic_stop, "Stop", stopIntent)           // 1
        if (type == TYPE_PAUSE) {
            builder.addAction(R.drawable.ic_play, "Play", playIntent)       // 2a
            builder.setContentText("(paused)")
        } else {
            builder.addAction(R.drawable.ic_pause, "Pause", pauseIntent)    // 2b
        }
        builder.addAction(R.drawable.ic_forward, "Next", nextIntent)        // 3

        if (type == TYPE_TICKER) {
            if (queueManager.size() > 1) {
                builder.setTicker("$notifyTitle ($indexText)")
            } else {
                builder.setTicker(notifyTitle)
            }
        }

        service.startForeground(NOTIFY_ID, builder.build())
    }

    companion object {
        private const val CHANNEL_ID = "xmp"
    }
}
