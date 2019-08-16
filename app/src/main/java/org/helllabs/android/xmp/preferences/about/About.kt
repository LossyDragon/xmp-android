package org.helllabs.android.xmp.preferences.about

import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp


import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.pref_about.*

class About : AppCompatActivity() {

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setContentView(R.layout.pref_about)

        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_close)

        version_name.text = getString(R.string.about_version, AppInfo.getVersion(this))
        xmp_version.text = getString(R.string.about_xmp, Xmp.version)

        (findViewById<View>(R.id.version_name) as TextView).text = getString(R.string.about_version, AppInfo.getVersion(this))

        (findViewById<View>(R.id.xmp_version) as TextView).text = getString(R.string.about_xmp, Xmp.version)
    }
}
