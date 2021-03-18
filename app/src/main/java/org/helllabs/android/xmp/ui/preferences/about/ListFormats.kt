package org.helllabs.android.xmp.ui.preferences.about

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp.getFormats
import org.helllabs.android.xmp.databinding.PrefFormatsBinding
import org.helllabs.android.xmp.util.toast

@AndroidEntryPoint
class ListFormats : AppCompatActivity() {

    @Inject
    lateinit var clipboard: ClipboardManager
    lateinit var binder: PrefFormatsBinding

    private val formats = getFormats()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder = PrefFormatsBinding.inflate(layoutInflater)

        setContentView(binder.root)
        setSupportActionBar(binder.appbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Sort alphabetically
        formats.sort()

        val formatsAdapter = ListFormatsAdapter()
        formatsAdapter.submitList(formats.toList())
        formatsAdapter.onLongClick = { item ->
            val clip = ClipData.newPlainText("Xmp Clipboard", item)
            clipboard.setPrimaryClip(clip)
            toast(R.string.clipboard_copied)
        }

        binder.appbar.toolbarText.text = getString(R.string.pref_list_formats_title)
        binder.formatsList.adapter = formatsAdapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
