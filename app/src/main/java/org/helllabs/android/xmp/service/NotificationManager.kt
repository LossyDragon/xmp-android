package org.helllabs.android.xmp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.MainActivity
import org.helllabs.android.xmp.util.getIconBitmap
import timber.log.Timber

// With Android 11 (R), you can swipe the media notification away, and still plays
// Solved by onTaskRemoved() in service.
// https://www.androidpolice.com/2020/08/07/android-11s-new-media-player-controls-can-be-swiped-away-in-beta-3/
class NotificationManager(
    private val service: PlayerService
) {
    companion object {
        const val NOTIFY_ID = 669
        const val REQUEST_CODE = 669

        private const val CHANNEL_ID = "org.helllabs.android.xmp.service.NotificationManager"
    }

    val notificationManager: NotificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val isAtLeastO: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    private val isAtLeastM: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    private val isAtLeastS: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    private val actions = if (isAtLeastS) intArrayOf(0, 2, 3) else intArrayOf(1, 2, 3)

    private val actionPlay: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_play,
        "Play",
        buildMediaButtonPendingIntent(PlaybackStateCompat.ACTION_PLAY)
    )
    private val actionPause: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_pause,
        "Pause",
        buildMediaButtonPendingIntent(PlaybackStateCompat.ACTION_PAUSE)
    )
    private val actionSkipNext: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_skip_next,
        "Skip Next",
        buildMediaButtonPendingIntent(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
    )
    private val actionSkipPrevious: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_skip_previous,
        "Skip Previous",
        buildMediaButtonPendingIntent(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
    )

    init {
        notificationManager.cancelAll()
    }

    fun getNotification(
        metadata: MediaMetadataCompat?,
        state: PlaybackStateCompat,
        token: MediaSessionCompat.Token
    ): Notification {
        val isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
        val description = metadata?.description
        val builder = buildNotification(state, token, isPlaying, description)
        return builder.build()
    }

    private fun buildNotification(
        state: PlaybackStateCompat,
        token: MediaSessionCompat.Token,
        isPlaying: Boolean,
        description: MediaDescriptionCompat?
    ): NotificationCompat.Builder {

        if (isAtLeastO) {
            createChannelForAndroidO()
        }

        val deletePendingIntent =
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                service,
                PlaybackStateCompat.ACTION_STOP
            )

        val builder = NotificationCompat.Builder(service, CHANNEL_ID)
            .setStyle(setMediaStyle(token))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(createContentIntent())
            .setContentTitle(description?.title)
            .setContentText(description?.subtitle)
            .setLargeIcon(service.getIconBitmap())
            .setDeleteIntent(deletePendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // If skip to next action is enabled.
        if (state.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L) {
            builder.addAction(actionSkipPrevious)
        }
        builder.addAction(if (isPlaying) actionPause else actionPlay)

        // If skip to prev action is enabled.
        if (state.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L) {
            builder.addAction(actionSkipNext)
        }

        return builder
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannelForAndroidO() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Player Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Allows Xmp Mod Player's service to run and display notifications"
                setShowBadge(false) // Maybe preference this?
            }

            notificationManager.createNotificationChannel(channel)

            Timber.d("Creating NotificationChannel")
        } else {
            Timber.d("NotificationChannel reused")
        }
    }

    private fun setMediaStyle(token: MediaSessionCompat.Token) =
        androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(token)
            .setShowActionsInCompactView(*actions)
            .setShowCancelButton(true)
            .setCancelButtonIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    service,
                    PlaybackStateCompat.ACTION_STOP
                )
            )

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(service, MainActivity::class.java).also {
            it.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flags = if (isAtLeastM) FLAG_IMMUTABLE or FLAG_CANCEL_CURRENT else FLAG_CANCEL_CURRENT

        return PendingIntent.getActivity(service, REQUEST_CODE, intent, flags)
    }

    private fun buildMediaButtonPendingIntent(
        @PlaybackStateCompat.MediaKeyAction action: Long
    ): PendingIntent? = MediaButtonReceiver.buildMediaButtonPendingIntent(service, action)
}
