package org.helllabs.android.xmp

import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

import android.app.Application

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
    }

    fun clearFileList() {
        fileList = null    // NOPMD
    }

    companion object {
        @get:Synchronized // NOPMD
        var instance: XmpApplication? = null
            private set
    }

}
