package org.helllabs.android.xmp.service.notifier

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.service.receiver.NotificationActionReceiver
import org.helllabs.android.xmp.service.utils.QueueManager
import java.util.Locale

abstract class Notifier(protected val service: Service) {

    protected lateinit var queueManager: QueueManager
    protected val contentIntent: PendingIntent

    protected val icon: Bitmap
    protected val prevIntent: PendingIntent
    protected val stopIntent: PendingIntent
    protected val pauseIntent: PendingIntent
    protected val nextIntent: PendingIntent
    protected val playIntent: PendingIntent

    init {
        val intent = Intent(service, PlayerActivity::class.java)
        contentIntent = PendingIntent.getActivity(service, 0, intent, 0)

        icon = BitmapFactory.decodeResource(service.resources, R.mipmap.ic_launcher_foreground)
        prevIntent = makePendingIntent(PlayerService.XMP_PLAYER_PREV)
        stopIntent = makePendingIntent(PlayerService.XMP_PLAYER_STOP)
        playIntent = makePendingIntent(PlayerService.XMP_PLAYER_PLAY)
        pauseIntent = makePendingIntent(PlayerService.XMP_PLAYER_PAUSE)
        nextIntent = makePendingIntent(PlayerService.XMP_PLAYER_NEXT)
    }

    protected fun formatIndex(index: Int): String {
        return String.format(Locale.US, "%d/%d", index + 1, queueManager.size())
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
        const val NOTIFY_ID = R.layout.layout_player

        const val TYPE_TICKER = 1
        const val TYPE_PAUSE = 2
    }
}
