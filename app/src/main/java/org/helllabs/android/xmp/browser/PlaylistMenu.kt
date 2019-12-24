package org.helllabs.android.xmp.browser

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Environment.MEDIA_MOUNTED
import android.os.Environment.MEDIA_MOUNTED_READ_ONLY
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_playlist_menu.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.*
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.*
import java.io.File

class PlaylistMenu :
        AppCompatActivity(),
        PlaylistAdapter.OnItemClickListener,
        PlaylistAdapter.OnItemLongClickListener {

    private var mediaPath: String? = null
    private var playlistAdapter: PlaylistAdapter? = null

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setContentView(R.layout.activity_playlist_menu)

        Log.d(TAG, "start application")

        // Swipe refresh
        swipeContainer.apply {
            setColorSchemeResources(R.color.colorRefresh)
            setOnRefreshListener {
                updateList()
                this.isRefreshing = false
            }
        }

        // Add playlist
        playlist_add_button.setOnClickListener {
            startActivityForResult(
                    Intent(this, PlaylistAddEdit::class.java),
                    MOD_ADD_REQUEST
            )
        }

        playlistAdapter = PlaylistAdapter(this, mutableListOf(), false, PlaylistAdapter.LAYOUT_CARD)
        playlistAdapter!!.setOnItemClickListener(this)
        playlistAdapter!!.setOnItemLongClickListener(this)

        plist_menu_list.apply {
            layoutManager = LinearLayoutManager(this@PlaylistMenu)
            adapter = playlistAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0 && playlist_add_button.visibility == View.VISIBLE) {
                        playlist_add_button.hide()
                    } else if (dy < 0 && playlist_add_button.visibility != View.VISIBLE) {
                        playlist_add_button.show()
                    }
                }
            })
        }

        if (!checkStorage()) {
            fatalError(R.string.error_storage)
        }

        if (Build.VERSION.SDK_INT >= 23) {
            getStoragePermissions()
        } else {
            setupDataDir()
        }

        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
            startPlayerActivity()
        }
    }

    override fun onResume() {
        super.onResume()
        updateList()

        showChangeLog(PrefManager.changelogVersion) { result, versionCode ->
            if (result)
                PrefManager.changelogVersion = versionCode
        }
    }

    private fun getStoragePermissions() {
        val hasPermission = ContextCompat
                .checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED

        if (hasPermission) {
            setupDataDir()
            updateList()
        } else {
            ActivityCompat.requestPermissions(
                    this, arrayOf(WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_WRITE_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    setupDataDir()
                    updateList()
                } else if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_DENIED) {
                    fatalError(R.string.error_write_storage)
                }
            }
        }
    }

    // Create application directory and populate with empty playlist
    private fun setupDataDir() {
        if (!Preferences.DATA_DIR.isDirectory) {
            if (Preferences.DATA_DIR.mkdirs()) {
                createEmptyPlaylist(
                        activity = this,
                        name = getString(R.string.empty_playlist),
                        comment = getString(R.string.empty_comment)
                )
            } else {
                fatalError(R.string.error_datadir)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // If we launch from launcher and we're playing a module, go straight to the player activity

        if (intent!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
            startPlayerActivity()
        }
    }

    override fun onItemClick(adapter: PlaylistAdapter, view: View, position: Int) {
        if (position == 0) {
            val intent = Intent(this, FilelistActivity::class.java)
            startActivityForResult(intent, PLAYLIST_REQUEST)
        } else {
            val intent = Intent(this, PlaylistActivity::class.java)
            intent.putExtra(PlaylistActivity.PLAYLIST_NAME, adapter.getItem(position).name)
            startActivityForResult(intent, PLAYLIST_REQUEST)
        }
    }

    override fun onLongItemClick(adapter: PlaylistAdapter, view: View, position: Int) {
        if (position == 0) {
            showChangeDir(mediaPath!!) { result, path ->
                if (result)
                    PrefManager.mediaPath = path
            }
        } else {
            val playlist = playlistAdapter!!.getItem(position)
            val intent = Intent(this, PlaylistAddEdit::class.java)
            intent.putExtra(PlaylistAddEdit.EXTRA_ID, playlist.id)
            intent.putExtra(PlaylistAddEdit.EXTRA_NAME, playlist.name)
            intent.putExtra(PlaylistAddEdit.EXTRA_COMMENT, playlist.comment)
            intent.putExtra(PlaylistAddEdit.EXTRA_TYPE, playlist.type)
            startActivityForResult(intent, MOD_EDIT_REQUEST)
        }
    }

    private fun startPlayerActivity() {
        if (PrefManager.startOnPlayer) {
            if (PlayerService.isAlive) {
                val playerIntent = Intent(this, PlayerActivity::class.java)
                startActivity(playerIntent)
            }
        }
    }

    private fun updateList() {
        mediaPath = PrefManager.mediaPath

        playlistAdapter!!.clear()
        val browserItem =
                PlaylistItem(
                        PlaylistItem.TYPE_SPECIAL,
                        getString(R.string.browser_filelist_title),
                        String.format(getString(R.string.browser_filelist_desc, mediaPath))
                )
        browserItem.imageRes = R.drawable.ic_browser
        playlistAdapter!!.add(browserItem)

        for (name in listNoSuffix()) {
            val item = PlaylistItem(
                    type = PlaylistItem.TYPE_PLAYLIST,
                    name = name,
                    comment = Playlist.readComment(this, name)
            )
            item.imageRes = R.drawable.ic_list
            playlistAdapter!!.add(item)
        }

        renumberIds(playlistAdapter!!.items)
        playlistAdapter!!.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            MOD_ADD_REQUEST -> {
                val name = data!!.getStringExtra(PlaylistAddEdit.EXTRA_NAME)!!
                val comment = data.getStringExtra(PlaylistAddEdit.EXTRA_COMMENT)!!

                if (!createEmptyPlaylist(this, name, comment)) {
                    Log.e(TAG, "Failed to create new playlist: $name")
                    error(R.string.error_create_playlist)
                }
            }
            MOD_EDIT_REQUEST -> {
                val id = data?.getIntExtra(PlaylistAddEdit.EXTRA_ID, -1)

                // Something went wrong.
                if (id == -1 || id == null) {
                    error(R.string.error_edit_playlist)
                    return
                }

                // Delete playlist.
                if (id == -2) {
                    val name = data.getStringExtra(PlaylistAddEdit.EXTRA_NAME)!!
                    Playlist.delete(this, name)
                    updateList()
                    return
                }

                // Everything is OK, edit playlist.
                editPlaylistFromFile(
                        data.getStringExtra(PlaylistAddEdit.EXTRA_NAME)!!,
                        data.getStringExtra(PlaylistAddEdit.EXTRA_COMMENT)!!,
                        data.getStringExtra(PlaylistAddEdit.EXTRA_OLD_NAME)!!
                )
            }
            SETTINGS_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    updateList()
                }
            }
            PLAYLIST_REQUEST -> {
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
            android.R.id.home ->
                startPlayerActivity()
            R.id.menu_prefs ->
                startActivityForResult(
                        Intent(this, Preferences::class.java), SETTINGS_REQUEST)
            R.id.menu_download ->
                startActivity(Intent(this, Search::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun editPlaylistFromFile(name: String, info: String, oldName: String) {
        if (!Playlist.rename(this, oldName, name))
            error(R.string.error_rename_playlist)

        val file = File(Preferences.DATA_DIR, name + Playlist.COMMENT_SUFFIX)
        if (!Playlist.editComment(file, info))
            error(R.string.error_edit_comment)

        updateList()
    }

    companion object {
        private val TAG = PlaylistMenu::class.java.simpleName

        private const val SETTINGS_REQUEST = 45
        private const val PLAYLIST_REQUEST = 46
        private const val REQUEST_WRITE_STORAGE = 112

        const val MOD_ADD_REQUEST = 1
        const val MOD_EDIT_REQUEST = 2

        private fun checkStorage(): Boolean {
            val state = Environment.getExternalStorageState()

            return if (MEDIA_MOUNTED == state || MEDIA_MOUNTED_READ_ONLY == state) {
                true
            } else {
                Log.e(TAG, "External storage state error: $state")
                false
            }
        }
    }
}
