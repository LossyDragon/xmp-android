package org.helllabs.android.xmp.service.notifier

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.os.Build

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
        var title = title
        var info = info

        if (title != null && title.trim { it <= ' ' }.isEmpty()) {
            title = "<untitled>"
        }

        val indexText = formatIndex(index)

        if (type == Notifier.TYPE_PAUSE) {
            info = "(paused)"
        }

        val builder = Notification.Builder(service)
                .setContentTitle(title)
                .setContentText(info)
                .setContentInfo(indexText)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(icon)
                .setOngoing(true)
                .setWhen(0)
                .setChannelId(CHANNEL_ID)
                .setStyle(Notification.MediaStyle().setShowActionsInCompactView(2))
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(R.drawable.ic_action_previous, "Prev", prevIntent)
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

        service.startForeground(NOTIFY_ID, builder.build())
    }

    companion object {

        private val CHANNEL_ID = "xmp"
    }


}
