package org.helllabs.android.xmp.ui.modarchive.result

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.databinding.ActivityResultListBinding
import org.helllabs.android.xmp.databinding.ItemSingleBinding
import org.helllabs.android.xmp.model.ArtistResult
import org.helllabs.android.xmp.model.Item
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.ARTIST_ID
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.ERROR
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.SEARCH_TEXT
import org.helllabs.android.xmp.ui.modarchive.SearchError
import org.helllabs.android.xmp.ui.modarchive.adapter.ArtistDiffUtil
import org.helllabs.android.xmp.ui.modarchive.adapter.ModAdapter
import org.helllabs.android.xmp.ui.modarchive.result.ArtistResultViewModel.ArtistState
import org.helllabs.android.xmp.util.hide
import org.helllabs.android.xmp.util.show

@AndroidEntryPoint
class ArtistResult : AppCompatActivity() {

    private lateinit var binder: ActivityResultListBinding

    private lateinit var artistAdapter: ModAdapter<Item, ItemSingleBinding>
    private val viewModel: ArtistResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder = ActivityResultListBinding.inflate(layoutInflater)

        setContentView(binder.root)
        setSupportActionBar(binder.appbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        artistAdapter = ModAdapter(
            ArtistDiffUtil(),
            R.layout.item_single
        ) { item ->
            val intent = Intent(this, SearchListResult::class.java)
            intent.putExtra(ARTIST_ID, item.id)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            startActivity(intent)
        }

        with(binder) {
            appbar.toolbarText.text = getString(R.string.search_artist_title)
            resultList.adapter = artistAdapter
        }

        lifecycleScope.launchWhenStarted {
            viewModel.artistState.collect {
                when (it) {
                    ArtistState.None -> Unit
                    ArtistState.Load -> onLoad()
                    is ArtistState.Error -> onError(it.error)
                    is ArtistState.SoftError -> onSoftError(it.softError)
                    is ArtistState.SearchResult -> onResult(it.result)
                }
            }
        }

        viewModel.fetchArtists(intent.getStringExtra(SEARCH_TEXT)!!)
    }

    private fun onLoad() {
        binder.apply {
            resultSpinner.show()
            resultList.hide()
            errorLayout.layout.hide()
        }
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
        binder.apply {
            resultSpinner.hide()
            resultList.hide()
            errorLayout.layout.show()
            errorLayout.message.text = softError
        }
    }

    private fun onResult(result: ArtistResult) {
        binder.resultList.show()
        binder.resultSpinner.hide()
        artistAdapter.submitList(result.items)
    }
}
