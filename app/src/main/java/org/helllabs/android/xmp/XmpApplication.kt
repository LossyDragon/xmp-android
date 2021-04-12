package org.helllabs.android.xmp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Someday: JSON playlists
// Someday: Migrate from OpenSLES to oboe.
// Someday: OpenGL ES instead of Canvas
// Someday: MediaBrowserServiceCompat service (Android Auto).
// Someday: Favorites option - Claudio
// Someday: Simple Player Activity (non Viewers)

@HiltAndroidApp
class XmpApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize preferences
        PrefManager.init(applicationContext)
    }

    companion object {
        @JvmStatic
        var fileList: List<String>? = null
    }
}
