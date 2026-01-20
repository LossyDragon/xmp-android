package org.helllabs.android.xmp

import android.app.Application
import android.util.Log
import org.helllabs.android.xmp.di.appModule
import org.helllabs.android.xmp.di.modArchiveModule
import org.helllabs.android.xmp.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

// TODO add migration tool for older playlists.

class XmpApplication : Application() {

    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.DEBUG) {
                return
            }

            Log.println(priority, "Xmp Mod Player", message)
        }
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@XmpApplication)
            modules(listOf(viewModelModule, modArchiveModule, appModule))
        }

        CrashHandler.initialize(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
}
