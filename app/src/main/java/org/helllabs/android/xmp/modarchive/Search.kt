package org.helllabs.android.xmp.modarchive

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.search.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.modarchive.result.ArtistResult
import org.helllabs.android.xmp.modarchive.result.RandomResult
import org.helllabs.android.xmp.modarchive.result.TitleResult

class Search : AppCompatActivity(), TextView.OnEditorActionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search)

        setTitle(R.string.search_title)

        search_type.check(R.id.title_radio)
        search_text!!.setOnEditorActionListener(this)

        search_button.setOnClickListener { performSearch() }
        random_button.setOnClickListener {
            startActivity(Intent(this, RandomResult::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onResume() {
        super.onResume()

        // Show soft keyboard
        search_text!!.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onEditorAction(view: TextView, actionId: Int, event: KeyEvent): Boolean {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            performSearch()
            return true
        }

        return false
    }

    private fun performSearch() {
        val searchText = search_text!!.text.toString().trim { it <= ' ' }

        val intent: Intent

        when (search_type!!.checkedRadioButtonId) {
            R.id.title_radio -> {
                intent = Intent(this, TitleResult::class.java)
                intent.putExtra(SEARCH_TEXT, searchText)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
            R.id.artist_radio -> {
                intent = Intent(this, ArtistResult::class.java)
                intent.putExtra(SEARCH_TEXT, searchText)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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
