package org.helllabs.android.xmp.service.notifier

import org.helllabs.android.xmp.R

import android.annotation.TargetApi
import android.app.Service
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as NotiCompatMedia

@TargetApi(21)
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

        val builder = NotificationCompat.Builder(service, NOTIFY_ID.toString())
                .setContentTitle(notifyTitle)
                .setContentText(notifyInfo)
                .setContentInfo(indexText)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(icon)
                .setOngoing(true)
                .setWhen(0)
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

}
