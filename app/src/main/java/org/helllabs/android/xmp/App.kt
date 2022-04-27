package org.helllabs.android.xmp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.helllabs.android.xmp.util.preferences.Manager
import timber.log.Timber

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        Manager.init(applicationContext)

        val tree = Timber.DebugTree()
        Timber.plant(tree)
    }
}
