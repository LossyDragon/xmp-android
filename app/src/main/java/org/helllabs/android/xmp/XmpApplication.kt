package org.helllabs.android.xmp

import android.app.Application
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

class XmpApplication : Application() {

    var fileList: List<String>? = null
    val requestQueue: RequestQueue by lazy { Volley.newRequestQueue(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        instance = this
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