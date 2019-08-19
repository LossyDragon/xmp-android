package org.helllabs.android.xmp.browser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.activity_add_edit_playlist.*
import org.helllabs.android.xmp.R

class PlaylistAddEdit : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_playlist)

        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)

        if (intent.hasExtra(EXTRA_ID)) {
            playlist_add_edit_name_editText.setText(intent.getStringExtra(EXTRA_NAME))
            playlist_add_edit_comment_editText.setText(intent.getStringExtra(EXTRA_COMMENT))

            title = playlist_add_edit_name_editText.text.toString()

            button_add_edit_playlist.text = getString(R.string.button_playlist_update)
            button_delete_playlist.visibility = View.VISIBLE
            button_delete_playlist.text = String.format(getString(R.string.button_playlist_delete), title)

        } else {
            title = getString(R.string.new_playlist)
            button_add_edit_playlist.text = getString(R.string.button_playlist_add)
            button_delete_playlist.visibility = View.GONE
        }

        button_add_edit_playlist.setOnClickListener {
            savePlaylist()
        }

        button_delete_playlist.setOnClickListener {
            deletePlaylist()
        }

        playlist_add_edit_name_editText.requestFocus()
    }

    private fun savePlaylist() {
        val name = playlist_add_edit_name_editText.text.toString()
        val comment = playlist_add_edit_comment_editText.text.toString()

        playlist_add_edit_name_layout.isErrorEnabled = false

        //Check if name is empty.
        if (name.trim().isBlank()) {
            playlist_add_edit_name_layout.isErrorEnabled = true
            playlist_add_edit_name_layout.error = getString(R.string.error_playlist_name)
            playlist_add_edit_name_editText.requestFocus()
            return
        }

        val playlistData = Intent().apply {
            putExtra(EXTRA_NAME, name)
            putExtra(EXTRA_OLD_NAME, title)
            putExtra(EXTRA_COMMENT, comment)

            if (intent.getIntExtra(EXTRA_ID, -1) != -1)
                putExtra(EXTRA_ID, intent.getIntExtra(EXTRA_ID, -1))
        }

        setResult(Activity.RESULT_OK, playlistData)
        finish()
    }

    private fun deletePlaylist() {
        val dialogMessage = String.format(getString(R.string.dialog_delete_playlist_message), title)
        MaterialDialog(this).show {
            title(R.string.dialog_delete_playlist)
            message(text = dialogMessage)
            positiveButton(R.string.menu_delete) {

                val deleteIntent = Intent().apply {
                    putExtra(EXTRA_ID, -2)
                    putExtra(EXTRA_NAME, intent.getStringExtra(EXTRA_NAME))
                }

                setResult(Activity.RESULT_OK, deleteIntent)
                finish()
            }
            negativeButton(R.string.cancel)
        }
    }

    companion object {
        const val EXTRA_ID = "org.helllabs.android.xmp.browser.EXTRA_ID"
        const val EXTRA_NAME = "org.helllabs.android.xmp.browser.EXTRA_NAME"
        const val EXTRA_OLD_NAME = "org.helllabs.android.xmp.browser.EXTRA_OLD_NAME"
        const val EXTRA_COMMENT = "org.helllabs.android.xmp.browser.EXTRA_COMMENT"
        const val EXTRA_TYPE = "org.helllabs.android.xmp.browser.EXTRA_TYPE"
    }
}