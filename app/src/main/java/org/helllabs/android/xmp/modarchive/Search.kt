package org.helllabs.android.xmp.modarchive

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics.DENSITY_HIGH
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.MODULE_ID
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.SEARCH_TEXT
import org.helllabs.android.xmp.modarchive.result.ArtistResult
import org.helllabs.android.xmp.modarchive.result.ModuleResult
import org.helllabs.android.xmp.modarchive.result.SearchListResult

class Search : AppCompatActivity(), TextView.OnEditorActionListener {

    private var canSearch: Boolean = false

    private lateinit var searchInput: TextInputEditText
    private lateinit var radioGroup: RadioGroup
    private lateinit var appBarText: TextView
    private lateinit var searchButton: MaterialButton
    private lateinit var randomButton: MaterialButton
    private lateinit var historyButton: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_search)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        appBarText = findViewById(R.id.toolbarText)
        searchInput = findViewById(R.id.search_edit_text)
        radioGroup = findViewById(R.id.search_radio_group)
        searchButton = findViewById(R.id.search_search_button)
        randomButton = findViewById(R.id.search_random_button)
        historyButton = findViewById(R.id.search_history_button)

        appBarText.text = getString(R.string.search_title)
        radioGroup.check(R.id.search_title_radio_button)
        searchButton.setOnClickListener { performSearch() }
        randomButton.setOnClickListener { performRandomSearch() }
        historyButton.setOnClickListener { showHistory() }
        searchInput.doOnTextChanged { text, _, _, _ ->
            canSearch = text!!.count() >= 3
            searchButton.isEnabled = canSearch
        }

        // Smaller screens will wrap the text of these buttons.
        // We'll remove the buttons, but keep the text
        if (resources.displayMetrics.densityDpi <= DENSITY_HIGH) {
            searchButton.icon = null
            randomButton.icon = null
        }
    }

    public override fun onResume() {
        super.onResume()

        // Show soft keyboard
        searchInput.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onPause() {
        super.onPause()
        searchInput.clearFocus()
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
        val searchText = searchInput.text.toString().trim { it <= ' ' }

        when (radioGroup.checkedRadioButtonId) {
            R.id.search_title_radio_button -> {
                val intent = Intent(this, SearchListResult::class.java)
                intent.putExtra(SEARCH_TEXT, searchText)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                startActivity(intent)
            }
            R.id.search_artist_radio_button -> {
                val intent = Intent(this, ArtistResult::class.java)
                intent.putExtra(SEARCH_TEXT, searchText)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                startActivity(intent)
            }
        }
    }
}
