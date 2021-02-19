package org.helllabs.android.xmp.preferences.about

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp.getVersion

class About : AppCompatActivity() {
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.about)
        val appVersion = AppInfo.getVersion(this)
        findViewById<TextView>(R.id.version_name).text = getString(
            R.string.about_version,
            appVersion
        )
        findViewById<TextView>(R.id.xmp_version).text = getString(R.string.about_xmp, getVersion())
    }
}
