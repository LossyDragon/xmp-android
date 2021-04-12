package org.helllabs.android.xmp.service

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import java.util.*
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.presentation.ui.player.PlayerActivity
import org.helllabs.android.xmp.service.utils.QueueManager
import org.helllabs.android.xmp.util.getIconBitmap
import org.helllabs.android.xmp.util.isAtLeastM
import org.helllabs.android.xmp.util.isAtLeastO

// With Android 11 (R), you can swipe the media notification away, and still plays
// Solved by onTaskRemoved() in service.
// https://www.androidpolice.com/2020/08/07/android-11s-new-media-player-controls-can-be-swiped-away-in-beta-3/
class Notifier(
    private val service: PlayerService,
    private val mediaSession: MediaSessionCompat
) {

    private var queueManager: QueueManager? = null

    private val playAction = NotificationCompat.Action(
        R.drawable.ic_play,
        service.getString(R.string.notif_play),
        makePendingIntent(ACTION_PLAY)
    )

    private val pauseAction = NotificationCompat.Action(
        R.drawable.ic_pause,
        service.getString(R.string.notif_pause),
        makePendingIntent(ACTION_PAUSE)
    )

    private val nextAction = NotificationCompat.Action(
        R.drawable.ic_forward,
        service.getString(R.string.notif_next),
        makePendingIntent(ACTION_NEXT)
    )

    private val prevAction = NotificationCompat.Action(
        R.drawable.ic_previous,
        service.getString(R.string.notif_prev),
        makePendingIntent(ACTION_PREV)
    )

    private val stopAction = NotificationCompat.Action(
        R.drawable.ic_stop,
        service.getString(R.string.notif_stop),
        makePendingIntent(ACTION_STOP)
    )

    init {
        if (isAtLeastO) {
            createNotificationChannel(service)
        }
    }

    fun notify(title: String, info: String, index: Int, type: Int) {

        val indexText = formatIndex(index)
        var notifyTitle: String = title
        var notifyInfo: String = info

        if (title.trim { it <= ' ' }.isEmpty())
            notifyTitle = service.getString(R.string.notif_unknown)

        if (type == TYPE_PAUSE)
            notifyInfo = service.getString(R.string.notif_paused)

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(1, 2, 3)
            .setShowCancelButton(true)

        // Preference to use the new MediaStyle notification or classic notification
        if (PrefManager.useMediaStyle)
            mediaStyle.setMediaSession(mediaSession.sessionToken)

        val notification = NotificationCompat.Builder(service, CHANNEL_ID)
            .setContentIntent(getContentIntent())
            .setStyle(mediaStyle)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notifyTitle)
            .setContentText(notifyInfo)
            .setLargeIcon(service.getIconBitmap())

        // Action Builders
        notification.addAction(prevAction) // 0
        notification.addAction(stopAction) // 1

        if (type == TYPE_PAUSE) {
            notification.addAction(playAction) // 2a
            notification.setContentText(service.getString(R.string.notif_paused))
        } else {
            notification.addAction(pauseAction) // 2b
        }

        notification.addAction(nextAction) // 3

        if (type == TYPE_TICKER) {
            if (queueManager!!.size() > 1)
                notification.setTicker("$notifyTitle ($indexText)")
            else
                notification.setTicker(notifyTitle)
        }

        service.startForeground(NOTIFY_ID, notification.build())
    }

    fun setQueue(queue: QueueManager?) {
        queueManager = queue
    }

    fun cancel() {
        service.stopForeground(true)
        queueManager = null
    }

    private fun formatIndex(index: Int): String {
        return String.format(Locale.US, "%d/%d", index + 1, queueManager!!.size())
    }

    private fun makePendingIntent(action: String): PendingIntent {
        val intent = Intent().apply { this.action = action }
        return PendingIntent.getBroadcast(
            service,
            669,
            intent,
            if (isAtLeastM) FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT else FLAG_UPDATE_CURRENT
        )
    }

    private fun getContentIntent(): PendingIntent {
        val intent = Intent(service, PlayerActivity::class.java)
        return PendingIntent.getActivity(
            service,
            0,
            intent,
            if (isAtLeastM) FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT else FLAG_UPDATE_CURRENT
        )
    }

    @TargetApi(26)
    private fun createNotificationChannel(service: Service) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            service.getString(R.string.notif_channel_name),
            IMPORTANCE_LOW
        ).apply {
            description = service.getString(R.string.notif_channel_desc)
            setShowBadge(false) // No thanks!
        }

        val manager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = BuildConfig.APPLICATION_ID
        const val NOTIFY_ID = R.layout.activity_player

        const val TYPE_TICKER = 1
        const val TYPE_PAUSE = 2

        const val ACTION_STOP = "org.helllabs.android.xmp.STOP"
        const val ACTION_PLAY = "org.helllabs.android.xmp.PLAY"
        const val ACTION_PAUSE = "org.helllabs.android.xmp.PAUSE"
        const val ACTION_PREV = "org.helllabs.android.xmp.PREV"
        const val ACTION_NEXT = "org.helllabs.android.xmp.NEXT"
    }
}
