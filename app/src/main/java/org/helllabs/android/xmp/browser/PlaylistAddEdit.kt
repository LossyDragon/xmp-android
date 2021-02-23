package org.helllabs.android.xmp.browser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.util.click
import org.helllabs.android.xmp.util.hide
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.show

class PlaylistAddEdit : AppCompatActivity() {

    private var isEditing: Boolean = false

    private lateinit var appBarText: TextView
    private lateinit var updateButton: MaterialButton
    private lateinit var deleteButton: MaterialButton
    private lateinit var nameEditText: TextView
    private lateinit var commentEditText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logD("onCreate")
        setContentView(R.layout.activity_add_edit_playlist)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val name = intent.getStringExtra(EXTRA_NAME)
        val comment = intent.getStringExtra(EXTRA_COMMENT)

        appBarText = findViewById(R.id.toolbarText)
        updateButton = findViewById(R.id.button_add_edit_playlist)
        deleteButton = findViewById(R.id.button_delete_playlist)
        nameEditText = findViewById(R.id.playlist_add_edit_name_editText)
        commentEditText = findViewById(R.id.playlist_add_edit_comment_editText)

        if (intent.hasExtra(EXTRA_ID)) {
            appBarText.text = getString(R.string.title_edit_playlist)
            nameEditText.text = name
            commentEditText.text = comment
            updateButton.text = getString(R.string.button_playlist_update)
            deleteButton.show()
            deleteButton.text = getString(R.string.button_playlist_delete, name)
            isEditing = true
        } else {
            appBarText.text = getString(R.string.menu_new_playlist)
            updateButton.text = getString(R.string.button_playlist_add)
            deleteButton.hide()
            isEditing = false
        }

        updateButton.click { savePlaylist() }
        deleteButton.click { deletePlaylist(name) }
        nameEditText.requestFocus()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun savePlaylist() {
        val name = nameEditText.text.toString()
        val comment = commentEditText.text.toString()
        val nameLayout: TextInputLayout = findViewById(R.id.playlist_add_edit_name_layout)

        nameLayout.isErrorEnabled = false

        // Check if name is empty.
        if (name.trim().isBlank()) {
            nameLayout.isErrorEnabled = true
            nameLayout.error = getString(R.string.error_playlist_name)
            nameEditText.requestFocus()
            return
        }

        val playlistData = Intent().apply {
            putExtra(EXTRA_NAME, name)
            putExtra(EXTRA_COMMENT, comment)
            if (isEditing) {
                putExtra(EXTRA_OLD_NAME, intent.getStringExtra(EXTRA_NAME))
                putExtra(EXTRA_ID, RESULT_EDIT_PLAYLIST)
            } else {
                putExtra(EXTRA_ID, RESULT_NEW_PLAYLIST)
            }
        }

        setResult(Activity.RESULT_OK, playlistData)
        finish()
    }

    private fun deletePlaylist(playlist: String?) {
        if (playlist == null)
            return

        val deleteIntent = Intent()
        MaterialDialog(this).show {
            title(R.string.dialog_delete_playlist)
            message(text = getString(R.string.dialog_delete_playlist_message, playlist))
            positiveButton(R.string.menu_delete) {
                deleteIntent.apply {
                    putExtra(EXTRA_ID, RESULT_DELETE_PLAYLIST)
                    putExtra(EXTRA_NAME, intent.getStringExtra(EXTRA_NAME))
                    putExtra(EXTRA_COMMENT, intent.getStringExtra(EXTRA_COMMENT))
                }
                setResult(Activity.RESULT_OK, deleteIntent)
                finish()
            }
            negativeButton(R.string.cancel)
        }
    }

    companion object {
        const val RESULT_NEW_PLAYLIST = 0
        const val RESULT_EDIT_PLAYLIST = 1
        const val RESULT_DELETE_PLAYLIST = 2

        const val EXTRA_ID = "org.helllabs.android.xmp.browser.EXTRA_ID"
        const val EXTRA_NAME = "org.helllabs.android.xmp.browser.EXTRA_NAME"
        const val EXTRA_OLD_NAME = "org.helllabs.android.xmp.browser.EXTRA_OLD_NAME"
        const val EXTRA_COMMENT = "org.helllabs.android.xmp.browser.EXTRA_COMMENT"
        const val EXTRA_TYPE = "org.helllabs.android.xmp.browser.EXTRA_TYPE"
    }
}
