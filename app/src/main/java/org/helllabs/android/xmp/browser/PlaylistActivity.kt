package org.helllabs.android.xmp.browser

import android.graphics.drawable.NinePatchDrawable
import android.os.Build
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.decoration.ItemShadowDecorator
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.Playlist
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.util.Log
import java.io.IOException


class PlaylistActivity : BasePlaylistActivity(), PlaylistAdapter.OnItemClickListener {
    private var mPlaylist: Playlist? = null
    private var mRecyclerView: RecyclerView? = null
    private var mWrappedAdapter: RecyclerView.Adapter<*>? = null
    private var mRecyclerViewDragDropManager: RecyclerViewDragDropManager? = null

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
        get() = mPlaylistAdapter!!.filenameList


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.playlist)

        val extras = intent.extras ?: return

        setTitle(R.string.browser_playlist_title)

        val name = extras.getString("name")
        val useFilename = mPrefs.getBoolean(Preferences.USE_FILENAME, false)

        try {
            mPlaylist = Playlist(this, name!!)
        } catch (e: IOException) {
            Log.e(TAG, "Can't read playlist " + name!!)
        }

        mRecyclerView = findViewById<RecyclerView>(R.id.plist_list)
        setSwipeRefresh(mRecyclerView!!)

        // layout manager
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL

        // drag & drop manager
        mRecyclerViewDragDropManager = RecyclerViewDragDropManager()
        @Suppress("DEPRECATION")
        mRecyclerViewDragDropManager!!.setDraggingItemShadowDrawable(resources.getDrawable(R.drawable.material_shadow_z3) as NinePatchDrawable)

        // adapter
        mPlaylistAdapter = PlaylistAdapter(this, mPlaylist!!, useFilename, PlaylistAdapter.LAYOUT_DRAG)
        mWrappedAdapter = mRecyclerViewDragDropManager!!.createWrappedAdapter(mPlaylistAdapter!!)

        val animator = RefactoredDefaultItemAnimator()

        mRecyclerView!!.layoutManager = layoutManager
        mRecyclerView!!.adapter = mWrappedAdapter
        mRecyclerView!!.itemAnimator = animator

        // fast scroll
        val fastScroller = findViewById<RecyclerFastScroller>(R.id.fast_scroller)
        fastScroller.attachRecyclerView(mRecyclerView)

        // additional decorations

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Lollipop or later has native drop shadow feature. ItemShadowDecorator is not required.
        } else {
            @Suppress("DEPRECATION")
            mRecyclerView!!.addItemDecoration(ItemShadowDecorator(resources.getDrawable(R.drawable.material_shadow_z1) as NinePatchDrawable))
        }
        @Suppress("DEPRECATION")
        mRecyclerView!!.addItemDecoration(SimpleListDividerDecorator(resources.getDrawable(R.drawable.list_divider), true))

        mRecyclerViewDragDropManager!!.attachRecyclerView(mRecyclerView!!)

        mPlaylistAdapter!!.setOnItemClickListener(this)

        val curListName = findViewById<TextView>(R.id.current_list_name)
        val curListDesc = findViewById<TextView>(R.id.current_list_description)

        curListName.text = name
        curListDesc.text = mPlaylist!!.comment
        registerForContextMenu(mRecyclerView)

        setupButtons()
    }

    override fun onResume() {
        super.onResume()

        mPlaylistAdapter!!.setUseFilename(mPrefs.getBoolean(Preferences.USE_FILENAME, false))
        update()
    }

    override fun onPause() {
        super.onPause()

        mRecyclerViewDragDropManager!!.cancelDrag()
        mPlaylist!!.commit()
    }

    override fun onDestroy() {
        if (mRecyclerViewDragDropManager != null) {
            mRecyclerViewDragDropManager!!.release()
            mRecyclerViewDragDropManager = null
        }

        if (mRecyclerView != null) {
            mRecyclerView!!.itemAnimator = null
            mRecyclerView!!.adapter = null
            mRecyclerView = null
        }

        if (mWrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(mWrappedAdapter)
            mWrappedAdapter = null
        }
        mPlaylistAdapter = null

        super.onDestroy()
    }

    public override fun update() {
        mPlaylistAdapter!!.notifyDataSetChanged()
    }

    // Playlist context menu
    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenuInfo) {

        val mode = Integer.parseInt(mPrefs.getString(Preferences.PLAYLIST_MODE, "1")!!)

        menu.setHeaderTitle("Edit playlist")
        menu.add(Menu.NONE, 0, 0, "Remove from playlist")
        menu.add(Menu.NONE, 1, 1, "Add to play queue")
        menu.add(Menu.NONE, 2, 2, "Add all to play queue")
        if (mode != 2) {
            menu.add(Menu.NONE, 3, 3, "Play this module")
        }
        if (mode != 1) {
            menu.add(Menu.NONE, 4, 4, "Play all starting here")
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        val position = mPlaylistAdapter!!.position

        when (itemId) {
            // Remove from playlist
            0 -> {
                mPlaylist!!.remove(position)
                mPlaylist!!.commit()
                update()
            }
            // Add to play queue
            1 -> addToQueue(mPlaylistAdapter!!.getFilename(position))
            // Add all to play queue
            2 -> addToQueue(mPlaylistAdapter!!.filenameList)
            // Play only this module
            3 -> playModule(mPlaylistAdapter!!.getFilename(position))
            // Play all starting here
            4 -> playModule(mPlaylistAdapter!!.filenameList, position)
        }

        return true
    }

    companion object {
        private const val TAG = "PlaylistActivity"
    }
}
