package org.helllabs.android.xmp.preferences.about

import android.app.ListActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp.getFormats
import java.util.*

class ListFormats : ListActivity() {
    private val formats = getFormats()
    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.list_formats)
        Arrays.sort(formats)
        listAdapter = ArrayAdapter(this, R.layout.format_list_item, formats)
    }
}