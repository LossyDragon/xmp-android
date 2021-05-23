package org.helllabs.android.xmp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Someday: JSON playlists
// Someday: Migrate from OpenSLES to oboe.
// Someday: OpenGL ES instead of Canvas
// Someday: MediaBrowserServiceCompat service (Android Auto).
// Someday: Favorites option - Claudio
// Someday: Simple Player Activity (non Viewers)

// Composable lists broken. Wont draw texts in certain cases
// Look: https://issuetracker.google.com/issues/188855913

@HiltAndroidApp
class XmpApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize preferences
        PrefManager.init(applicationContext)
    }

    companion object {
        var fileList: List<String>? = null
    }
}
