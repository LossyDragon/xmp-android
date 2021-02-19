package org.helllabs.android.xmp

import android.app.Application
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import org.helllabs.android.xmp.preferences.PrefManager

class XmpApplication : Application() {

    var fileList: List<String>? = null
    val requestQueue: RequestQueue by lazy { Volley.newRequestQueue(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize preferences
        PrefManager.init(applicationContext)
    }

    fun clearFileList() {
        fileList = null
    }

    companion object {
        @get:Synchronized
        var instance: XmpApplication? = null
            private set
    }
}
