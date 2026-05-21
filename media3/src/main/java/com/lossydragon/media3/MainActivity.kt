package com.lossydragon.media3

import android.Manifest
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.lossydragon.media3.player.XmpService
import com.lossydragon.media3.ui.XmpNavHost
import com.lossydragon.media3.ui.theme.XmpTheme
import com.lossydragon.media3.util.requestNotificationPermission
import com.lossydragon.media3.util.requestWriteStoragePermission
import com.lossydragon.media3.util.setEdgeToEdgeConfig

class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val notificationPermission = registerForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        callback = { /* No-Op */ }
    )

    private val storagePermission = registerForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        callback = { /* No-Op */ }
    )

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(
            this,
            ComponentName(this, XmpService::class.java)
        )
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
    }

    override fun onStop() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeConfig()

        requestNotificationPermission {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestWriteStoragePermission {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        setContent {
            XmpTheme {
                XmpNavHost(onBack = ::finish)
            }
        }
    }
}
