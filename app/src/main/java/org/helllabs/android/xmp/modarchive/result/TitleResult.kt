package org.helllabs.android.xmp.modarchive.result

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import kotlinx.android.synthetic.main.result_list.*
import kotlinx.android.synthetic.main.activity_search_error.*
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.modarchive.adapter.ModuleArrayAdapter
import org.helllabs.android.xmp.modarchive.request.ModArchiveRequest
import org.helllabs.android.xmp.modarchive.request.ModuleRequest
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse
import org.helllabs.android.xmp.modarchive.response.ModuleResponse
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse
import java.io.UnsupportedEncodingException

class TitleResult :
        Result(),
        ModArchiveRequest.OnResponseListener,
        AdapterView.OnItemClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result_list)
        setupCrossfade()

        setTitle(R.string.search_title_title)

        result_list.onItemClickListener = this

        val searchText = intent.getStringExtra(Search.SEARCH_TEXT)!!
        val key = BuildConfig.ApiKey

        try {
            val request = ModuleRequest(key, ModArchiveRequest.FILENAME_OR_TITLE, searchText)
            request.setOnResponseListener(this).send(this)
        } catch (e: UnsupportedEncodingException) {
            handleQueryError()
        }
    }

    override fun onResponse(response: ModArchiveResponse) {
        val moduleList = response as ModuleResponse
        val adapter = ModuleArrayAdapter(this, R.layout.item_search, moduleList.list)
        result_list!!.adapter = adapter

        if (moduleList.isEmpty) {
            error_message!!.setText(R.string.search_no_result)
            result_list!!.visibility = View.GONE
        }
        crossfade()
    }

    override fun onSoftError(response: SoftErrorResponse) {
        error_message.text = response.message
        result_list!!.visibility = View.GONE
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
