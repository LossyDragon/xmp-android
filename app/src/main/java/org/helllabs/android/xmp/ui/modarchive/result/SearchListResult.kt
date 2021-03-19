package org.helllabs.android.xmp.ui.modarchive.result

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.databinding.ActivityResultListBinding
import org.helllabs.android.xmp.databinding.ItemSearchListBinding
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.SearchListResult
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.ARTIST_ID
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.ERROR
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.SEARCH_TEXT
import org.helllabs.android.xmp.ui.modarchive.SearchError
import org.helllabs.android.xmp.ui.modarchive.adapter.ModAdapter
import org.helllabs.android.xmp.ui.modarchive.adapter.SearchDiffUtil
import org.helllabs.android.xmp.ui.modarchive.result.SearchListViewModel.SearchResultState
import org.helllabs.android.xmp.util.hide
import org.helllabs.android.xmp.util.show

class SearchListResult : AppCompatActivity() {

    private lateinit var binder: ActivityResultListBinding
    private lateinit var searchListAdapter: ModAdapter<Module, ItemSearchListBinding>
    private val viewModel: SearchListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder = ActivityResultListBinding.inflate(layoutInflater)

        setContentView(binder.root)
        setSupportActionBar(binder.appbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        searchListAdapter = ModAdapter(
            SearchDiffUtil(),
            R.layout.item_search_list
        ) { item ->
            val intent = Intent(this, ModuleResult::class.java)
            intent.putExtra(MODULE_ID, item.id)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            startActivity(intent)
        }

        binder.resultList.adapter = searchListAdapter

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
            binder.appbar.toolbarText.text = getString(R.string.search_title_title)
            viewModel.getFileOrTitle(it)
        }

        intent.getIntExtra(ARTIST_ID, -1).let {
            if (it < 0) return@let
            binder.appbar.toolbarText.text = getString(R.string.search_artist_modules_title)
            viewModel.getArtistById(it)
        }
    }

    private fun onLoad() {
        binder.resultSpinner.show()
        binder.resultList.hide()
        binder.errorLayout.layout.hide()
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
        binder.resultSpinner.hide()
        binder.resultList.hide()
        binder.errorLayout.layout.show()
        binder.errorLayout.message.text = softError
    }

    private fun onResult(result: SearchListResult) {
        binder.resultList.show()
        binder.resultSpinner.hide()
        searchListAdapter.submitList(result.module)
    }
}
