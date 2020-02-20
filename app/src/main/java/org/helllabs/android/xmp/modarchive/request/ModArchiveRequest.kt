package org.helllabs.android.xmp.modarchive.request

import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse
import org.helllabs.android.xmp.util.Log
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

abstract class ModArchiveRequest(
        private val mKey: String,
        private val mRequest: String
) :
        Response.Listener<String>,
        Response.ErrorListener {

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

    fun send(requestQueue: RequestQueue) {
        val url = "$SERVER/xml-tools.php?key=$mKey&request=$mRequest"
        val jsObjRequest = StringRequest(url, this, this)
        requestQueue.add(jsObjRequest)
    }

    override fun onErrorResponse(error: VolleyError) {
        Log.e(TAG, "Volley error: " + error.message)
        mOnResponseListener!!.onHardError(HardErrorResponse(error))
    }

    override fun onResponse(result: String) {
        Log.i(TAG, "Volley: get response")
        val response = xmlParse(result)
        if (response is SoftErrorResponse) {
            mOnResponseListener!!.onSoftError(response)
        } else {
            mOnResponseListener!!.onResponse(response)
        }
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
