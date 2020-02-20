package org.helllabs.android.xmp.modarchive.result

import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.modarchive.request.ModuleRequest

import android.os.Bundle
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.modarchive.request.ModArchiveRequest

class RandomResult : ModuleResult(), ModArchiveRequest.OnResponseListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.search_random_title)
    }

    override fun makeRequest(query: String) {
        val key = BuildConfig.ApiKey
        val request = ModuleRequest(key, ModArchiveRequest.RANDOM)
        request.setOnResponseListener(this).send(xmpApplication().requestQueue)
    }
}
