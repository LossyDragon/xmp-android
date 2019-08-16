package org.helllabs.android.xmp.service

import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.service.receiver.BluetoothConnectionReceiver
import org.helllabs.android.xmp.service.receiver.HeadsetPlugReceiver
import org.helllabs.android.xmp.service.receiver.MediaButtonsReceiver
import org.helllabs.android.xmp.service.receiver.NotificationActionReceiver
import org.helllabs.android.xmp.util.Log

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.view.KeyEvent
import androidx.preference.PreferenceManager

class ReceiverHelper(private val player: PlayerService) {
    private var headsetPlugReceiver: HeadsetPlugReceiver? = null
    private var bluetoothConnectionReceiver: BluetoothConnectionReceiver? = null
    private var mediaButtons: MediaButtons? = null
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(player)

    // Autopause
    var isAutoPaused: Boolean = false            // paused on phone call
    var isHeadsetPaused: Boolean = false

    fun registerReceivers() {
        if (prefs.getBoolean(Preferences.HEADSET_PAUSE, true)) {
            Log.i(TAG, "Register headset receiver")
            // For listening to headset changes, the broadcast receiver cannot be
            // declared in the manifest, it must be dynamically registered.
            headsetPlugReceiver = HeadsetPlugReceiver()
            player.registerReceiver(headsetPlugReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
        }

        if (prefs.getBoolean(Preferences.BLUETOOTH_PAUSE, true)) {
            Log.i(TAG, "Register bluetooth receiver")
            bluetoothConnectionReceiver = BluetoothConnectionReceiver()
            val filter = IntentFilter()
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)

            player.registerReceiver(bluetoothConnectionReceiver, filter)
        }

        mediaButtons = MediaButtons(player)
        mediaButtons!!.register()
    }


    fun unregisterReceivers() {
        if (headsetPlugReceiver != null) {
            player.unregisterReceiver(headsetPlugReceiver)
        }
        if (bluetoothConnectionReceiver != null) {        // Z933 (glaucus) needs this test
            player.unregisterReceiver(bluetoothConnectionReceiver)
        }
        if (mediaButtons != null) {
            mediaButtons!!.unregister()
        }
    }

    fun checkReceivers() {
        checkMediaButtons()
        checkHeadsetState()
        checkBluetoothState()
        checkNotificationButtons()
    }

    private fun checkMediaButtons() {
        val key = MediaButtonsReceiver.keyCode

        if (key != MediaButtonsReceiver.NO_KEY) {
            when (key) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    Log.i(TAG, "Handle KEYCODE_MEDIA_NEXT")
                    player.actionNext()
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    Log.i(TAG, "Handle KEYCODE_MEDIA_PREVIOUS")
                    player.actionPrev()
                }
                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    Log.i(TAG, "Handle KEYCODE_MEDIA_STOP")
                    player.actionStop()
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    Log.i(TAG, "Handle KEYCODE_MEDIA_PLAY_PAUSE")
                    player.actionPlayPause()
                    isHeadsetPaused = false
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    Log.i(TAG, "Handle KEYCODE_MEDIA_PLAY")
                    if (player.isPlayerPaused) {
                        player.actionPlayPause()
                    }
                }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    Log.i(TAG, "Handle KEYCODE_MEDIA_PAUSE")
                    if (!player.isPlayerPaused) {
                        player.actionPlayPause()
                        isHeadsetPaused = false
                    }
                }
            }

            MediaButtonsReceiver.keyCode = MediaButtonsReceiver.NO_KEY
        }
    }

    private fun checkNotificationButtons() {
        val key = NotificationActionReceiver.keyCode

        if (key != NotificationActionReceiver.NO_KEY) {
            when (key) {
                NotificationActionReceiver.STOP -> {
                    Log.i(TAG, "Handle notification stop")
                    player.actionStop()
                }
                NotificationActionReceiver.PAUSE -> {
                    Log.i(TAG, "Handle notification pause")
                    player.actionPlayPause()
                    isHeadsetPaused = false
                }
                NotificationActionReceiver.NEXT -> {
                    Log.i(TAG, "Handle notification next")
                    player.actionNext()
                }
                NotificationActionReceiver.PREV -> {
                    Log.i(TAG, "Handle notification prev")
                    player.actionPrev()
                }
            }

            NotificationActionReceiver.keyCode = NotificationActionReceiver.NO_KEY
        }
    }

    private fun checkHeadsetState() {
        val state = HeadsetPlugReceiver.state

        if (state != HeadsetPlugReceiver.NO_STATE) {
            when (state) {
                HeadsetPlugReceiver.HEADSET_UNPLUGGED -> {
                    Log.i(TAG, "Handle headset unplugged")

                    // If not already paused
                    if (!player.isPlayerPaused && !isAutoPaused) {
                        isHeadsetPaused = true
                        player.actionPlayPause()
                    } else {
                        Log.i(TAG, "Already paused")
                    }
                }
                HeadsetPlugReceiver.HEADSET_PLUGGED -> {
                    Log.i(TAG, "Handle headset plugged")

                    // If paused by headset unplug
                    if (isHeadsetPaused) {
                        // Don't unpause if we're paused due to phone call
                        if (!isAutoPaused) {
                            player.actionPlayPause()
                        } else {
                            Log.i(TAG, "Paused by phone state, don't unpause")
                        }
                        isHeadsetPaused = false
                    } else {
                        Log.i(TAG, "Manual pause, don't unpause")
                    }
                }
            }

            HeadsetPlugReceiver.state = HeadsetPlugReceiver.NO_STATE
        }
    }

    private fun checkBluetoothState() {
        val state = BluetoothConnectionReceiver.state

        if (state != BluetoothConnectionReceiver.NO_STATE) {
            when (state) {
                BluetoothConnectionReceiver.DISCONNECTED -> {
                    Log.i(TAG, "Handle bluetooth disconnection")

                    // If not already paused
                    if (!player.isPlayerPaused && !isAutoPaused) {
                        isHeadsetPaused = true
                        player.actionPlayPause()
                    } else {
                        Log.i(TAG, "Already paused")
                    }
                }
                BluetoothConnectionReceiver.CONNECTED -> {
                    Log.i(TAG, "Handle bluetooth connection")

                    // If paused by headset unplug
                    if (isHeadsetPaused) {
                        // Don't unpause if we're paused due to phone call
                        if (!isAutoPaused) {
                            player.actionPlayPause()
                        } else {
                            Log.i(TAG, "Paused by phone state, don't unpause")
                        }
                        isHeadsetPaused = false
                    } else {
                        Log.i(TAG, "Manual pause, don't unpause")
                    }
                }
            }

            BluetoothConnectionReceiver.state = BluetoothConnectionReceiver.NO_STATE
        }
    }

    companion object {
        private const val TAG = "ReceiverHelper"
    }
}
