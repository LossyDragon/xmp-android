package org.helllabs.android.xmp.browser

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.Playlist
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter.Companion.LAYOUT_CARD
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.modarchive.Search
import org.helllabs.android.xmp.player.PlayerActivity
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.FileUtils.writeToFile
import org.helllabs.android.xmp.util.Message.error
import org.helllabs.android.xmp.util.Message.fatalError
import org.helllabs.android.xmp.util.Message.yesNoDialog
import java.io.File
import java.io.IOException
import java.util.*

class PlaylistMenu : AppCompatActivity(), PlaylistAdapter.OnItemClickListener {

    private lateinit var playlistAdapter: PlaylistAdapter
    private var mediaPath: String? = null
    private var deletePosition = 0

    private val storagePermissions: Unit
        get() {
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
                    REQUEST_WRITE_STORAGE)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logI("Start application")
        setContentView(R.layout.activity_playlist_menu)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)

        // Appbar text tomfoolery
        val spannable = SpannableString(getString(R.string.app_name))
        val color = resources.color(R.color.accent)
        spannable.setSpan(ForegroundColorSpan(color), 0, 3, SPAN_EXCLUSIVE_EXCLUSIVE)
        findViewById<TextView>(R.id.toolbarText).apply {
            text = spannable
            click { startPlayerActivity() }
        }

        // Swipe refresh
        val swipe = findViewById<SwipeRefreshLayout>(R.id.swipeContainer).apply {
            setColorSchemeResources(R.color.refresh_color)
            setOnRefreshListener {
                updateList()
                isRefreshing = false
            }
        }

        // Playlist adapter
        playlistAdapter = PlaylistAdapter(this, ArrayList(), false, LAYOUT_CARD)
        playlistAdapter.setOnItemClickListener(this)

        findViewById<RecyclerView>(R.id.plist_menu_list).apply {
            layoutManager = LinearLayoutManager(this@PlaylistMenu)
            adapter = playlistAdapter
            registerForContextMenu(this)
            setOnItemTouchListener(
                onInterceptTouchEvent = { _, e ->
                    if (e.action == MotionEvent.ACTION_DOWN) {
                        var enable = false
                        if (childCount > 0) {
                            enable = !canScrollVertically(-1)
                        }
                        swipe.isEnabled = enable
                    }
                    false
                }
            )
        }

        // FAB
        findViewById<FloatingActionButton>(R.id.playlist_add_button).click {
            PlaylistUtils.newPlaylistDialog(this) { updateList() }
        }

        if (!Preferences.checkStorage()) {
            fatalError(this, getString(R.string.error_storage))
        }

        if (isAtLeastM) {
            storagePermissions
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
        updateList()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SETTINGS_REQUEST -> if (resultCode == RESULT_OK) updateList()
            PLAYLIST_REQUEST -> updateList()
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

    // Playlist context menu
    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenuInfo) {
        menu.setHeaderTitle("Playlist options")
        val position = playlistAdapter.position
        if (position == 0) {
            // Module list
            menu.add(Menu.NONE, 0, 0, "Change directory")
        } else {
            // Playlists
            menu.add(Menu.NONE, 0, 0, "Rename")
            menu.add(Menu.NONE, 1, 1, "Edit comment")
            menu.add(Menu.NONE, 2, 2, "Delete playlist")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val index = item.itemId
        val position = playlistAdapter.position
        if (position == 0) {
            // First item of list
            if (index == 0) {
                // First item of context menu
                changeDir()
                return true
            }
        } else {
            when (index) {
                0 -> {
                    renameList(position - 1)
                    updateList()
                    return true
                }
                1 -> {
                    editComment(position - 1)
                    updateList()
                    return true
                }
                2 -> {
                    deletePosition = position - 1
                    yesNoDialog(
                        this,
                        "Delete",
                        "Are you sure to delete playlist " +
                            PlaylistUtils.listNoSuffix()[deletePosition] + "?"
                    ) {
                        Playlist.delete(this, PlaylistUtils.listNoSuffix()[deletePosition])
                        updateList()
                    }
                    return true
                }
            }
        }
        return true
    }

    override fun onItemClick(adapter: PlaylistAdapter, view: View?, position: Int) {
        val intent: Intent
        if (position == 0) {
            intent = Intent(this@PlaylistMenu, FilelistActivity::class.java)
        } else {
            intent = Intent(this@PlaylistMenu, PlaylistActivity::class.java)
            intent.putExtra("name", adapter.getItem(position).name)
        }
        startActivityForResult(intent, PLAYLIST_REQUEST)
    }

    // Create application directory and populate with empty playlist
    private fun setupDataDir() {
        if (!Preferences.DATA_DIR.isDirectory) {
            if (Preferences.DATA_DIR.mkdirs()) {
                PlaylistUtils.createEmptyPlaylist(
                    this,
                    getString(R.string.empty_playlist),
                    getString(R.string.empty_comment)
                )
            } else {
                fatalError(this, getString(R.string.error_datadir))
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
        mediaPath = PrefManager.mediaPath
        playlistAdapter.clear()
        val browserItem = PlaylistItem(
            PlaylistItem.TYPE_SPECIAL,
            "File browser",
            "Files in $mediaPath"
        )
        browserItem.imageRes = R.drawable.browser
        playlistAdapter.add(browserItem)
        for (name in PlaylistUtils.listNoSuffix()) {
            val item = PlaylistItem(
                PlaylistItem.TYPE_PLAYLIST,
                name,
                Playlist.readComment(this, name)
            )
            item.imageRes = R.drawable.ic_list
            playlistAdapter.add(item)
        }
        PlaylistUtils.renumberIds(playlistAdapter.getItems())
        playlistAdapter.notifyDataSetChanged()
    }

    private fun changeDir() {
        InputDialog(this).apply {
            setTitle("Change directory")
            setMessage("Enter the mod directory:")
            input.setText(mediaPath)
            setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                val value = input.text.toString()
                if (value != mediaPath) {
                    PrefManager.mediaPath = value
                    updateList()
                }
            }
            setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int -> }
        }.show()
    }

    private fun renameList(index: Int) {
        val name = PlaylistUtils.listNoSuffix()[index]
        InputDialog(this).apply {
            setTitle("Rename playlist")
            setMessage("Enter the new playlist name:")
            input.setText(name)
            setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                if (!Playlist.rename(this@PlaylistMenu, name, input.text.toString())) {
                    error(this@PlaylistMenu, getString(R.string.error_rename_playlist))
                }
                updateList()
            }
            setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int -> }
        }.show()
    }

    private fun editComment(index: Int) {
        val name = PlaylistUtils.listNoSuffix()[index]
        InputDialog(this).apply {
            setTitle("Edit comment")
            setMessage("Enter the new comment for $name:")
            input.setText(Playlist.readComment(this@PlaylistMenu, name))
            setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int ->
                val file = File(Preferences.DATA_DIR, name + Playlist.COMMENT_SUFFIX)
                try {
                    file.delete()
                    file.createNewFile()
                    writeToFile(file, input.text.toString().replace("\n", " "))
                } catch (e: IOException) {
                    error(this@PlaylistMenu, getString(R.string.error_edit_comment))
                }
                updateList()
            }
            setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int -> }
        }.show()
    }

    companion object {
        private const val SETTINGS_REQUEST = 45
        private const val PLAYLIST_REQUEST = 46
        private const val REQUEST_WRITE_STORAGE = 112
    }
}
