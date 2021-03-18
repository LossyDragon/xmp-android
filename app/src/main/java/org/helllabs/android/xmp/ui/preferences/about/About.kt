package org.helllabs.android.xmp.ui.preferences.about

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.databinding.PrefAboutBinding

class About : AppCompatActivity() {

    private lateinit var binder: PrefAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder = PrefAboutBinding.inflate(layoutInflater)

        setContentView(binder.root)
        setSupportActionBar(binder.appbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binder.appbar.toolbarText.text = getString(R.string.pref_about_title)
        binder.versionName.text = getString(R.string.about_version, BuildConfig.VERSION_NAME)
        binder.xmpVersion.text = getString(R.string.about_xmp, Xmp.getVersion())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
