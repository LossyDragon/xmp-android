package org.helllabs.android.xmp.browser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.databinding.ActivityAddEditPlaylistBinding
import org.helllabs.android.xmp.util.click
import org.helllabs.android.xmp.util.hide
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.show

class PlaylistAddEdit : AppCompatActivity() {

    private lateinit var binder: ActivityAddEditPlaylistBinding
    private var isEditing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder = ActivityAddEditPlaylistBinding.inflate(layoutInflater)

        logD("onCreate")
        setContentView(binder.root)
        setSupportActionBar(binder.appbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val name = intent?.getStringExtra(EXTRA_NAME)
        val comment = intent?.getStringExtra(EXTRA_COMMENT)

        if (intent.hasExtra(EXTRA_ID)) {
            binder.appbar.toolbarText.text = getString(R.string.title_edit_playlist)
            binder.playlistAddEditNameEditText.setText(name)
            binder.playlistAddEditCommentEditText.setText(comment)
            binder.buttonAddEditPlaylist.text = getString(R.string.button_playlist_update)
            binder.buttonDeletePlaylist.show()
            binder.buttonDeletePlaylist.text = getString(R.string.button_playlist_delete, name)
            isEditing = true
        } else {
            binder.appbar.toolbarText.text = getString(R.string.menu_new_playlist)
            binder.buttonAddEditPlaylist.text = getString(R.string.button_playlist_add)
            binder.buttonDeletePlaylist.hide()
            isEditing = false
        }

        binder.buttonAddEditPlaylist.click { savePlaylist() }
        binder.buttonDeletePlaylist.click { deletePlaylist(name) }
        binder.playlistAddEditNameEditText.requestFocus()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun savePlaylist() {
        val name = binder.playlistAddEditNameEditText.text.toString()
        val comment = binder.playlistAddEditCommentEditText.text.toString()

        binder.playlistAddEditNameLayout.isErrorEnabled = false

        // Check if name is empty.
        if (name.trim().isBlank()) {
            binder.playlistAddEditNameLayout.isErrorEnabled = true
            binder.playlistAddEditNameLayout.error = getString(R.string.error_playlist_name)
            binder.playlistAddEditNameLayout.requestFocus()
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
