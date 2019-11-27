package org.helllabs.android.xmp.preferences.about

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.pref_item_formats.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.util.toast
import java.util.*


class ListFormats : AppCompatActivity() {
    private val formats = Xmp.getFormats()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.pref_item_formats)

        setSupportActionBar(toolbar)
        supportActionBar!!.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close)
            title = getString(R.string.pref_list_formats_title)
        }

        Arrays.sort(formats)
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, formats)

        list.setOnItemLongClickListener { _, _, position, _ ->
            val item = list.getItemAtPosition(position) as String
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
            val clip: ClipData = ClipData.newPlainText("Xmp Clipboard", item)
            clipboard?.setPrimaryClip(clip)

            toast(R.string.clipboard_copied)

            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}