package org.helllabs.android.xmp.service.notifier

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as NotiCompatMedia

import org.helllabs.android.xmp.R

@TargetApi(26)
class OreoNotifier(service: Service) : Notifier(service) {

    init {
        createNotificationChannel(service)
    }

    @TargetApi(26)
    private fun createNotificationChannel(service: Service) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID,
                    service.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_LOW)
            channel.description = service.getString(R.string.notif_channel_desc)
            val notificationManager = service.getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
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
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(icon)
                .setOngoing(true)
                .setWhen(0)
                .setChannelId(CHANNEL_ID)
                .setStyle(NotiCompatMedia.MediaStyle().setShowActionsInCompactView(2))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.ic_action_previous, "Prev", prevIntent)
                .addAction(R.drawable.ic_action_stop, "Stop", stopIntent)

        if (type == TYPE_PAUSE) {
            builder.addAction(R.drawable.ic_action_play, "Play", pauseIntent)
            builder.setContentText("(paused)")
        } else {
            builder.addAction(R.drawable.ic_action_pause, "Pause", pauseIntent)
        }

        builder.addAction(R.drawable.ic_action_next, "Next", nextIntent)

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
