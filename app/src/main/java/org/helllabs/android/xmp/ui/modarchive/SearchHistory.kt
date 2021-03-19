package org.helllabs.android.xmp.ui.modarchive

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.databinding.ActivityResultListBinding
import org.helllabs.android.xmp.databinding.ItemSearchListBinding
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.ui.modarchive.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.ui.modarchive.adapter.ModAdapter
import org.helllabs.android.xmp.ui.modarchive.adapter.SearchDiffUtil
import org.helllabs.android.xmp.ui.modarchive.result.ModuleResult
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.util.hide
import org.helllabs.android.xmp.util.show

@AndroidEntryPoint
class SearchHistory : AppCompatActivity() {

    @Inject
    lateinit var moshiAdapter: JsonAdapter<List<Module>>

    private lateinit var binder: ActivityResultListBinding
    private lateinit var historyAdapter: ModAdapter<Module, ItemSearchListBinding>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder = ActivityResultListBinding.inflate(layoutInflater)

        setContentView(binder.root)
        setSupportActionBar(binder.appbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        historyAdapter = ModAdapter(
            SearchDiffUtil(),
            R.layout.item_search_list
        ) { item ->
            val intent = Intent(this, ModuleResult::class.java)
            intent.putExtra(MODULE_ID, item.id)
            startActivity(intent)
        }

        with(binder) {
            appbar.toolbarText.text = getString(R.string.search_history)
            resultSpinner.hide()
            resultList.adapter = historyAdapter
        }

        refreshList()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_history, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_history_delete) {
            deleteHistory()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refreshList() {
        historyAdapter.submitList(getHistory().reversed()) // Oldest to bottom

        if (historyAdapter.currentList.isEmpty()) {
            binder.resultList.hide()
            binder.errorLayout.layout.show()
            binder.errorLayout.message.text = getString(R.string.history_no_items)
        } else {
            binder.resultList.show()
            binder.errorLayout.layout.hide()
        }
    }

    private fun getHistory(): List<Module> {
        return PrefManager.searchHistory?.let {
            moshiAdapter.fromJson(it)
        }.orEmpty()
    }

    private fun deleteHistory() {
        MaterialDialog(this).show {
            title(text = "Clear search history")
            message(text = "Are you want to clear your module search history?")
            positiveButton(R.string.delete) {
                PrefManager.clearSearchHistory()
                refreshList()
            }
            negativeButton(R.string.cancel)
        }
    }

    companion object {
        const val HISTORY_LENGTH = 50
    }
}
