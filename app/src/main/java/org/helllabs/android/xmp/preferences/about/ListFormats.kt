package org.helllabs.android.xmp.preferences.about

import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.pref_item_formats.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import java.util.*


class ListFormats : AppCompatActivity() {
    private val formats = Xmp.getFormats()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.pref_item_formats)

        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close)
        }

        Arrays.sort(formats)
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, formats)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}