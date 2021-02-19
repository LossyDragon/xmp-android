package org.helllabs.android.xmp.modarchive

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import org.helllabs.android.xmp.R

class SearchError : AppCompatActivity(), Runnable {

    private var msg: TextView? = null
    private var frameBlink = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 		// Hide the status bar
        //        if (Build.VERSION.SDK_INT < 16) {
        //            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //        } else {
        //        	final View decorView = getWindow().getDecorView();
        //        	decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        //        }

        setContentView(R.layout.search_error)
        title = "Search error"
        val error = intent.getSerializableExtra(Search.ERROR) as Throwable
        msg = findViewById<View>(R.id.error_message) as TextView
        // msg.getPaint().setAntiAlias(false);
        var message = error.message

        if (message == null) {
            message = UNKNOWN_ERROR
        } else {
            // Remove java exception stuff
            val idx = message.indexOf("Exception: ")
            if (idx >= 0) {
                message = message.substring(idx + 11)
            }
            message = if (message.trim { it <= ' ' }.isEmpty()) {
                UNKNOWN_ERROR
            } else {
                message.substring(0, 1).toUpperCase(Locale.US) + message.substring(1) +
                    ".  Press back button to continue."
            }
        }
        msg!!.text = message
        val typeface = Typeface.createFromAsset(assets, "fonts/TopazPlus_a500_v1.0.ttf")
        msg!!.typeface = typeface
        msg!!.postDelayed(this, PERIOD.toLong())
    }

    public override fun onDestroy() {
        msg!!.removeCallbacks(this)
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
        msg!!.setBackgroundDrawable(
            resources.getDrawable(
                if (frameBlink) R.drawable.guru_frame else R.drawable.guru_frame_2
            )
        )
        frameBlink = frameBlink xor true
        msg!!.postDelayed(this, PERIOD.toLong())
    }

    companion object {
        private const val PERIOD = 1337
        private const val UNKNOWN_ERROR = "Software Failure.   " +
            "Press back to continue.\n\nGuru Meditation #35068035.48454C50"
    }
}
