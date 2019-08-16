package org.helllabs.android.xmp.browser

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
import java.util.ArrayList


class PlaylistMenu : AppCompatActivity(), PlaylistAdapter.OnItemClickListener {
    private var prefs: SharedPreferences? = null
    private var mediaPath: String? = null
    private var deletePosition: Int = 0
    private var playlistAdapter: PlaylistAdapter? = null

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.playlist_menu)

        Log.i(TAG, "start application")

        val toolbar = findViewById(R.id.toolbar) as Toolbar

        if (toolbar != null) {
            setSupportActionBar(toolbar)
            //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar()?.setElevation(0f) // or other
        }

        setTitle("")

        val title = findViewById(R.id.toolbar_title) as TextView
        title?.setOnClickListener { startPlayerActivity() }

        // Swipe refresh
        val swipeRefresh = findViewById(R.id.swipeContainer) as SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(object : SwipeRefreshLayout.OnRefreshListener {
            override fun onRefresh() {
                updateList()
                swipeRefresh.setRefreshing(false)
            }
        })
        swipeRefresh.setColorSchemeResources(R.color.refresh_color)

        val recyclerView = findViewById(R.id.plist_menu_list) as RecyclerView

        recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_DOWN) {
                    var enable = false
                    if (recyclerView.getChildCount() > 0) {
                        enable = !recyclerView.canScrollVertically(-1)
                    }
                    swipeRefresh.setEnabled(enable)
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
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL)
        recyclerView.setLayoutManager(layoutManager)

        playlistAdapter = PlaylistAdapter(this@PlaylistMenu, ArrayList<PlaylistItem>(), false, PlaylistAdapter.LAYOUT_CARD)
        playlistAdapter!!.setOnItemClickListener(this)
        recyclerView.setAdapter(playlistAdapter)

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

        if (getIntent().getFlags() and Intent.FLAG_ACTIVITY_NEW_TASK !== 0) {
            startPlayerActivity()
        }

        //enableHomeButton();

        //updateList();
    }

    override fun onResume() {
        super.onResume()
        updateList()
    }

    private fun getStoragePermissions() {
        val hasPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) === PackageManager.PERMISSION_GRANTED
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
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

    /*
	@TargetApi(14)
	private void enableHomeButton() {
		if (Build.VERSION.SDK_INT >= 14) {
			getSupportActionBar().setHomeButtonEnabled(true);
		}
	}
	*/

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
            val item = PlaylistItem(PlaylistItem.TYPE_PLAYLIST, name, Playlist.readComment(this, name))    // NOPMD
            item.imageRes = R.drawable.list
            playlistAdapter!!.add(item)
        }

        PlaylistUtils.renumberIds(playlistAdapter!!.items)
        playlistAdapter!!.notifyDataSetChanged()
    }


    // Playlist context menu

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenu.ContextMenuInfo) {
        //final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        menu.setHeaderTitle("Playlist options")

        val position = playlistAdapter!!.position

        if (position == 0) {                    // Module list
            menu.add(Menu.NONE, 0, 0, "Change directory")
            //menu.add(Menu.NONE, 1, 1, "Add to playlist");
        } else {                                    // Playlists
            menu.add(Menu.NONE, 0, 0, "Rename")
            menu.add(Menu.NONE, 1, 1, "Edit comment")
            menu.add(Menu.NONE, 2, 2, "Delete playlist")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        //final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

        val index = item.itemId
        val position = playlistAdapter!!.position

        if (position == 0) {        // First item of list
            if (index == 0) {            // First item of context menu
                changeDir(this)
                return true
            }
        } else {
            when (index) {
                0                        // Rename
                -> {
                    renameList(this, position - 1)
                    updateList()
                    return true
                }
                1                        // Edit comment
                -> {
                    editComment(this, position - 1)
                    updateList()
                    return true
                }
                2                        // Delete
                -> {
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

        alert.setPositiveButton(R.string.ok) { dialog, whichButton ->
            val value = alert.input.text.toString()

            if (!Playlist.rename(activity, name, value)) {
                Message.error(activity, getString(R.string.error_rename_playlist))
            }

            updateList()
        }

        alert.setNegativeButton(R.string.cancel) { dialog, whichButton ->
            // Canceled.
        }

        alert.show()
    }

    private fun changeDir(context: Context) {
        val alert = InputDialog(context)
        alert.setTitle("Change directory")
        alert.setMessage("Enter the mod directory:")
        alert.input.setText(mediaPath)

        alert.setPositiveButton(R.string.ok) { dialog, whichButton ->
            val value = alert.input.text.toString()
            if (value != mediaPath) {
                val editor = prefs!!.edit()
                editor.putString(Preferences.MEDIA_PATH, value)
                editor.apply()
                updateList()
            }
        }

        alert.setNegativeButton(R.string.cancel) { dialog, whichButton ->
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

        alert.setPositiveButton(R.string.ok) { dialog, whichButton ->
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

        alert.setNegativeButton(R.string.cancel) { dialog, whichButton ->
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
        val inflater = getMenuInflater()
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
        private val TAG = "PlaylistMenu"
        private val SETTINGS_REQUEST = 45
        private val PLAYLIST_REQUEST = 46

        private val REQUEST_WRITE_STORAGE = 112

        private fun checkStorage(): Boolean {
            val state = Environment.getExternalStorageState()

            if (Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state) {
                return true
            } else {
                Log.e(TAG, "External storage state error: $state")
                return false
            }
        }
    }
}
