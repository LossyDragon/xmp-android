package org.helllabs.android.xmp.modarchive

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.databinding.ActivitySearchErrorBinding
import org.helllabs.android.xmp.modarchive.ModArchiveConstants.ERROR

class SearchError : AppCompatActivity(), Runnable {

    private lateinit var binder: ActivitySearchErrorBinding

    private var frameBlink: Boolean = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder = ActivitySearchErrorBinding.inflate(layoutInflater)

        setContentView(binder.root)
        setSupportActionBar(binder.appbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binder.appbar.toolbarText.text = getString(R.string.search_title_error)

        var message: String? = intent.getStringExtra(ERROR)
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

        binder.errorMessage.text = message
    }

    override fun onResume() {
        super.onResume()
        binder.errorMessage.postDelayed(this, BLINK_PERIOD)
    }

    override fun onPause() {
        super.onPause()
        binder.errorMessage.removeCallbacks(this)
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
        binder.errorMessage.apply {
            background.alpha = if (frameBlink) 255 else 0
            postDelayed(this@SearchError, BLINK_PERIOD)
        }

        frameBlink = !frameBlink
    }

    companion object {
        private const val BLINK_PERIOD = 1337L
    }
}
