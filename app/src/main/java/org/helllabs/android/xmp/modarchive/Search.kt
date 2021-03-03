package org.helllabs.android.xmp.modarchive

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics.DENSITY_HIGH
import android.view.KeyEvent
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.databinding.ActivitySearchBinding
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.SEARCH_TEXT
import org.helllabs.android.xmp.modarchive.result.ArtistResult
import org.helllabs.android.xmp.modarchive.result.ModuleResult
import org.helllabs.android.xmp.modarchive.result.SearchListResult

class Search : AppCompatActivity(), TextView.OnEditorActionListener {

    private lateinit var binder: ActivitySearchBinding
    private var canSearch: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder = ActivitySearchBinding.inflate(layoutInflater)

        setContentView(binder.root)
        setSupportActionBar(binder.appbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binder.apply {
            appbar.toolbarText.text = getString(R.string.search_title)
            searchRadioGroup.check(R.id.search_title_radio_button)
            searchSearchButton.setOnClickListener { performSearch() }
            searchRandomButton.setOnClickListener { performRandomSearch() }
            searchHistoryButton.setOnClickListener { showHistory() }
            searchEditText.doOnTextChanged { text, _, _, _ ->
                canSearch = text!!.count() >= 3
                binder.searchSearchButton.isEnabled = canSearch
            }
        }

        // Smaller screens will wrap the text of these buttons.
        // We'll remove the buttons, but keep the text
        if (resources.displayMetrics.densityDpi <= DENSITY_HIGH) {
            binder.searchSearchButton.icon = null
            binder.searchRandomButton.icon = null
        }
    }

    public override fun onResume() {
        super.onResume()

        // Show soft keyboard
        binder.searchInputLayout.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onPause() {
        super.onPause()
        binder.searchInputLayout.clearFocus()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        return if (canSearch && actionId == EditorInfo.IME_ACTION_SEARCH) {
            performSearch()
            true
        } else {
            false
        }
    }

    private fun showHistory() {
        val intent = Intent(this, SearchHistory::class.java)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        startActivity(intent)
    }

    private fun performRandomSearch() {
        val intent = Intent(this, ModuleResult::class.java)
        intent.putExtra(MODULE_ID, -1)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        startActivity(intent)
    }

    private fun performSearch() {
        val searchText = binder.searchEditText.text.toString().trim { it <= ' ' }
        var intent: Intent? = null

        when (binder.searchRadioGroup.checkedRadioButtonId) {
            R.id.search_title_radio_button -> {
                intent = Intent(this, SearchListResult::class.java)
                    .putExtra(SEARCH_TEXT, searchText)
            }
            R.id.search_artist_radio_button -> {
                intent = Intent(this, ArtistResult::class.java)
                    .putExtra(SEARCH_TEXT, searchText)
            }
        }

        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        startActivity(intent)
    }
}
