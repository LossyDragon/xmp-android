package com.lossydragon.media3

import android.app.Application
import android.util.Log
import com.lossydragon.media3.core.CrashHandler
import com.lossydragon.media3.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class XmpApp : Application() {
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.DEBUG) return
            Log.println(priority, "Xmp Mod Player", message)
        }
    }

    override fun onCreate() {
        super.onCreate()

        CrashHandler.initialize(this@XmpApp)

        startKoin {
            androidContext(this@XmpApp)
            androidLogger(Level.DEBUG)
            modules(appModule)
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
}
