package org.helllabs.android.xmp.preferences.about

import java.util.Arrays

import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp

import android.app.ListActivity
import android.os.Bundle
import android.widget.ArrayAdapter


class ListFormats : ListActivity() {
    private val formats = Xmp.formats

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.list_formats)
        Arrays.sort(formats)
        listAdapter = ArrayAdapter<String>(this, R.layout.format_list_item, formats)
    }
}
