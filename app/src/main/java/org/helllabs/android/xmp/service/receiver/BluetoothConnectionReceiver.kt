package org.helllabs.android.xmp.service.receiver

import org.helllabs.android.xmp.util.Log

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BluetoothConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Action " + action!!)

        if (intent.action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) {
            val bluetoothState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1)
            Log.i(TAG, "Extra state: $bluetoothState")
            if (bluetoothState == BluetoothProfile.STATE_DISCONNECTING || bluetoothState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Bluetooth state changed to disconnected")
                state = DISCONNECTED
            } else if (bluetoothState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Bluetooth state changed to connected")
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
        private val TAG = "BluetoothConnectionReceiver"
        val DISCONNECTED = 0
        val CONNECTED = 1
        val NO_STATE = -1
        var state = NO_STATE
    }

}
