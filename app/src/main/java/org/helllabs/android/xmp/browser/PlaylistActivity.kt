package org.helllabs.android.xmp.browser

import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.Playlist
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter.Companion.LAYOUT_DRAG
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.util.hide
import org.helllabs.android.xmp.util.logE
import org.helllabs.android.xmp.util.recyclerview.OnStartDragListener
import org.helllabs.android.xmp.util.recyclerview.SimpleItemTouchHelperCallback
import org.helllabs.android.xmp.util.show

// TODO: Remove contextual menu as it interferes with drag handle.
class PlaylistActivity :
    BasePlaylistActivity(),
    OnStartDragListener,
    PlaylistAdapter.OnItemClickListener {

    private lateinit var mItemTouchHelper: ItemTouchHelper
    private lateinit var mRecyclerView: RecyclerView
    private var mPlaylist: Playlist? = null

    override var isShuffleMode: Boolean
        get() = mPlaylist!!.isShuffleMode
        set(shuffleMode) {
            mPlaylist!!.isShuffleMode = shuffleMode
        }
    override var isLoopMode: Boolean
        get() = mPlaylist!!.isLoopMode
        set(loopMode) {
            mPlaylist!!.isLoopMode = loopMode
        }
    override val allFiles: List<String>
        get() = mPlaylistAdapter.filenameList

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_playlist)
        findViewById<TextView>(R.id.toolbarText).text = getString(R.string.browser_playlist_title)

        val name = intent.extras?.getString("name") ?: return

        try {
            mPlaylist = Playlist(this, name)
        } catch (e: IOException) {
            logE("Can't read playlist $name")
        }

        findViewById<TextView>(R.id.current_list_name).text = name
        findViewById<TextView>(R.id.current_list_description).text = mPlaylist!!.comment

        mPlaylistAdapter = PlaylistAdapter(
            this,
            mPlaylist!!,
            PrefManager.useFilename,
            LAYOUT_DRAG,
            this
        )
        mPlaylistAdapter.setOnItemClickListener(this)

        mRecyclerView = findViewById<RecyclerView>(R.id.plist_list).apply {
            adapter = mPlaylistAdapter
            setHasFixedSize(true)
            addItemDecoration(
                DividerItemDecoration(this@PlaylistActivity, LinearLayoutManager.HORIZONTAL)
            )
        }

        val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(mPlaylistAdapter)
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper.attachToRecyclerView(mRecyclerView)

        setSwipeRefresh(mRecyclerView)
        registerForContextMenu(mRecyclerView)
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        mPlaylistAdapter.setUseFilename(PrefManager.useFilename)
        update()
    }

    public override fun onPause() {
        super.onPause()
        mPlaylist!!.commit()
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder?) {
        TODO("Not yet implemented")
    }

    override fun onStopDrag(newList: MutableList<PlaylistItem>) {
        TODO("Not yet implemented")
    }

    // Playlist context menu
    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenuInfo?) {
        val mode = PrefManager.playlistMode.toInt()
        menu.setHeaderTitle("Edit playlist")
        menu.add(Menu.NONE, 0, 0, "Remove from playlist")
        menu.add(Menu.NONE, 1, 1, "Add to play queue")
        menu.add(Menu.NONE, 2, 2, "Add all to play queue")
        if (mode != 2) menu.add(Menu.NONE, 3, 3, "Play this module")
        if (mode != 1) menu.add(Menu.NONE, 4, 4, "Play all starting here")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val position = mPlaylistAdapter.position
        when (item.itemId) {
            0 -> {
                mPlaylist!!.remove(position)
                mPlaylist!!.commit()
                update()
            }
            1 -> addToQueue(mPlaylistAdapter.getFilename(position))
            2 -> addToQueue(mPlaylistAdapter.filenameList)
            3 -> playModule(mPlaylistAdapter.getFilename(position))
            4 -> playModule(mPlaylistAdapter.filenameList, position)
        }
        return true
    }

    public override fun update() {
        if (mPlaylistAdapter.getItems().isEmpty()) {
            findViewById<TextView>(R.id.empty_message).show()
        } else {
            findViewById<TextView>(R.id.empty_message).hide()
        }
        mPlaylistAdapter.notifyDataSetChanged()
    }
}
