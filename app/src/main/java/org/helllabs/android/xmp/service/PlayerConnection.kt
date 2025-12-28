package org.helllabs.android.xmp.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class PlayerConnection(private val context: Context) {
    var modPlayer: PlayerService? = null
        private set

    private val _isBound = MutableStateFlow(false)
    val isBound: StateFlow<Boolean> = _isBound.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            modPlayer = (binder as PlayerBinder).getService()
            _isBound.value = true
            Timber.d("Service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            modPlayer = null
            _isBound.value = false
            Timber.d("Service disconnected")
        }
    }

    fun bindService() {
        if (!isBound.value) {
            val intent = Intent(context, PlayerService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unBindService() {
        if (isBound.value) {
            context.unbindService(serviceConnection)
            _isBound.value = false
            modPlayer = null
        }
    }

    fun startForegroundService() {
        val intent = Intent(context, PlayerService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopForegroundService() {
        val intent = Intent(context, PlayerService::class.java)
        context.stopService(intent)
    }
}
