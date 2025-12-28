package org.helllabs.android.xmp

import android.app.Application
import android.net.Uri
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.ReleaseTree
import org.helllabs.android.xmp.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

// TODO add migration tool for older playlists.

class XmpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
}
