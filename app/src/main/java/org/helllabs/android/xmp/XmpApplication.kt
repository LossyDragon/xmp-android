package org.helllabs.android.xmp

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley.newRequestQueue
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.util.THEME_DEFAULT
import org.helllabs.android.xmp.util.applyTheme


class XmpApplication : Application() {

    var fileList: MutableList<String>? = null
    val requestQueue: RequestQueue by lazy { newRequestQueue(applicationContext) }
    val sharedPrefs: SharedPreferences by lazy { getDefaultSharedPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        //Fetch download manager
        val fetchConfiguration = FetchConfiguration.Builder(this)
                .enableRetryOnNetworkGain(true)
                .setDownloadConcurrentLimit(1)
                .build()

        Fetch.setDefaultInstanceConfiguration(fetchConfiguration)

        //DayNight Theme
        val themePref = sharedPrefs.getString(Preferences.APP_THEME, THEME_DEFAULT)
        applyTheme(themePref!!)
    }

    companion object {

        @get:Synchronized
        var instance: XmpApplication? = null
            private set

    }
}
