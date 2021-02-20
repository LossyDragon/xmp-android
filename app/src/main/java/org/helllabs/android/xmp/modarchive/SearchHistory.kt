package org.helllabs.android.xmp.modarchive

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.modarchive.adapter.HistoryAdapter
import org.helllabs.android.xmp.modarchive.result.ModuleResult
import org.helllabs.android.xmp.model.History
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.util.hide
import org.helllabs.android.xmp.util.show

class SearchHistory : AppCompatActivity(), HistoryAdapter.HistoryAdapterListener {

    private lateinit var historyAdapter: HistoryAdapter

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

        appBarText.text = getString(R.string.search_history)

        historyAdapter = HistoryAdapter()
        historyAdapter.historyListener = this
        resultSpinner.hide()
        resultList.apply {
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
            resultList.hide()
            errorLayout.show()
            errorMessage.text = getString(R.string.history_no_items)
        } else {
            resultList.show()
            errorLayout.hide()
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
