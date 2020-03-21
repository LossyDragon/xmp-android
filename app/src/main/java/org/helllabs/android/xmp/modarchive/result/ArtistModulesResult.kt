package org.helllabs.android.xmp.modarchive.result

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import kotlinx.android.synthetic.main.activity_search_error.*
import kotlinx.android.synthetic.main.result_list.*
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.extension.intent
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.modarchive.adapter.ModuleArrayAdapter
import org.helllabs.android.xmp.modarchive.request.ModArchiveRequest
import org.helllabs.android.xmp.modarchive.request.ModuleRequest
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse
import org.helllabs.android.xmp.modarchive.response.ModuleResponse
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse
import java.io.UnsupportedEncodingException

class ArtistModulesResult :
        Result(),
        ModArchiveRequest.OnResponseListener,
        AdapterView.OnItemClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result_list)
        setupCrossfade()

        setTitle(R.string.search_artist_modules_title)

        result_list.onItemClickListener = this

        val artistId = intent.getLongExtra(Search.ARTIST_ID, -1)
        val key = BuildConfig.ApiKey

        try {
            val request = ModuleRequest(key, ModArchiveRequest.ARTIST_MODULES, artistId)
            request.setOnResponseListener(this).send(this)
        } catch (e: UnsupportedEncodingException) {
            handleQueryError()
        }
    }

    override fun onResponse(response: ModArchiveResponse) {
        val moduleList = response as ModuleResponse
        val adapter = ModuleArrayAdapter(this, R.layout.item_search, moduleList.list)
        result_list.adapter = adapter

        if (moduleList.isEmpty) {
            error_message!!.setText(R.string.search_artist_no_mods)
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
        val adapter = parent.adapter as ModuleArrayAdapter
        startActivity(
                intent(ModuleResult::class.java).apply {
                    putExtra(Search.MODULE_ID, adapter.getItem(position)!!.id)
                })
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}
