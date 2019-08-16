package org.helllabs.android.xmp.modarchive

import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.modarchive.result.ArtistResult
import org.helllabs.android.xmp.modarchive.result.RandomResult
import org.helllabs.android.xmp.modarchive.result.TitleResult

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Search : AppCompatActivity(), TextView.OnEditorActionListener {
    private var searchType: RadioGroup? = null
    private var searchEdit: EditText? = null
    private var context: Context? = null

    private val searchClick = View.OnClickListener { performSearch() }

    private val randomClick = View.OnClickListener {
        startActivity(Intent(context, RandomResult::class.java))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search)

        setTitle(R.string.search_title)
        context = this

        val searchButton = findViewById(R.id.search_button) as Button
        val randomButton = findViewById(R.id.random_button) as Button

        searchType = findViewById(R.id.search_type) as RadioGroup
        searchType!!.check(R.id.title_radio)

        searchButton.setOnClickListener(searchClick)
        randomButton.setOnClickListener(randomClick)

        searchEdit = findViewById(R.id.search_text) as EditText
        searchEdit!!.setOnEditorActionListener(this)
    }

    override fun onResume() {
        super.onResume()

        // Show soft keyboard
        searchEdit!!.requestFocus()
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onEditorAction(view: TextView, actionId: Int, event: KeyEvent): Boolean {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            performSearch()
            return true
        }
        return false
    }

    private fun performSearch() {
        val selectedId = searchType!!.checkedRadioButtonId
        val searchText = searchEdit!!.text.toString().trim { it <= ' ' }

        val intent: Intent

        when (selectedId) {
            R.id.title_radio -> {
                intent = Intent(context, TitleResult::class.java)
                intent.putExtra(SEARCH_TEXT, searchText)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
            R.id.artist_radio -> {
                intent = Intent(context, ArtistResult::class.java)
                intent.putExtra(SEARCH_TEXT, searchText)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
            else -> {
            }
        }
    }

    companion object {

        val SEARCH_TEXT = "search_text"
        val MODULE_ID = "module_id"
        val ARTIST_ID = "artist_id"
        val ERROR = "error"
    }

}
