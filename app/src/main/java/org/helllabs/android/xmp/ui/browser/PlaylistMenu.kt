package org.helllabs.android.xmp.ui.browser

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import java.io.File
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.databinding.ActivityPlaylistMenuBinding
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.ui.browser.playlist.Playlist
import org.helllabs.android.xmp.ui.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.ui.browser.playlist.PlaylistAdapter.Companion.LAYOUT_CARD
import org.helllabs.android.xmp.ui.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.ui.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.ui.browser.playlist.PlaylistUtils.createEmptyPlaylist
import org.helllabs.android.xmp.ui.modarchive.Search
import org.helllabs.android.xmp.ui.player.PlayerActivity
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.preferences.Preferences
import org.helllabs.android.xmp.util.*

class PlaylistMenu : AppCompatActivity() {

    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var mediaPath: String

    private lateinit var binder: ActivityPlaylistMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder = ActivityPlaylistMenuBinding.inflate(layoutInflater)

        logI("Start application")
        setContentView(binder.root)
        setSupportActionBar(binder.appbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)

        // Appbar text tomfoolery
        val spannable = SpannableString(getString(R.string.app_name))
        val color = resources.color(R.color.accent)
        spannable.setSpan(ForegroundColorSpan(color), 0, 3, SPAN_EXCLUSIVE_EXCLUSIVE)
        binder.appbar.toolbarText.apply {
            text = spannable
            click { startPlayerActivity() }
        }

        // Swipe refresh
        binder.swipeContainer.apply {
            setColorSchemeResources(R.color.refresh_color)
            setOnRefreshListener {
                updateList()
                isRefreshing = false
            }
        }

        // Playlist adapter
        playlistAdapter = PlaylistAdapter(LAYOUT_CARD, false)
        playlistAdapter.onClick = { position -> onClick(position) }
        playlistAdapter.onLongClick = { position -> onLongClick(position) }

        binder.plistMenuList.apply {
            adapter = playlistAdapter
            setOnItemTouchListener(
                onInterceptTouchEvent = { _, e ->
                    if (e.action == MotionEvent.ACTION_DOWN) {
                        var enable = false
                        if (childCount > 0) {
                            enable = !canScrollVertically(-1)
                        }
                        binder.swipeContainer.isEnabled = enable
                    }
                    false
                }
            )
        }

        // FAB
        binder.fab.click {
            val intent = Intent(this, PlaylistAddEdit::class.java)
            startActivityForResult(intent, MOD_ADD_REQUEST)
        }

        if (!Preferences.checkStorage()) {
            fatalError(getString(R.string.error_storage))
        }

        if (isAtLeastM) {
            val hasPermission =
                ContextCompat.checkSelfPermission(
                    this,
                    WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                setupDataDir()
                updateList()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_STORAGE
                )
            }
        } else {
            setupDataDir()
        }

        // Show Changelog
        showChangeLog()

        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
            startPlayerActivity()
        }
    }

    public override fun onResume() {
        super.onResume()
        playlistAdapter.submitList(null) // Stop flicker
        updateList()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                SETTINGS_REQUEST, PLAYLIST_REQUEST -> updateList()
                MOD_ADD_REQUEST -> addPlaylist(data)
                MOD_EDIT_REQUEST -> editPlaylist(data)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupDataDir()
                updateList()
            }
        }
    }

    // Menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_options, menu)

        // Calling super after populating the menu is necessary here to ensure that the
        // action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> startPlayerActivity()
            R.id.menu_prefs -> {
                val intent = Intent(this, Preferences::class.java)
                startActivityForResult(intent, SETTINGS_REQUEST)
            }
            R.id.menu_download -> {
                val intent = Intent(this, Search::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onNewIntent(intent: Intent) {
        // If we launch from launcher and we're playing a module, go straight to the player activity
        super.onNewIntent(intent)
        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
            startPlayerActivity()
        }
    }

    private fun onClick(position: Int) {
        val intent: Intent
        if (position == 0) {
            intent = Intent(this@PlaylistMenu, FilelistActivity::class.java)
        } else {
            intent = Intent(this@PlaylistMenu, PlaylistActivity::class.java)
            intent.putExtra("name", playlistAdapter.currentList[position].name)
        }
        startActivityForResult(intent, PLAYLIST_REQUEST)
    }

    private fun onLongClick(position: Int) {
        if (position == 0) {
            changeDir()
        } else {
            val playlist = playlistAdapter.currentList[position]
            val intent = Intent(this, PlaylistAddEdit::class.java).apply {
                putExtra(PlaylistAddEdit.EXTRA_ID, playlist.id)
                putExtra(PlaylistAddEdit.EXTRA_NAME, playlist.name)
                putExtra(PlaylistAddEdit.EXTRA_COMMENT, playlist.comment)
                putExtra(PlaylistAddEdit.EXTRA_TYPE, playlist.type)
            }
            startActivityForResult(intent, MOD_EDIT_REQUEST)
        }
    }

    // Create application directory and populate with empty playlist
    private fun setupDataDir() {
        if (!Preferences.DATA_DIR.isDirectory) {
            if (Preferences.DATA_DIR.mkdirs()) {
                createEmptyPlaylist(
                    this,
                    getString(R.string.empty_playlist),
                    getString(R.string.empty_comment)
                )
            } else {
                fatalError(getString(R.string.error_datadir))
            }
        }
    }

    private fun startPlayerActivity() {
        if (PrefManager.startOnPlayer) {
            if (PlayerService.isPlayerAlive.value == true) {
                val playerIntent = Intent(this, PlayerActivity::class.java)
                startActivity(playerIntent)
            }
        }
    }

    private fun updateList() {
        playlistAdapter.submitList(null) // Stop Flicker

        val list = mutableListOf<PlaylistItem>()
        mediaPath = PrefManager.mediaPath
        val browserItem = PlaylistItem(
            PlaylistItem.TYPE_SPECIAL,
            getString(R.string.playlist_special_title),
            getString(R.string.playlist_special_comment, mediaPath)
        )
        list.add(browserItem)

        PlaylistUtils.listNoSuffix().forEach { name ->
            val item = PlaylistItem(
                PlaylistItem.TYPE_PLAYLIST,
                name,
                Playlist.readComment(this, name)
            )
            list.add(item)
        }

        PlaylistUtils.renumberIds(playlistAdapter.getItems())
        playlistAdapter.submitList(list)
    }

    private fun addPlaylist(data: Intent?) {
        if (data == null) {
            toast("Couldn't add playlist")
            return
        }

        val name = data.getStringExtra(PlaylistAddEdit.EXTRA_NAME)!!
        val comment = data.getStringExtra(PlaylistAddEdit.EXTRA_COMMENT)!!
        if (!createEmptyPlaylist(this, name, comment)) {
            generalError(getString(R.string.error_create_playlist))
        }

        updateList()
    }

    private fun editPlaylist(data: Intent?) {

        if (data == null) {
            toast(R.string.msg_edit_playlist_failed)
            return
        }

        val id = data.getIntExtra(PlaylistAddEdit.EXTRA_ID, -1)
        val name = data.getStringExtra(PlaylistAddEdit.EXTRA_NAME)!!
        val comment = data.getStringExtra(PlaylistAddEdit.EXTRA_COMMENT)!!
        val oldName = data.getStringExtra(PlaylistAddEdit.EXTRA_OLD_NAME)

        when (id) {
            PlaylistAddEdit.RESULT_DELETE_PLAYLIST -> Playlist.delete(name)
            PlaylistAddEdit.RESULT_EDIT_PLAYLIST -> {
                if (!Playlist.rename(oldName!!, name)) {
                    generalError(getString(R.string.error_rename_playlist))
                    return // Don't attempt to edit comment if failed.
                }

                val file = File(Preferences.DATA_DIR, name + Playlist.COMMENT_SUFFIX)
                if (!Playlist.editComment(file, comment)) {
                    generalError(getString(R.string.error_edit_comment))
                }
            }
            else -> throw IllegalArgumentException("Edit playlist id was not correct: $id")
        }

        updateList()
    }

    private fun changeDir() {
        MaterialDialog(this).show {
            title(R.string.dialog_change_dir_title)
            message(R.string.dialog_change_dir_msg)
            input(
                prefill = mediaPath,
                waitForPositiveButton = true,
                allowEmpty = false
            ) { _, text ->
                if (mediaPath != mediaPath) {
                    PrefManager.mediaPath = text.toString()
                    updateList()
                }
            }
            negativeButton(R.string.cancel)
        }
    }

    companion object {
        private const val MOD_ADD_REQUEST = 1
        private const val MOD_EDIT_REQUEST = 2
        private const val SETTINGS_REQUEST = 45
        private const val PLAYLIST_REQUEST = 46
        private const val REQUEST_WRITE_STORAGE = 112
    }
}
