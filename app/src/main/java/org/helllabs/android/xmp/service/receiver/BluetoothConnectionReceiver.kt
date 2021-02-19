package org.helllabs.android.xmp.service.receiver

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.helllabs.android.xmp.util.Log.i

class BluetoothConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        i(TAG, "Action $action")
        if (intent.action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) {
            val bluetoothState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1)
            i(TAG, "Extra state: $bluetoothState")
            if (bluetoothState == BluetoothProfile.STATE_DISCONNECTING ||
                bluetoothState == BluetoothProfile.STATE_DISCONNECTED
            ) {
                i(TAG, "Bluetooth state changed to disconnected")
                state = DISCONNECTED
            } else if (bluetoothState == BluetoothProfile.STATE_CONNECTED) {
                i(TAG, "Bluetooth state changed to connected")
                state = CONNECTED
            }
        } /* else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
			Log.i(TAG, "Bluetooth connected");
			//state = CONNECTED;
		} else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)) {
			Log.i(TAG, "Bluetooth disconnect requested");
			state = DISCONNECTED;
		} else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
			Log.i(TAG, "Bluetooth disconnected");
			state = DISCONNECTED;
		} */
    }

    companion object {
        private const val TAG = "BluetoothConnectionReceiver"
        const val DISCONNECTED = 0
        const val CONNECTED = 1
        const val NO_STATE = -1
        private var state = NO_STATE

        fun getState(): Int = state

        fun setState(state: Int) {
            Companion.state = state
        }
    }
}
