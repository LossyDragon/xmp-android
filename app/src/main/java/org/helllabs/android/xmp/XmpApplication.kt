package org.helllabs.android.xmp

import android.app.Application
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley.newRequestQueue
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.util.applyTheme

// TODO: Perform migration functions between cmatsuoka's version to this
// TODO: PlayerActivity DayNight theme
// TODO: Emu crashes on API 19??
// TODO: Finally work on Internal / External storage support

class XmpApplication : Application() {

    var fileList: MutableList<String>? = null
    val requestQueue: RequestQueue by lazy { newRequestQueue(applicationContext) }

    // Workaround for now. Read notes from its usage.
    var isThemeChanged: Boolean = false

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Preferences
        PrefManager.init(applicationContext)

        // Fetch download manager
        val fetchConfiguration = FetchConfiguration.Builder(this)
                .enableRetryOnNetworkGain(true)
                .setDownloadConcurrentLimit(1)
                .build()

        Fetch.setDefaultInstanceConfiguration(fetchConfiguration)

        // DayNight Theme
        applyTheme(PrefManager.themePref)
    }

    companion object {

        @get:Synchronized
        var instance: XmpApplication? = null
            private set
    }
}
