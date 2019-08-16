package org.helllabs.android.xmp.service.notifier

import java.util.Locale

import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.service.receiver.NotificationActionReceiver
import org.helllabs.android.xmp.service.utils.QueueManager

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory


abstract class Notifier(protected val service: Service) {

    protected lateinit var queueManager: QueueManager
    protected val contentIntent: PendingIntent

    protected val icon: Bitmap
    protected val prevIntent: PendingIntent
    protected val stopIntent: PendingIntent
    protected val pauseIntent: PendingIntent
    protected val nextIntent: PendingIntent


    init {
        val intent = Intent(service, PlayerActivity::class.java)
        contentIntent = PendingIntent.getActivity(service, 0, intent, 0)

        icon = BitmapFactory.decodeResource(service.resources, R.drawable.icon)
        prevIntent = makePendingIntent(ACTION_PREV)
        stopIntent = makePendingIntent(ACTION_STOP)
        pauseIntent = makePendingIntent(ACTION_PAUSE)
        nextIntent = makePendingIntent(ACTION_NEXT)
    }

    protected fun formatIndex(index: Int): String {
        return String.format(Locale.US, "%d/%d", index + 1, queueManager.size())
    }

    fun cancel() {
        service.stopForeground(true)
    }

    private fun makePendingIntent(action: String): PendingIntent {
        val intent = Intent(service, NotificationActionReceiver::class.java)
        intent.action = action
        return PendingIntent.getBroadcast(service, 0, intent, 0)
    }

    abstract fun notify(title: String, info: String, index: Int, type: Int)

    fun setQueue(queue: QueueManager) {
        queueManager = queue
    }

    companion object {

        const val NOTIFY_ID = R.layout.player

        val TYPE_TICKER = 1
        val TYPE_PAUSE = 2

        val ACTION_STOP = "org.helllabs.android.xmp.STOP"
        val ACTION_PAUSE = "org.helllabs.android.xmp.PAUSE"
        val ACTION_PREV = "org.helllabs.android.xmp.PREV"
        val ACTION_NEXT = "org.helllabs.android.xmp.NEXT"
    }
}
