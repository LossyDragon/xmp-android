package org.helllabs.android.xmp.modarchive.result

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.modarchive.adapter.ArtistArrayAdapter
import org.helllabs.android.xmp.modarchive.request.ArtistRequest
import org.helllabs.android.xmp.modarchive.request.ModArchiveRequest
import org.helllabs.android.xmp.modarchive.request.ModArchiveRequest.OnResponseListener
import org.helllabs.android.xmp.modarchive.response.ArtistResponse
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse
import java.io.UnsupportedEncodingException

class ArtistResult : Result(), OnResponseListener, AdapterView.OnItemClickListener {

    private var list: ListView? = null
    private var errorMessage: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result_list)
        setupCrossfade()
        setTitle(R.string.search_artist_title)
        list = findViewById<View>(R.id.result_list) as ListView
        list!!.onItemClickListener = this
        errorMessage = findViewById<View>(R.id.error_message) as TextView
        val searchText = intent.getStringExtra(Search.SEARCH_TEXT)
        try {
            val request = ArtistRequest(apiKey, ModArchiveRequest.ARTIST, searchText)
            request.setOnResponseListener(this).send()
        } catch (e: UnsupportedEncodingException) {
            handleQueryError()
        }
    }

    override fun onResponse(response: ModArchiveResponse) {
        val artistList = response as ArtistResponse
        val adapter = ArtistArrayAdapter(this, android.R.layout.simple_list_item_1, artistList.getList())
        list!!.adapter = adapter
        if (artistList.isEmpty) {
            errorMessage!!.setText(R.string.search_no_result)
            list!!.visibility = View.GONE
        }
        crossfade()
    }

    override fun onSoftError(response: SoftErrorResponse) {
        val errorMessage = findViewById<View>(R.id.error_message) as TextView
        errorMessage.text = response.message
        list!!.visibility = View.GONE
        crossfade()
    }

    override fun onHardError(response: HardErrorResponse) {
        handleError(response.error)
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val adapter = parent.adapter as ArtistArrayAdapter
        val intent = Intent(this, ArtistModulesResult::class.java)
        intent.putExtra(Search.ARTIST_ID, adapter.getItem(position)!!.id)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}