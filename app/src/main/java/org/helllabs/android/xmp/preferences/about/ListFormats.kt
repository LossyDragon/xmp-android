package org.helllabs.android.xmp.preferences.about

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.list_formats.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import java.util.*


class ListFormats : AppCompatActivity() {
    private val formats = Xmp.formats

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.pref_formats)

        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_close)

        Arrays.sort(formats)
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, formats)
    }
}