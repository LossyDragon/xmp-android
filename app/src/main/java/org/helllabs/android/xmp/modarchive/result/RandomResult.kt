package org.helllabs.android.xmp.modarchive.result

import android.os.Bundle
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.modarchive.request.ModArchiveRequest
import org.helllabs.android.xmp.modarchive.request.ModArchiveRequest.OnResponseListener
import org.helllabs.android.xmp.modarchive.request.ModuleRequest

class RandomResult : ModuleResult(), OnResponseListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.search_random_title)
    }

    override fun makeRequest(query: String?) {
        val request = ModuleRequest(apiKey, ModArchiveRequest.RANDOM)
        request.setOnResponseListener(this).send()
    }
}
