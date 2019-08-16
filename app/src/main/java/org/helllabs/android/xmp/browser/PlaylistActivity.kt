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

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator
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

    protected override var isShuffleMode: Boolean
        get() = mPlaylist!!.isShuffleMode
        set(shuffleMode) {
            mPlaylist!!.isShuffleMode = shuffleMode
        }

    protected override var isLoopMode: Boolean
        get() = mPlaylist!!.isLoopMode
        set(loopMode) {
            mPlaylist!!.isLoopMode = loopMode
        }

    protected override val allFiles: List<String>
        get() = mPlaylistAdapter!!.filenameList

    // List reorder

    /*private final TouchListView.DropListener onDrop = new TouchListView.DropListener() {
		@Override
		public void drop(final int from, final int to) {
			final PlaylistItem item = playlistAdapter.getItem(from);
			playlistAdapter.remove(item);
			playlistAdapter.insert(item, to);
			playlist.setListChanged(true);
		}
	};

	private final TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
		@Override
		public void remove(final int which) {
			playlistAdapter.remove(playlistAdapter.getItem(which));
		}
	};	*/

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.playlist)

        val extras = getIntent().getExtras() ?: return

        setTitle(R.string.browser_playlist_title)

        val name = extras.getString("name")
        val useFilename = mPrefs.getBoolean(Preferences.USE_FILENAME, false)

        try {
            mPlaylist = Playlist(this, name!!)
        } catch (e: IOException) {
            Log.e(TAG, "Can't read playlist " + name!!)
        }

        mRecyclerView = findViewById(R.id.plist_list) as RecyclerView
        setSwipeRefresh(mRecyclerView!!)

        // layout manager
        val layoutManager = LinearLayoutManager(this)
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL)

        // drag & drop manager
        mRecyclerViewDragDropManager = RecyclerViewDragDropManager()
        mRecyclerViewDragDropManager!!.setDraggingItemShadowDrawable(getResources().getDrawable(R.drawable.material_shadow_z3) as NinePatchDrawable)

        // adapter
        mPlaylistAdapter = PlaylistAdapter(this, mPlaylist!!, useFilename, PlaylistAdapter.LAYOUT_DRAG)
        mWrappedAdapter = mRecyclerViewDragDropManager!!.createWrappedAdapter(mPlaylistAdapter!!)

        val animator = RefactoredDefaultItemAnimator()

        mRecyclerView!!.setLayoutManager(layoutManager)
        mRecyclerView!!.setAdapter(mWrappedAdapter)
        mRecyclerView!!.setItemAnimator(animator)

        // fast scroll
        val fastScroller = findViewById(R.id.fast_scroller) as RecyclerFastScroller
        fastScroller.attachRecyclerView(mRecyclerView)

        // additional decorations

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Lollipop or later has native drop shadow feature. ItemShadowDecorator is not required.
        } else {
            mRecyclerView!!.addItemDecoration(ItemShadowDecorator(getResources().getDrawable(R.drawable.material_shadow_z1) as NinePatchDrawable))
        }
        mRecyclerView!!.addItemDecoration(SimpleListDividerDecorator(getResources().getDrawable(R.drawable.list_divider), true))

        mRecyclerViewDragDropManager!!.attachRecyclerView(mRecyclerView!!)

        mPlaylistAdapter!!.setOnItemClickListener(this)

        val curListName = findViewById(R.id.current_list_name) as TextView
        val curListDesc = findViewById(R.id.current_list_description) as TextView

        curListName.setText(name)
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
            mRecyclerView!!.setItemAnimator(null)
            mRecyclerView!!.setAdapter(null)
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
            0                                        // Remove from playlist
            -> {
                mPlaylist!!.remove(position)
                mPlaylist!!.commit()
                update()
            }
            1                                        // Add to play queue
            -> addToQueue(mPlaylistAdapter!!.getFilename(position))
            2                                        // Add all to play queue
            -> addToQueue(mPlaylistAdapter!!.filenameList)
            3                                        // Play only this module
            -> playModule(mPlaylistAdapter!!.getFilename(position))
            4                                        // Play all starting here
            -> playModule(mPlaylistAdapter!!.filenameList, position)
        }

        return true
    }

    companion object {
        private val TAG = "PlaylistActivity"
    }
}
