package org.helllabs.android.xmp.modarchive.result

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import kotlinx.android.synthetic.main.layout_error.*
import kotlinx.android.synthetic.main.result_list.*
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.extension.intent
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.modarchive.adapter.ArtistArrayAdapter
import org.helllabs.android.xmp.modarchive.request.ArtistRequest
import org.helllabs.android.xmp.modarchive.request.ModArchiveRequest
import org.helllabs.android.xmp.modarchive.response.ArtistResponse
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse
import java.io.UnsupportedEncodingException

class ArtistResult :
        Result(),
        ModArchiveRequest.OnResponseListener,
        AdapterView.OnItemClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result_list)
        setupCrossfade()

        setTitle(R.string.search_artist_title)

        result_list!!.onItemClickListener = this

        val searchText = intent.getStringExtra(Search.SEARCH_TEXT)!!
        val key = BuildConfig.ApiKey

        try {
            val request = ArtistRequest(key, ModArchiveRequest.ARTIST, searchText)
            request.setOnResponseListener(this).send(this)
        } catch (e: UnsupportedEncodingException) {
            handleQueryError()
        }
    }

    override fun onResponse(response: ModArchiveResponse) {
        val artistList = response as ArtistResponse
        val adapter = ArtistArrayAdapter(this, android.R.layout.simple_list_item_1, artistList.list)
        result_list.adapter = adapter

        if (artistList.isEmpty) {
            error_message.setText(R.string.search_no_result)
            result_list.visibility = View.GONE
        }

        crossfade()
    }

    override fun onSoftError(response: SoftErrorResponse) {
        error_message.text = response.message
        result_list.visibility = View.GONE
        crossfade()
    }

    override fun onHardError(response: HardErrorResponse) {
        handleError(response.error)
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val adapter = parent.adapter as ArtistArrayAdapter
        startActivity(
                intent(ArtistModulesResult::class.java).apply {
                    putExtra(Search.ARTIST_ID, adapter.getItem(position)!!.id)
                })
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}
