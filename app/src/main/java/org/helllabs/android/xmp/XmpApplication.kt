package org.helllabs.android.xmp

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import org.helllabs.android.xmp.preferences.Preferences


class XmpApplication : Application() {

    var fileList: MutableList<String>? = null
    private var mRequestQueue: RequestQueue? = null

    val requestQueue: RequestQueue
        get() {
            if (mRequestQueue == null) {
                mRequestQueue = Volley.newRequestQueue(applicationContext)
            }

            return mRequestQueue!!
        }

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
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val themePref = sharedPreferences.getString(Preferences.APP_THEME, DEFAULT_MODE)
        applyTheme(themePref!!)
    }

    fun clearFileList() {
        fileList = null
    }

    companion object {
        private const val LIGHT_MODE = "light"
        private const val DARK_MODE = "dark"
        const val DEFAULT_MODE = "default"

        @get:Synchronized
        var instance: XmpApplication? = null
            private set

        fun applyTheme(theme: String) {
            when (theme) {
                LIGHT_MODE -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                DARK_MODE -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                    }
                }
            }
        }
    }

}
