package org.helllabs.android.xmp

import android.app.Application
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley


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
        fileList = null
    }

    companion object {
        @get:Synchronized
        var instance: XmpApplication? = null
            private set
    }

}
