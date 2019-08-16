package org.helllabs.android.xmp.service.notifier

import org.helllabs.android.xmp.R

import android.annotation.TargetApi
import android.app.Notification
import android.app.Service

//import android.media.session.MediaSession;

@TargetApi(21)
class LollipopNotifier
//private final MediaSession.Token token;

(service: Service /*, final MediaSession.Token token*/)//this.token = token;
    : Notifier(service) {

    @TargetApi(21)
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

        service.startForeground(Notifier.NOTIFY_ID, builder.build())
    }

}
