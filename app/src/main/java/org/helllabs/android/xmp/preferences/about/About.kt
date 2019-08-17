package org.helllabs.android.xmp.preferences.about


import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.pref_item_about.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp

class About : AppCompatActivity() {

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setContentView(R.layout.pref_item_about)

        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close)
        }

        version_name.text = getString(R.string.about_version, AppInfo.getVersion(this))
        xmp_version.text = getString(R.string.about_xmp, Xmp.getVersion())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}
