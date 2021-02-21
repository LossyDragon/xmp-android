package org.helllabs.android.xmp.browser

import android.graphics.drawable.NinePatchDrawable
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller
import java.io.IOException
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.Playlist
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.util.drawable
import org.helllabs.android.xmp.util.logE

class PlaylistActivity : BasePlaylistActivity(), PlaylistAdapter.OnItemClickListener {

    private var mPlaylist: Playlist? = null
    private var mRecyclerView: RecyclerView? = null
    private var mWrappedAdapter: RecyclerView.Adapter<*>? = null
    private var mRecyclerViewDragDropManager: RecyclerViewDragDropManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.playlist)

        val extras = intent.extras ?: return
        setTitle(R.string.browser_playlist_title)
        val name = extras.getString("name") ?: return
        val useFilename = PrefManager.useFilename
        try {
            mPlaylist = Playlist(this, name)
        } catch (e: IOException) {
            logE("Can't read playlist $name")
        }
        mRecyclerView = findViewById<View>(R.id.plist_list) as RecyclerView
        setSwipeRefresh(mRecyclerView!!)

        // layout manager
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL

        // drag & drop manager
        mRecyclerViewDragDropManager = RecyclerViewDragDropManager()
        mRecyclerViewDragDropManager!!.setDraggingItemShadowDrawable(
            resources.drawable(R.drawable.material_shadow_z3) as NinePatchDrawable
        )

        // adapter
        mPlaylistAdapter = PlaylistAdapter(
            this,
            mPlaylist!!,
            useFilename,
            PlaylistAdapter.LAYOUT_DRAG
        )
        mWrappedAdapter = mRecyclerViewDragDropManager!!.createWrappedAdapter(mPlaylistAdapter)
        val animator: GeneralItemAnimator = RefactoredDefaultItemAnimator()
        mRecyclerView!!.layoutManager = layoutManager
        mRecyclerView!!.adapter = mWrappedAdapter
        mRecyclerView!!.itemAnimator = animator

        // fast scroll
        val fastScroller = findViewById<View>(R.id.fast_scroller) as RecyclerFastScroller
        fastScroller.attachRecyclerView(mRecyclerView)

        mRecyclerView!!.addItemDecoration(
            SimpleListDividerDecorator(resources.drawable(R.drawable.list_divider), true)
        )

        mRecyclerViewDragDropManager!!.attachRecyclerView(mRecyclerView!!)
        mPlaylistAdapter.setOnItemClickListener(this)
        val curListName = findViewById<View>(R.id.current_list_name) as TextView
        val curListDesc = findViewById<View>(R.id.current_list_description) as TextView
        curListName.text = name
        curListDesc.text = mPlaylist!!.comment
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
        mRecyclerViewDragDropManager!!.cancelDrag()
        mPlaylist!!.commit()
    }

    public override fun onDestroy() {
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
        // mPlaylistAdapter = null
        super.onDestroy()
    }

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

    public override fun update() {
        mPlaylistAdapter.notifyDataSetChanged()
    }

    // Playlist context menu
    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenuInfo) {
        val mode = PrefManager.playlistMode.toInt()
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
        val position = mPlaylistAdapter.position
        when (itemId) {
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
}
