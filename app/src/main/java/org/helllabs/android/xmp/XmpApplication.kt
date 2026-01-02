package org.helllabs.android.xmp

import android.app.Application
import org.helllabs.android.xmp.core.ReleaseTree
import org.helllabs.android.xmp.di.appModule
import org.helllabs.android.xmp.di.modArchiveModule
import org.helllabs.android.xmp.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

// TODO add migration tool for older playlists.

class XmpApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@XmpApplication)
            modules(listOf(viewModelModule, modArchiveModule, appModule))
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
}
