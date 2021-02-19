package org.helllabs.android.xmp.modarchive.result

import android.content.*
import android.os.Bundle
import android.view.*
import android.widget.*
import java.io.UnsupportedEncodingException
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

class TitleResult : Result(), OnResponseListener, AdapterView.OnItemClickListener {

    private var context: Context? = null
    private var list: ListView? = null
    private var errorMessage: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result_list)
        setupCrossfade()
        setTitle(R.string.search_title_title)
        context = this
        list = findViewById<View>(R.id.result_list) as ListView
        list!!.onItemClickListener = this
        errorMessage = findViewById<View>(R.id.error_message) as TextView
        val searchText = intent.getStringExtra(Search.SEARCH_TEXT)
        try {
            val request = ModuleRequest(apiKey, ModArchiveRequest.FILENAME_OR_TITLE, searchText)
            request.setOnResponseListener(this).send()
        } catch (e: UnsupportedEncodingException) {
            handleQueryError()
        }
    }

    override fun onResponse(response: ModArchiveResponse) {
        val moduleList = response as ModuleResponse
        val adapter = ModuleArrayAdapter(context!!, R.layout.search_list_item, moduleList.getList())
        list!!.adapter = adapter
        if (moduleList.isEmpty) {
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
        val adapter = parent.adapter as ModuleArrayAdapter
        val intent = Intent(this, ModuleResult::class.java)
        intent.putExtra(Search.MODULE_ID, adapter.getItem(position)!!.id)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}
