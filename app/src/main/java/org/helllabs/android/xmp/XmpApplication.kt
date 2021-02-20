package org.helllabs.android.xmp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.helllabs.android.xmp.preferences.PrefManager

@HiltAndroidApp
class XmpApplication : Application() {

    var fileList: List<String>? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize preferences
        PrefManager.init(applicationContext)
    }

    fun clearFileList() {
        fileList = null
    }

    companion object {
        @get:Synchronized
        var instance: XmpApplication? = null
            private set
    }
}
