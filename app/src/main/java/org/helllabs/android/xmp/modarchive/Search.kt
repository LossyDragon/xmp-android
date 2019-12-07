package org.helllabs.android.xmp.modarchive

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_search.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.modarchive.result.ArtistResult
import org.helllabs.android.xmp.modarchive.result.RandomResult
import org.helllabs.android.xmp.modarchive.result.TitleResult

class Search : AppCompatActivity(), TextView.OnEditorActionListener {

    private val searchClick = View.OnClickListener { performSearch() }

    private val randomClick = View.OnClickListener {
        startActivity(Intent(this@Search, RandomResult::class.java))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setTitle(R.string.search_title)

        search_edit_text!!.setOnEditorActionListener(this)

        // Search cant be empty or less than 3 characters, let's stop a wasted event.
        search_edit_text.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                search_search_button.isEnabled = !(s.isNullOrEmpty() || s.length < 3)
            }
        })

        search_radio_group!!.check(R.id.search_title_radio_button)

        search_search_button.setOnClickListener(searchClick)

        search_random_button.setOnClickListener(randomClick)
    }

    public override fun onResume() {
        super.onResume()

        // Show soft keyboard
        search_input_layout!!.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onEditorAction(view: TextView, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            performSearch()
            return true
        }
        return false
    }

    private fun performSearch() {
        val searchText = search_edit_text!!.text.toString().trim { it <= ' ' }

        val intent: Intent

        when (search_radio_group!!.checkedRadioButtonId) {
            R.id.search_title_radio_button -> {
                intent = Intent(this@Search, TitleResult::class.java)
                intent.putExtra(SEARCH_TEXT, searchText)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
            R.id.search_artist_radio_button -> {
                intent = Intent(this@Search, ArtistResult::class.java)
                intent.putExtra(SEARCH_TEXT, searchText)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
            else -> {
            }
        }
    }

    companion object {

        const val SEARCH_TEXT = "search_text"
        const val MODULE_ID = "module_id"
        const val ARTIST_ID = "artist_id"
        const val ERROR = "error"
    }
}
