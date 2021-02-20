package org.helllabs.android.xmp.modarchive.result

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.ARTIST_ID
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.ERROR
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.SEARCH_TEXT
import org.helllabs.android.xmp.modarchive.SearchError
import org.helllabs.android.xmp.modarchive.adapter.SearchListAdapter
import org.helllabs.android.xmp.modarchive.result.SearchListViewModel.SearchResultState
import org.helllabs.android.xmp.model.SearchListResult
import org.helllabs.android.xmp.util.hide
import org.helllabs.android.xmp.util.show

@AndroidEntryPoint
class SearchListResult : AppCompatActivity(), SearchListAdapter.SearchListListener {

    private lateinit var searchListAdapter: SearchListAdapter
    private val viewModel: SearchListViewModel by viewModels()

    private lateinit var appBarText: TextView
    private lateinit var errorMessage: TextView
    private lateinit var errorLayout: LinearLayout
    private lateinit var resultSpinner: ProgressBar
    private lateinit var resultList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_result_list)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        appBarText = findViewById(R.id.toolbarText)
        resultSpinner = findViewById(R.id.result_spinner)
        resultList = findViewById(R.id.result_list)
        errorMessage = findViewById(R.id.message)
        errorLayout = findViewById(R.id.layout)

        searchListAdapter = SearchListAdapter()
        searchListAdapter.searchListListener = this

        resultList.apply {
            layoutManager = LinearLayoutManager(this@SearchListResult)
            adapter = searchListAdapter
            addItemDecoration(
                DividerItemDecoration(
                    this@SearchListResult,
                    LinearLayoutManager.HORIZONTAL
                )
            )
        }

        lifecycleScope.launchWhenStarted {
            viewModel.searchResultState.collect {
                when (it) {
                    SearchResultState.None -> Unit
                    SearchResultState.Load -> onLoad()
                    is SearchResultState.Error -> onError(it.error)
                    is SearchResultState.SoftError -> onSoftError(it.softError)
                    is SearchResultState.SearchResult -> onResult(it.result)
                }
            }
        }

        intent.getStringExtra(SEARCH_TEXT)?.let {
            appBarText.text = getString(R.string.search_title_title)
            viewModel.getFileOrTitle(it)
        }

        intent.getIntExtra(ARTIST_ID, -1).let {
            if (it < 0) return@let
            appBarText.text = getString(R.string.search_artist_modules_title)
            viewModel.getArtistById(it)
        }
    }

    override fun onClick(id: Int) {
        val intent = Intent(this, ModuleResult::class.java)
        intent.putExtra(MODULE_ID, id)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        startActivity(intent)
    }

    private fun onLoad() {
        resultSpinner.show()
        resultList.hide()
        errorLayout.hide()
    }

    private fun onError(error: String?) {
        val message = error ?: getString(R.string.search_unknown_error)
        val intent = Intent(this, SearchError::class.java)
        intent.putExtra(ERROR, message)
        intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        overridePendingTransition(0, 0)
        startActivity(intent)
    }

    private fun onSoftError(softError: String) {
        resultSpinner.hide()
        resultList.hide()
        errorLayout.show()
        errorMessage.text = softError
    }

    private fun onResult(result: SearchListResult) {
        resultList.show()
        resultSpinner.hide()
        searchListAdapter.submitList(result.module)
    }
}
