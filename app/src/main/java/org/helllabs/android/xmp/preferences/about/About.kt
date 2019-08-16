package org.helllabs.android.xmp.preferences.about

import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView

class About : Activity() {

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setContentView(R.layout.about)

        (findViewById<View>(R.id.version_name) as TextView).text = getString(R.string.about_version, AppInfo.getVersion(this))

        (findViewById<View>(R.id.xmp_version) as TextView).text = getString(R.string.about_xmp, Xmp.version)
    }
}
