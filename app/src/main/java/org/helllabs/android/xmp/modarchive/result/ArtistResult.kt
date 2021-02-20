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
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.SEARCH_TEXT
import org.helllabs.android.xmp.modarchive.SearchError
import org.helllabs.android.xmp.modarchive.adapter.ArtistAdapter
import org.helllabs.android.xmp.modarchive.result.ArtistResultViewModel.ArtistState
import org.helllabs.android.xmp.model.ArtistResult
import org.helllabs.android.xmp.util.hide
import org.helllabs.android.xmp.util.show

@AndroidEntryPoint
class ArtistResult : AppCompatActivity(), ArtistAdapter.ArtistAdapterListener {

    private lateinit var artistAdapter: ArtistAdapter
    private val viewModel: ArtistResultViewModel by viewModels()

    private lateinit var appBarText: TextView
    private lateinit var resultList: RecyclerView
    private lateinit var resultSpinner: ProgressBar
    private lateinit var errorMessage: TextView
    private lateinit var errorLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_result_list)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        appBarText = findViewById(R.id.toolbarText)
        resultList = findViewById(R.id.result_list)
        resultSpinner = findViewById(R.id.result_spinner)
        errorMessage = findViewById(R.id.message)
        errorLayout = findViewById(R.id.layout)

        appBarText.text = getString(R.string.search_artist_title)

        artistAdapter = ArtistAdapter()
        artistAdapter.artistAdapterListener = this

        resultList.apply {
            layoutManager = LinearLayoutManager(this@ArtistResult)
            adapter = artistAdapter
            addItemDecoration(
                DividerItemDecoration(
                    this@ArtistResult,
                    LinearLayoutManager.HORIZONTAL
                )
            )
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

    override fun onClick(id: Int) {
        val intent = Intent(this, SearchListResult::class.java)
        intent.putExtra(ARTIST_ID, id)
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

    private fun onResult(result: ArtistResult) {
        resultList.show()
        resultSpinner.hide()
        artistAdapter.submitList(result.items)
    }
}
