package org.helllabs.android.xmp.modarchive

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.appbar.MaterialToolbar
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.ERROR

class SearchError : AppCompatActivity(), Runnable {

    private var frameBlink: Boolean = false

    private lateinit var appBarText: TextView
    private lateinit var errorMessage: TextView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_search_error)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        appBarText = findViewById(R.id.toolbarText)
        errorMessage = findViewById(R.id.error_message)

        appBarText.text = getString(R.string.search_title_error)

        val error = intent.getStringExtra(ERROR)

        var message = error
        if (message == null) {
            message = getString(R.string.search_unknown_error)
        } else {
            // Remove java exception stuff
            val idx = message.indexOf("Exception: ")
            if (idx >= 0) {
                message = message.substring(idx + 11)
            }
            message = if (message.trim { it <= ' ' }.isEmpty()) {
                getString(R.string.search_unknown_error)
            } else {
                val err = message.substring(0, 1).toUpperCase(Locale.US) + message.substring(1)
                getString(R.string.search_known_error, err)
            }
        }

        val font = ResourcesCompat.getFont(applicationContext, R.font.font_topaz_plus_a500)
        errorMessage.text = message
        errorMessage.typeface = font
    }

    override fun onResume() {
        super.onResume()
        errorMessage.postDelayed(this, BLINK_PERIOD)
    }

    override fun onPause() {
        super.onPause()
        errorMessage.removeCallbacks(this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Back key returns to search
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val intent = Intent(this, Search::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            overridePendingTransition(0, 0)
            startActivity(intent)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // Guru frame blink
    override fun run() {
        errorMessage.apply {
            background.alpha = if (frameBlink) 255 else 0
            postDelayed(this@SearchError, BLINK_PERIOD)
        }

        frameBlink = !frameBlink
    }

    companion object {
        private const val BLINK_PERIOD = 1337L
    }
}
