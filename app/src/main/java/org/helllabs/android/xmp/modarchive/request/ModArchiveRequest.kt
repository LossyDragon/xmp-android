package org.helllabs.android.xmp.modarchive.request

import android.content.Context
import android.os.Handler
import okhttp3.*
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse
import org.helllabs.android.xmp.util.Log
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

abstract class ModArchiveRequest(
        private val mKey: String,
        private val mRequest: String
) {

    private var mOnResponseListener: OnResponseListener? = null

    interface OnResponseListener {
        fun onResponse(response: ModArchiveResponse)
        fun onSoftError(response: SoftErrorResponse)
        fun onHardError(response: HardErrorResponse)
    }

    init {
        Log.d(TAG, "request=$mRequest")
    }

    @Throws(UnsupportedEncodingException::class)
    constructor(key: String, request: String, parameter: String) :
            this(key, request + URLEncoder.encode(parameter, "UTF-8"))

    fun setOnResponseListener(listener: OnResponseListener): ModArchiveRequest {
        mOnResponseListener = listener
        return this
    }

    fun send(context: Context) {
        val request = Request.Builder()
                .url("$SERVER/xml-tools.php?key=$mKey&request=$mRequest")
                .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "okHttp error: " + e.message)
                mOnResponseListener!!.onHardError(HardErrorResponse(Throwable(e.message)))
            }

            override fun onResponse(call: Call, response: Response) {
                Log.i(TAG, "okHttp: get response")
                val resp = xmlParse(response.body!!.string())
                Handler(context.mainLooper).post {
                    if (resp is SoftErrorResponse) {
                        mOnResponseListener!!.onSoftError(resp)
                    } else {
                        mOnResponseListener!!.onResponse(resp)
                    }
                }
            }
        })
    }

    protected abstract fun xmlParse(result: String): ModArchiveResponse

    companion object {
        private val TAG = ModArchiveRequest::class.java.simpleName

        private const val SERVER = "http://api.modarchive.org"

        const val ARTIST = "search_artist&query="
        const val ARTIST_MODULES = "view_modules_by_artistid&query="
        const val MODULE = "view_by_moduleid&query="
        const val RANDOM = "random"
        const val FILENAME_OR_TITLE = "search&type=filename_or_songtitle&query="
    }
}
