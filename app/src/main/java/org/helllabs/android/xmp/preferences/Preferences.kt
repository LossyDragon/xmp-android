package org.helllabs.android.xmp.preferences

import android.os.Bundle
import android.os.Environment
import android.os.Environment.MEDIA_MOUNTED
import android.os.Environment.MEDIA_MOUNTED_READ_ONLY
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.util.logE

class Preferences : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.pref_layout)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val appbar = findViewById<TextView>(R.id.toolbarText)
        appbar.text = getString(R.string.pref_category_preferences)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, PreferencesFragment())
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        @Suppress("DEPRECATION") // Not using SAF yet
        private val SD_DIR: File = Environment.getExternalStorageDirectory()

        val DATA_DIR = File(SD_DIR, "Xmp for Android")
        val CACHE_DIR = File(SD_DIR, "Android/data/org.helllabs.android.xmp/cache/")
        val DEFAULT_MEDIA_PATH = "$SD_DIR/mod"

        fun checkStorage(): Boolean {
            val state = Environment.getExternalStorageState()
            return if (MEDIA_MOUNTED == state || MEDIA_MOUNTED_READ_ONLY == state) {
                true
            } else {
                logE("External storage state error: $state")
                false
            }
        }
    }
}
