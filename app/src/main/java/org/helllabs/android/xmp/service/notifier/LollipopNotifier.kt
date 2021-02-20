package org.helllabs.android.xmp.service.notifier

import android.annotation.TargetApi
import android.app.Notification
import android.app.Notification.MediaStyle
import android.app.Service
import org.helllabs.android.xmp.R

class LollipopNotifier(service: Service) : Notifier(service) {

    @TargetApi(21)
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
        val builder = Notification.Builder(service)
            .setContentTitle(notifyTitle)
            .setContentText(notifyInfo)
            .setContentInfo(indexText)
            .setContentIntent(contentIntent)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(icon)
            .setOngoing(true)
            .setWhen(0)
            .setStyle(MediaStyle().setShowActionsInCompactView(2))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
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
            if (queueManager!!.size() > 1) {
                builder.setTicker("$notifyTitle ($indexText)")
            } else {
                builder.setTicker(notifyTitle)
            }
        }

        service.startForeground(NOTIFY_ID, builder.build())
    }
}
