package org.helllabs.android.xmp.browser

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.Playlist
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.ChangeLog
import org.helllabs.android.xmp.util.FileUtils
import org.helllabs.android.xmp.util.Log
import org.helllabs.android.xmp.util.Message
import java.io.File
import java.io.IOException
import java.util.*


class PlaylistMenu : AppCompatActivity(), PlaylistAdapter.OnItemClickListener {
    private var prefs: SharedPreferences? = null
    private var mediaPath: String? = null
    private var deletePosition: Int = 0
    private var playlistAdapter: PlaylistAdapter? = null

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.playlist_menu)

        Log.i(TAG, "start application")

        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        supportActionBar?.elevation = 0f // or other

        title = ""

        val title = findViewById<TextView>(R.id.toolbar_title)
        title.setOnClickListener { startPlayerActivity() }

        // Swipe refresh
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeContainer)
        swipeRefresh.setOnRefreshListener {
            updateList()
            swipeRefresh.isRefreshing = false
        }
        swipeRefresh.setColorSchemeResources(R.color.refresh_color)

        val recyclerView = findViewById<RecyclerView>(R.id.plist_menu_list)

        recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_DOWN) {
                    var enable = false
                    if (recyclerView.childCount > 0) {
                        enable = !recyclerView.canScrollVertically(-1)
                    }
                    swipeRefresh.isEnabled = enable
                }

                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                // do nothing
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                // do nothing
            }
        })


        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = layoutManager

        playlistAdapter = PlaylistAdapter(this@PlaylistMenu, ArrayList(), false, PlaylistAdapter.LAYOUT_CARD)
        playlistAdapter!!.setOnItemClickListener(this)
        recyclerView.adapter = playlistAdapter

        registerForContextMenu(recyclerView)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (!checkStorage()) {
            Message.fatalError(this, getString(R.string.error_storage))
        }

        if (Build.VERSION.SDK_INT >= 23) {
            getStoragePermissions()
        } else {
            setupDataDir()
        }

        val changeLog = ChangeLog(this)
        changeLog.show()

        if (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
            startPlayerActivity()
        }
    }

    override fun onResume() {
        super.onResume()
        updateList()
    }

    private fun getStoragePermissions() {
        val hasPermission = ContextCompat.checkSelfPermission(this,
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
                Message.fatalError(this, getString(R.string.error_datadir))
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
        browserItem.imageRes = R.drawable.browser
        playlistAdapter!!.add(browserItem)

        for (name in PlaylistUtils.listNoSuffix()) {
            val item = PlaylistItem(PlaylistItem.TYPE_PLAYLIST, name, Playlist.readComment(this, name))
            item.imageRes = R.drawable.list
            playlistAdapter!!.add(item)
        }

        PlaylistUtils.renumberIds(playlistAdapter!!.items)
        playlistAdapter!!.notifyDataSetChanged()
    }


    // Playlist context menu

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenu.ContextMenuInfo) {
        menu.setHeaderTitle("Playlist options")

        if (playlistAdapter!!.position == 0) {
            // Module list
            menu.add(Menu.NONE, 0, 0, "Change directory")
            //menu.add(Menu.NONE, 1, 1, "Add to playlist");
        } else {
            // Playlists
            menu.add(Menu.NONE, 0, 0, "Rename")
            menu.add(Menu.NONE, 1, 1, "Edit comment")
            menu.add(Menu.NONE, 2, 2, "Delete playlist")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val index = item.itemId
        val position = playlistAdapter!!.position

        if (position == 0) {            // First item of list
            if (index == 0) {           // First item of context menu
                changeDir(this)
                return true
            }
        } else {
            when (index) {
                // Rename
                0 -> {
                    renameList(this, position - 1)
                    updateList()
                    return true
                }
                // Edit comment
                1 -> {
                    editComment(this, position - 1)
                    updateList()
                    return true
                }
                // Delete
                2 -> {
                    deletePosition = position - 1
                    Message.yesNoDialog(this, "Delete", "Are you sure to delete playlist " +
                            PlaylistUtils.listNoSuffix()[deletePosition] + "?", Runnable {
                        Playlist.delete(this@PlaylistMenu, PlaylistUtils.listNoSuffix()[deletePosition])
                        updateList()
                    })

                    return true
                }
                else -> {
                }
            }
        }

        return true
    }

    private fun renameList(activity: Activity, index: Int) {
        val name = PlaylistUtils.listNoSuffix()[index]
        val alert = InputDialog(activity)
        alert.setTitle("Rename playlist")
        alert.setMessage("Enter the new playlist name:")
        alert.input.setText(name)

        alert.setPositiveButton(R.string.ok) { _, _ ->
            val value = alert.input.text.toString()

            if (!Playlist.rename(activity, name, value)) {
                Message.error(activity, getString(R.string.error_rename_playlist))
            }

            updateList()
        }

        alert.setNegativeButton(R.string.cancel) { _, _ ->
            // Canceled.
        }

        alert.show()
    }

    private fun changeDir(context: Context) {
        val alert = InputDialog(context)
        alert.setTitle("Change directory")
        alert.setMessage("Enter the mod directory:")
        alert.input.setText(mediaPath)

        alert.setPositiveButton(R.string.ok) { _, _ ->
            val value = alert.input.text.toString()
            if (value != mediaPath) {
                val editor = prefs!!.edit()
                editor.putString(Preferences.MEDIA_PATH, value)
                editor.apply()
                updateList()
            }
        }

        alert.setNegativeButton(R.string.cancel) { _, _ ->
            // Canceled.
        }

        alert.show()
    }

    private fun editComment(activity: Activity, index: Int) {
        val name = PlaylistUtils.listNoSuffix()[index]
        val alert = InputDialog(activity)
        alert.setTitle("Edit comment")
        alert.setMessage("Enter the new comment for $name:")
        alert.input.setText(Playlist.readComment(activity, name))

        alert.setPositiveButton(R.string.ok) { _, _ ->
            val value = alert.input.text.toString().replace("\n", " ")
            val file = File(Preferences.DATA_DIR, name + Playlist.COMMENT_SUFFIX)
            try {
                file.delete()
                file.createNewFile()
                FileUtils.writeToFile(file, value)
            } catch (e: IOException) {
                Message.error(activity, getString(R.string.error_edit_comment))
            }

            updateList()
        }

        alert.setNegativeButton(R.string.cancel) { _, _ ->
            // Canceled.
        }

        alert.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SETTINGS_REQUEST -> if (resultCode == RESULT_OK) {
                updateList()
            }
            PLAYLIST_REQUEST -> updateList()
            else -> {
            }
        }
    }

    // Menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.options_menu, menu)

        // Calling super after populating the menu is necessary here to ensure that the
        // action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> startPlayerActivity()
            R.id.menu_new_playlist -> PlaylistUtils.newPlaylistDialog(this, Runnable { updateList() })
            R.id.menu_prefs -> startActivityForResult(Intent(this, Preferences::class.java), SETTINGS_REQUEST)
            R.id.menu_refresh -> updateList()
            R.id.menu_download -> startActivity(Intent(this, Search::class.java))
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun fabClick(view: View) {
        PlaylistUtils.newPlaylistDialog(this, Runnable { updateList() })
    }

    companion object {
        private const val TAG = "PlaylistMenu"
        private const val SETTINGS_REQUEST = 45
        private const val PLAYLIST_REQUEST = 46

        private const val REQUEST_WRITE_STORAGE = 112

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
