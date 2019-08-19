package org.helllabs.android.xmp.service.utils

import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.RemoteControlClient
import org.helllabs.android.xmp.service.receiver.RemoteControlReceiver
import org.helllabs.android.xmp.util.Log


@TargetApi(19)
class RemoteControl(context: Context, audioManager: AudioManager) {
    private val remoteControlReceiver: ComponentName = ComponentName(context.packageName, RemoteControlReceiver::class.java.name)
    private var remoteControlClient: RemoteControlClientCompat? = null

    init {

        if (remoteControlClient == null) {
            Log.i(TAG, "Register remote control client")

            audioManager.registerMediaButtonEventReceiver(remoteControlReceiver)

            val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
            intent.component = remoteControlReceiver

            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            remoteControlClient = RemoteControlClientCompat(pendingIntent)

            remoteControlClient!!.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE or
                            RemoteControlClient.FLAG_KEY_MEDIA_STOP or
                            RemoteControlClient.FLAG_KEY_MEDIA_NEXT or
                            RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS)


            RemoteControlHelper.registerRemoteControlClient(audioManager, remoteControlClient!!)
        }
    }

//    fun unregisterReceiver() {
//        Log.w(TAG, "Unregister remote control client")
//        audioManager.unregisterMediaButtonEventReceiver(remoteControlReceiver)
//        RemoteControlHelper.unregisterRemoteControlClient(audioManager, remoteControlClient!!)
//    }

    @TargetApi(14)
    fun setStatePlaying() {
        if (remoteControlClient != null) {
            Log.i(TAG, "Set state to playing")
            remoteControlClient!!.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING)
        }
    }

    @TargetApi(14)
    fun setStatePaused() {
        if (remoteControlClient != null) {
            Log.i(TAG, "Set state to paused")
            remoteControlClient!!.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED)
        }
    }

    @TargetApi(14)
    fun setStateStopped() {
        if (remoteControlClient != null) {
            Log.i(TAG, "Set state to stopped")
            remoteControlClient!!.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED)
        }
    }

    @TargetApi(10)
    fun setMetadata(title: String, type: String, duration: Long) {
        if (remoteControlClient != null) {
            val editor = remoteControlClient!!.editMetadata(true)
            //editor.putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, dummyAlbumArt);
            editor.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, duration)
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, type)
            editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, title)
            editor.apply()
        }
    }

    companion object {
        private const val TAG = "RemoteControl"
    }
}
