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

//TODO: Perform migration functions between cmatsuoka's version to this
// 1: Preferences migrations
// 2. Playlists shouldn't be touched, but verify
// 3. Cache (same as above)

//TODO: 'Completely' reorganize strings and arrays for multi language support
//TODO: Find hardcode strings and bring to strings.xml
//TODO: Bring changelog into HTML format. Move out of strings.xml
//TODO: Phase out Crossfader

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
