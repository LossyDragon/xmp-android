package org.helllabs.android.xmp.modarchive

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.databinding.ActivityResultListBinding
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.modarchive.adapter.HistoryAdapter
import org.helllabs.android.xmp.modarchive.result.ModuleResult
import org.helllabs.android.xmp.model.History
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.util.hide
import org.helllabs.android.xmp.util.show

class SearchHistory : AppCompatActivity(), HistoryAdapter.HistoryAdapterListener {

    private lateinit var binder: ActivityResultListBinding
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder = ActivityResultListBinding.inflate(layoutInflater)

        setContentView(binder.root)
        setSupportActionBar(binder.appbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binder.appbar.toolbarText.text = getString(R.string.search_history)

        historyAdapter = HistoryAdapter()
        historyAdapter.historyListener = this
        binder.resultSpinner.hide()
        binder.resultList.apply {
            layoutManager = LinearLayoutManager(this@SearchHistory)
            adapter = historyAdapter
            addItemDecoration(
                DividerItemDecoration(
                    this@SearchHistory,
                    LinearLayoutManager.HORIZONTAL
                )
            )
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

    override fun onClick(id: Int) {
        val intent = Intent(this, ModuleResult::class.java)
        intent.putExtra(MODULE_ID, id)
        startActivity(intent)
    }

    private fun refreshList() {
        historyAdapter.submitList(getHistory().sortedBy { it.visitDate })

        if (historyAdapter.historySet.isEmpty()) {
            binder.resultList.hide()
            binder.errorLayout.layout.show()
            binder.errorLayout.message.text = getString(R.string.history_no_items)
        } else {
            binder.resultList.show()
            binder.errorLayout.layout.hide()
        }
    }

    private fun getHistory(): List<History> {
        val type = object : TypeToken<List<History?>?>() {}.type
        return Gson().fromJson<List<History>>(PrefManager.searchHistory, type).orEmpty()
    }

    private fun deleteHistory() {
        MaterialDialog(this).show {
            title(text = "Clear search history")
            message(text = "Are you want to clear your module search history?")
            positiveButton(R.string.result_delete) {
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
