package org.helllabs.android.xmp.preferences.about


import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.pref_item_about.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.util.getChangelog

class About : AppCompatActivity() {

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setContentView(R.layout.pref_item_about)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close)
            title = getString(R.string.pref_about_title)
        }

        version_name.text = getString(R.string.about_version, getVersion())
        xmp_version.text = getString(R.string.about_xmp, Xmp.getVersion())
        text_changelog.text = getChangelog()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    //Get the app's version number
    private fun getVersion(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            var version = packageInfo.versionName
            val end = version.indexOf(' ')
            if (end > 0) {
                version = version.substring(0, end)
            }

            version
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }
}
