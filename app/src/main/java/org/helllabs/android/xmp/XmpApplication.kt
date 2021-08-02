package org.helllabs.android.xmp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.helllabs.android.xmp.ui.preferences.PrefManager

// Someday: JSON playlists
// Someday: Migrate from OpenSLES to oboe.
// Someday: OpenGL ES instead of Canvas
// Someday: MediaBrowserServiceCompat (Android Auto).
// Someday: Favorites system (after MediaBrowserServiceCompat)
// Someday: Simple Player Activity (non Viewers)
// Someday: Jetpack Compose -> [Studying]

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
