package org.helllabs.android.xmp.preferences.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp.getFormats
import org.helllabs.android.xmp.util.toast

@AndroidEntryPoint
class ListFormats : AppCompatActivity() {

    @Inject
    lateinit var clipboard: ClipboardManager

    private val formats = getFormats()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        setContentView(R.layout.pref_formats)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val appbar = findViewById<TextView>(R.id.toolbarText)
        val list = findViewById<ListView>(R.id.formatsList)

        // Sort alphabetically
        formats.sort()

        appbar.text = getString(R.string.pref_list_formats_title)
        list.apply {
            adapter = ArrayAdapter(this@ListFormats, R.layout.item_single, formats)
            setOnItemLongClickListener { _, _, position, _ ->
                val item = this.getItemAtPosition(position) as String
                val clip = ClipData.newPlainText("Xmp Clipboard", item)
                clipboard.setPrimaryClip(clip)
                toast(R.string.clipboard_copied)
                true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
