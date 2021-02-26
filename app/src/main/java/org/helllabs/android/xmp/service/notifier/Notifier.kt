package org.helllabs.android.xmp.service.notifier

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.service.receiver.NotificationActionReceiver
import org.helllabs.android.xmp.service.utils.QueueManager

abstract class Notifier(protected val service: Service) {

    protected var queueManager: QueueManager? = null
    protected val contentIntent: PendingIntent
    protected val icon: Bitmap
    protected val prevIntent: PendingIntent
    protected val stopIntent: PendingIntent
    protected val pauseIntent: PendingIntent
    protected val nextIntent: PendingIntent

    init {
        val intent = Intent(service, PlayerActivity::class.java)
        contentIntent = PendingIntent.getActivity(service, 0, intent, 0)
        prevIntent = makePendingIntent(ACTION_PREV)
        stopIntent = makePendingIntent(ACTION_STOP)
        pauseIntent = makePendingIntent(ACTION_PAUSE)
        nextIntent = makePendingIntent(ACTION_NEXT)
        icon = ResourcesCompat.getDrawable(
            service.resources,
            R.drawable.ic_xmp_vector,
            null
        )!!.toBitmap()
    }

    abstract fun notify(title: String, info: String, index: Int, type: Int)

    protected fun formatIndex(index: Int): String {
        return String.format(Locale.US, "%d/%d", index + 1, queueManager!!.size())
    }

    private fun makePendingIntent(action: String): PendingIntent {
        val intent = Intent(service, NotificationActionReceiver::class.java)
        intent.action = action
        return PendingIntent.getBroadcast(service, 0, intent, 0)
    }

    fun setQueue(queue: QueueManager?) {
        queueManager = queue
    }

    fun cancel() {
        service.stopForeground(true)
    }

    companion object {
        const val NOTIFY_ID = R.layout.activity_player
        const val TYPE_TICKER = 1
        const val TYPE_PAUSE = 2
        const val ACTION_STOP = "org.helllabs.android.xmp.STOP"
        const val ACTION_PAUSE = "org.helllabs.android.xmp.PAUSE"
        const val ACTION_PREV = "org.helllabs.android.xmp.PREV"
        const val ACTION_NEXT = "org.helllabs.android.xmp.NEXT"
    }
}
