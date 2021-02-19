package org.helllabs.android.xmp.preferences.about

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp

class About : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.pref_about)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val appbar = findViewById<TextView>(R.id.toolbarText)
        val appVersion = findViewById<TextView>(R.id.version_name)
        val xmpVersion = findViewById<TextView>(R.id.xmp_version)

        appbar.text = getString(R.string.pref_about_title)
        appVersion.text = getString(R.string.about_version, BuildConfig.VERSION_NAME)
        xmpVersion.text = getString(R.string.about_xmp, Xmp.getVersion())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
