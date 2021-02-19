package org.helllabs.android.xmp.service

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.view.KeyEvent
import androidx.preference.PreferenceManager
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.service.receiver.BluetoothConnectionReceiver
import org.helllabs.android.xmp.service.receiver.HeadsetPlugReceiver
import org.helllabs.android.xmp.service.receiver.MediaButtonsReceiver
import org.helllabs.android.xmp.service.receiver.NotificationActionReceiver
import org.helllabs.android.xmp.util.Log.i

class ReceiverHelper(private val player: PlayerService) {

    private var headsetPlugReceiver: HeadsetPlugReceiver? = null
    private var bluetoothConnectionReceiver: BluetoothConnectionReceiver? = null
    private var mediaButtons: MediaButtons? = null
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(player)

    // Autopause
    var isAutoPaused = false // paused on phone call
    var isHeadsetPaused = false

    fun registerReceivers() {
        if (prefs.getBoolean(Preferences.HEADSET_PAUSE, true)) {
            i(TAG, "Register headset receiver")
            // For listening to headset changes, the broadcast receiver cannot be
            // declared in the manifest, it must be dynamically registered.
            headsetPlugReceiver = HeadsetPlugReceiver()
            player.registerReceiver(headsetPlugReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
        }
        if (prefs.getBoolean(Preferences.BLUETOOTH_PAUSE, true)) {
            i(TAG, "Register bluetooth receiver")
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
        val key: Int = MediaButtonsReceiver.getKeyCode()
        if (key != MediaButtonsReceiver.NO_KEY) {
            when (key) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    i(TAG, "Handle KEYCODE_MEDIA_NEXT")
                    player.actionNext()
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    i(TAG, "Handle KEYCODE_MEDIA_PREVIOUS")
                    player.actionPrev()
                }
                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    i(TAG, "Handle KEYCODE_MEDIA_STOP")
                    player.actionStop()
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    i(TAG, "Handle KEYCODE_MEDIA_PLAY_PAUSE")
                    player.actionPlayPause()
                    isHeadsetPaused = false
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    i(TAG, "Handle KEYCODE_MEDIA_PLAY")
                    if (player.isPlayerPaused) {
                        player.actionPlayPause()
                    }
                }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    i(TAG, "Handle KEYCODE_MEDIA_PAUSE")
                    if (!player.isPlayerPaused) {
                        player.actionPlayPause()
                        isHeadsetPaused = false
                    }
                }
            }
            MediaButtonsReceiver.setKeyCode(MediaButtonsReceiver.NO_KEY)
        }
    }

    private fun checkNotificationButtons() {
        val key: Int = NotificationActionReceiver.getKeyCode()
        if (key != NotificationActionReceiver.NO_KEY) {
            when (key) {
                NotificationActionReceiver.STOP -> {
                    i(TAG, "Handle notification stop")
                    player.actionStop()
                }
                NotificationActionReceiver.PAUSE -> {
                    i(TAG, "Handle notification pause")
                    player.actionPlayPause()
                    isHeadsetPaused = false
                }
                NotificationActionReceiver.NEXT -> {
                    i(TAG, "Handle notification next")
                    player.actionNext()
                }
                NotificationActionReceiver.PREV -> {
                    i(TAG, "Handle notification prev")
                    player.actionPrev()
                }
            }
            NotificationActionReceiver.setKeyCode(NotificationActionReceiver.NO_KEY)
        }
    }

    private fun checkHeadsetState() {
        val state: Int = HeadsetPlugReceiver.getState()
        if (state != HeadsetPlugReceiver.NO_STATE) {
            when (state) {
                HeadsetPlugReceiver.HEADSET_UNPLUGGED -> {
                    i(TAG, "Handle headset unplugged")

                    // If not already paused
                    if (!player.isPlayerPaused && !isAutoPaused) {
                        isHeadsetPaused = true
                        player.actionPlayPause()
                    } else {
                        i(TAG, "Already paused")
                    }
                }
                HeadsetPlugReceiver.HEADSET_PLUGGED -> {
                    i(TAG, "Handle headset plugged")

                    // If paused by headset unplug
                    if (isHeadsetPaused) {
                        // Don't unpause if we're paused due to phone call
                        if (!isAutoPaused) {
                            player.actionPlayPause()
                        } else {
                            i(TAG, "Paused by phone state, don't unpause")
                        }
                        isHeadsetPaused = false
                    } else {
                        i(TAG, "Manual pause, don't unpause")
                    }
                }
            }
            HeadsetPlugReceiver.setState(HeadsetPlugReceiver.NO_STATE)
        }
    }

    private fun checkBluetoothState() {
        val state: Int = BluetoothConnectionReceiver.getState()
        if (state != BluetoothConnectionReceiver.NO_STATE) {
            when (state) {
                BluetoothConnectionReceiver.DISCONNECTED -> {
                    i(TAG, "Handle bluetooth disconnection")

                    // If not already paused
                    if (!player.isPlayerPaused && !isAutoPaused) {
                        isHeadsetPaused = true
                        player.actionPlayPause()
                    } else {
                        i(TAG, "Already paused")
                    }
                }
                BluetoothConnectionReceiver.CONNECTED -> {
                    i(TAG, "Handle bluetooth connection")

                    // If paused by headset unplug
                    if (isHeadsetPaused) {
                        // Don't unpause if we're paused due to phone call
                        if (!isAutoPaused) {
                            player.actionPlayPause()
                        } else {
                            i(TAG, "Paused by phone state, don't unpause")
                        }
                        isHeadsetPaused = false
                    } else {
                        i(TAG, "Manual pause, don't unpause")
                    }
                }
            }
            BluetoothConnectionReceiver.setState(BluetoothConnectionReceiver.NO_STATE)
        }
    }

    companion object {
        private const val TAG = "ReceiverHelper"
    }

}