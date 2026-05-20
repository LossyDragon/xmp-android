package com.lossydragon.media3.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

fun Context.requestNotificationPermission(onRequest: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val hasPerm = ContextCompat
            .checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (hasPerm != PackageManager.PERMISSION_GRANTED) {
            onRequest()
        }
    }
}

fun Context.requestWriteStoragePermission(onRequest: () -> Unit) {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        val hasPerm = ContextCompat
            .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (hasPerm != PackageManager.PERMISSION_GRANTED) {
            onRequest()
        }
    }
}
