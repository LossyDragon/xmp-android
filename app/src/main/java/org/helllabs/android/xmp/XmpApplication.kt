package org.helllabs.android.xmp

import android.app.Application
import android.net.Uri
import android.util.Log
import org.helllabs.android.xmp.core.PrefManager
import timber.log.Timber

// TODO add migration tool for older playlists.

class XmpApplication : Application() {
    var fileListUri: List<Uri>? = null

    override fun onCreate() {
        super.onCreate()
        setInstance(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        PrefManager.init(applicationContext)
    }

    fun clearFileList() {
        fileListUri = null
    }

    companion object {
        @get:Synchronized
        var instance: XmpApplication? = null
            private set

        private fun setInstance(app: XmpApplication) {
            instance = app
        }
    }

    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.DEBUG) {
                return
            }

            Log.println(priority, "Xmp Mod Player", message)
        }
    }
}
