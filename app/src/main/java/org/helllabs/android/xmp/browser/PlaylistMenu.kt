package org.helllabs.android.xmp.browser

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_playlist_menu.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.Playlist
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.*
import java.io.File
import java.util.*

class PlaylistMenu : AppCompatActivity(), PlaylistAdapter.OnItemClickListener {
    private var prefs: SharedPreferences? = null
    private var mediaPath: String? = null
    private var playlistAdapter: PlaylistAdapter? = null

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.activity_playlist_menu)

        // Init prefs
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        Log.i(TAG, "start application")

        setSupportActionBar(toolbar)

        title = ""  // Custom toolbar has a TextView as a button

        toolbar_title.setOnClickListener { startPlayerActivity() }

        // Swipe refresh
        swipeContainer.apply {
            setColorSchemeResources(R.color.refresh_color)
            setOnRefreshListener {
                updateList()
                this.isRefreshing = false
            }
        }

        // Add playlist
        playlist_add_button.setOnClickListener {
            startActivityForResult(
                    Intent(this@PlaylistMenu, PlaylistAddEdit::class.java),
                    MOD_ADD_REQUEST
            )
        }

        playlistAdapter = PlaylistAdapter(this@PlaylistMenu, ArrayList(), false, PlaylistAdapter.LAYOUT_CARD)
        playlistAdapter!!.setOnItemClickListener(this)

        plist_menu_list.apply {
            layoutManager = LinearLayoutManager(this@PlaylistMenu)
            adapter = playlistAdapter
            registerForContextMenu(this)
        }

        if (!checkStorage()) {
            fatalError(getString(R.string.error_storage))
        }

        if (Build.VERSION.SDK_INT >= 23) {
            getStoragePermissions()
        } else {
            setupDataDir()
        }

        showChangeLog()

        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
            startPlayerActivity()
        }
    }

    override fun onResume() {
        super.onResume()
        updateList()
    }

    private fun getStoragePermissions() {
        val hasPermission =
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            setupDataDir()
            updateList()
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_STORAGE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_WRITE_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupDataDir()
                    updateList()
                }
            }
        }
    }

    // Create application directory and populate with empty playlist
    private fun setupDataDir() {
        if (!Preferences.DATA_DIR.isDirectory) {
            if (Preferences.DATA_DIR.mkdirs()) {
                PlaylistUtils.createEmptyPlaylist(this, getString(R.string.empty_playlist), getString(R.string.empty_comment))
            } else {
                fatalError(getString(R.string.error_datadir))
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
            val intent = Intent(this@PlaylistMenu, FilelistActivity::class.java)
            startActivityForResult(intent, PLAYLIST_REQUEST)
        } else {
            val intent = Intent(this@PlaylistMenu, PlaylistActivity::class.java)
            intent.putExtra("name", adapter.getItem(position).name)
            startActivityForResult(intent, PLAYLIST_REQUEST)
        }
    }

    private fun startPlayerActivity() {
        if (prefs!!.getBoolean(Preferences.START_ON_PLAYER, true)) {
            if (PlayerService.isAlive) {
                val playerIntent = Intent(this, PlayerActivity::class.java)
                startActivity(playerIntent)
            }
        }
    }

    private fun updateList() {
        mediaPath = prefs!!.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH)

        playlistAdapter!!.clear()
        val browserItem = PlaylistItem(PlaylistItem.TYPE_SPECIAL, "File browser", "Files in " + mediaPath!!)
        browserItem.imageRes = R.drawable.ic_browser
        playlistAdapter!!.add(browserItem)

        for (name in PlaylistUtils.listNoSuffix()) {
            val item = PlaylistItem(PlaylistItem.TYPE_PLAYLIST, name, Playlist.readComment(this, name))
            item.imageRes = R.drawable.ic_list
            playlistAdapter!!.add(item)
        }

        PlaylistUtils.renumberIds(playlistAdapter!!.items)
        playlistAdapter!!.notifyDataSetChanged()
    }


    // Playlist context menu
    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        menu.setHeaderTitle("Playlist options")

        if (playlistAdapter!!.position == 0) {
            // Module list
            menu.add(Menu.NONE, 0, 0, "Change directory")
            //menu.add(Menu.NONE, 1, 1, "Add to playlist");
        } else {
            // Playlists
            menu.add(Menu.NONE, 0, 0, "Edit Playlist")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val index = item.itemId
        val position = playlistAdapter!!.position

        if (position == 0) {            // First item of list
            if (index == 0) {           // First item of context menu
                showChangeDir(prefs!!, Runnable { updateList() })
                return true
            }
        } else {
            if (index == 0) {
                val playlist = playlistAdapter!!.getItem(position )
                val intent = Intent(this@PlaylistMenu, PlaylistAddEdit::class.java)
                intent.putExtra(PlaylistAddEdit.EXTRA_ID, playlist.id)
                intent.putExtra(PlaylistAddEdit.EXTRA_NAME, playlist.name)
                intent.putExtra(PlaylistAddEdit.EXTRA_COMMENT, playlist.comment)
                intent.putExtra(PlaylistAddEdit.EXTRA_TYPE, playlist.type)
                startActivityForResult(intent, MOD_EDIT_REQUEST)
                return true
            }
        }

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            MOD_ADD_REQUEST -> {
                val name = data!!.getStringExtra(PlaylistAddEdit.EXTRA_NAME)!!
                val comment = data.getStringExtra(PlaylistAddEdit.EXTRA_COMMENT)!!

                if (!PlaylistUtils.createEmptyPlaylist(this, name, comment)) {
                    Log.w(TAG, "Failed to create new Playlist")
                    error(text = "Failed to create new Playlist")
                }

            }
            MOD_EDIT_REQUEST -> {
                val id = data?.getIntExtra(PlaylistAddEdit.EXTRA_ID, -1)

                //Something went wrong.
                if (id == -1 || id == null) {
                    error(text = "Cannot edit playlist")
                    return
                }

                //Delete playlist.
                if (id == -2) {
                    val name = data.getStringExtra(PlaylistAddEdit.EXTRA_NAME)!!
                    Playlist.delete(this@PlaylistMenu, name)
                    updateList()
                    return
                }

                //Everything is OK, edit playlist.
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
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_options, menu)

        // Calling super after populating the menu is necessary here to ensure that the
        // action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> startPlayerActivity()
            R.id.menu_prefs -> startActivityForResult(Intent(this, Preferences::class.java), SETTINGS_REQUEST)
            R.id.menu_download -> startActivity(Intent(this, Search::class.java))
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
        private const val TAG = "PlaylistMenu"

        private const val SETTINGS_REQUEST = 45
        private const val PLAYLIST_REQUEST = 46
        private const val REQUEST_WRITE_STORAGE = 112

        const val MOD_ADD_REQUEST = 1
        const val MOD_EDIT_REQUEST = 2

        private fun checkStorage(): Boolean {
            val state = Environment.getExternalStorageState()

            return if (Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state) {
                true
            } else {
                Log.e(TAG, "External storage state error: $state")
                false
            }
        }
    }
}
