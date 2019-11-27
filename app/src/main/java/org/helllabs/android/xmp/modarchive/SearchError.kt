package org.helllabs.android.xmp.modarchive

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_search_error.*
import org.helllabs.android.xmp.R
import java.util.*


class SearchError : AppCompatActivity(), Runnable {

    private var frameBlink: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_search_error)
        setSupportActionBar(toolbar)

        val error = intent.getSerializableExtra(Search.ERROR) as Throwable

        var message: String? = error.message
        if (message == null) {
            message = getString(R.string.search_error_unknown)
        } else {
            // Remove java exception stuff
            val idx = message.indexOf("Exception: ")
            if (idx >= 0) {
                message = message.substring(idx + 11)
            }

            message = if (message.trim { it <= ' ' }.isEmpty()) {
                getString(R.string.search_error_unknown)
            } else {
                message.substring(0, 1)
                        .toUpperCase(Locale.US) + message.substring(1) + getString(R.string.search_error_back)
            }
        }

        error_message.apply {
            text = message
            typeface = Typeface.createFromAsset(assets, "fonts/TopazPlus_a500_v1.0.ttf")
            postDelayed(this@SearchError, PERIOD.toLong())
        }
    }

    override fun onDestroy() {
        error_message.removeCallbacks(this)
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {

        // Back key returns to search
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val intent = Intent(this, Search::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            overridePendingTransition(0, 0)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


    override fun run() {
        // Guru frame blink
        val blink = if (frameBlink) R.drawable.guru_frame else R.drawable.guru_frame_2

        error_message.apply {
            @Suppress("DEPRECATION")
            setBackgroundDrawable(resources.getDrawable(blink))
            postDelayed(this@SearchError, PERIOD.toLong())
        }

        frameBlink = frameBlink xor true
    }

    companion object {
        private const val PERIOD = 1337
    }
}
