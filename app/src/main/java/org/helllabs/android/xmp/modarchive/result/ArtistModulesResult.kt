package org.helllabs.android.xmp.modarchive.result

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.modarchive.adapter.ModuleArrayAdapter
import org.helllabs.android.xmp.modarchive.request.ModArchiveRequest
import org.helllabs.android.xmp.modarchive.request.ModArchiveRequest.OnResponseListener
import org.helllabs.android.xmp.modarchive.request.ModuleRequest
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse
import org.helllabs.android.xmp.modarchive.response.ModuleResponse
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse
import java.io.UnsupportedEncodingException

class ArtistModulesResult : Result(), OnResponseListener, AdapterView.OnItemClickListener {

    private var list: ListView? = null
    private var errorMessage: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result_list)
        setupCrossfade()
        setTitle(R.string.search_artist_modules_title)
        list = findViewById<View>(R.id.result_list) as ListView
        list!!.onItemClickListener = this
        errorMessage = findViewById<View>(R.id.error_message) as TextView
        val artistId = intent.getLongExtra(Search.ARTIST_ID, -1)
        try {
            val request = ModuleRequest(apiKey, ModArchiveRequest.ARTIST_MODULES, artistId)
            request.setOnResponseListener(this).send()
        } catch (e: UnsupportedEncodingException) {
            handleQueryError()
        }
    }

    override fun onResponse(response: ModArchiveResponse) {
        val moduleList = response as ModuleResponse
        val adapter = ModuleArrayAdapter(this, R.layout.search_list_item, moduleList.getList())
        list!!.adapter = adapter
        if (moduleList.isEmpty) {
            errorMessage!!.setText(R.string.search_artist_no_mods)
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
        val adapter = parent.adapter as ModuleArrayAdapter
        val intent = Intent(this, ModuleResult::class.java)
        intent.putExtra(Search.MODULE_ID, adapter.getItem(position)!!.id)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}