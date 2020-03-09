package org.helllabs.android.xmp.preferences

import android.os.Bundle
import android.os.Environment
import android.os.Environment.MEDIA_MOUNTED
import android.os.Environment.MEDIA_MOUNTED_READ_ONLY
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.extension.fatalError
import org.helllabs.android.xmp.extension.isAtMostN
import org.helllabs.android.xmp.util.Log
import java.io.File

class Preferences : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.pref_layout)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, PreferencesFragment())
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {

            // Known issue with API <= 24 for themes
            // https://android-review.googlesource.com/c/platform/frameworks/support/+/971248
            // https://issuetracker.google.com/issues/131851825
            if (isAtMostN() && isThemeChanged) {
                fatalError(R.string.change_theme_older_apis)
            } else {
                onBackPressed()
            }
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        private val TAG = Preferences::class.java.simpleName

        fun checkStorage(): Boolean {
            val state = Environment.getExternalStorageState()

            return if (MEDIA_MOUNTED == state || MEDIA_MOUNTED_READ_ONLY == state) {
                true
            } else {
                Log.e(TAG, "External storage state error: $state")
                false
            }
        }

        @Volatile
        var isThemeChanged: Boolean = false

        // SAF absolutely sucks. Keep until that `pile` is better documented.
        @Suppress("DEPRECATION")
        internal val SD_DIR: File = Environment.getExternalStorageDirectory()
        val DATA_DIR: File = File(SD_DIR, "Xmp for Android")
        val CACHE_DIR: File = File(SD_DIR, "Android/data/org.helllabs.android.xmp/cache/")
        val DEFAULT_MEDIA_PATH = "$SD_DIR/mod"
    }
}
