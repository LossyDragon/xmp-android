package org.helllabs.android.xmp.modarchive.request

import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import java.net.URLEncoder
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse
import org.helllabs.android.xmp.util.Log.d
import org.helllabs.android.xmp.util.Log.e
import org.helllabs.android.xmp.util.Log.i

abstract class ModArchiveRequest(
    key: String,
    request: String
) : Response.Listener<String>,
    Response.ErrorListener {

    private val mKey: String
    private val mRequest: String
    private var mOnResponseListener: OnResponseListener? = null

    interface OnResponseListener {
        fun onResponse(response: ModArchiveResponse)
        fun onSoftError(response: SoftErrorResponse)
        fun onHardError(response: HardErrorResponse)
    }

    init {
        d(TAG, "request=$request")
        mKey = key
        mRequest = request
    }

    constructor(key: String, request: String, parameter: String?) :
        this(key, request + URLEncoder.encode(parameter, "UTF-8"))

    protected abstract fun xmlParse(result: String): ModArchiveResponse

    fun setOnResponseListener(listener: OnResponseListener?): ModArchiveRequest {
        mOnResponseListener = listener
        return this
    }

    fun send() {
        val url = "$SERVER/xml-tools.php?key=$mKey&request=$mRequest"
        val queue: RequestQueue = XmpApplication.instance!!.requestQueue
        val jsObjRequest = StringRequest(url, this, this)
        queue.add(jsObjRequest)
    }

    override fun onErrorResponse(error: VolleyError) {
        e(TAG, "Volley error: " + error.message)
        mOnResponseListener!!.onHardError(HardErrorResponse(error))
    }

    override fun onResponse(result: String) {
        i(TAG, "Volley: get response")
        val response = xmlParse(result)
        if (response is SoftErrorResponse) {
            mOnResponseListener!!.onSoftError(response)
        } else {
            mOnResponseListener!!.onResponse(response)
        }
    }

    companion object {
        private const val TAG = "ModArchiveRequest"
        private const val SERVER = "https://api.modarchive.org"
        const val ARTIST = "search_artist&query="
        const val ARTIST_MODULES = "view_modules_by_artistid&query="
        const val MODULE = "view_by_moduleid&query="
        const val RANDOM = "random"
        const val FILENAME_OR_TITLE = "search&type=filename_or_songtitle&query="
    }
}
