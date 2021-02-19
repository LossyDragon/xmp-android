package org.helllabs.android.xmp.browser

import android.graphics.drawable.NinePatchDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
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
import java.io.IOException
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.Playlist
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.util.logE

class PlaylistActivity : BasePlaylistActivity(), PlaylistAdapter.OnItemClickListener {
    private var mPlaylist: Playlist? = null
    private var mRecyclerView: RecyclerView? = null
    private var mWrappedAdapter: RecyclerView.Adapter<*>? = null
    private var mRecyclerViewDragDropManager: RecyclerViewDragDropManager? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.playlist)
        val extras = intent.extras ?: return
        setTitle(R.string.browser_playlist_title)
        val name = extras.getString("name") ?: return
        val useFilename = mPrefs!!.getBoolean(Preferences.USE_FILENAME, false)
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
            resources.getDrawable(R.drawable.material_shadow_z3) as NinePatchDrawable
        )

        // adapter
        mPlaylistAdapter = PlaylistAdapter(
            this,
            mPlaylist!!,
            useFilename,
            PlaylistAdapter.LAYOUT_DRAG
        )
        mWrappedAdapter = mRecyclerViewDragDropManager!!.createWrappedAdapter(mPlaylistAdapter!!)
        val animator: GeneralItemAnimator = RefactoredDefaultItemAnimator()
        mRecyclerView!!.layoutManager = layoutManager
        mRecyclerView!!.adapter = mWrappedAdapter
        mRecyclerView!!.itemAnimator = animator

        // fast scroll
        val fastScroller = findViewById<View>(R.id.fast_scroller) as RecyclerFastScroller
        fastScroller.attachRecyclerView(mRecyclerView)

        // additional decorations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Lollipop or later has native drop shadow feature. ItemShadowDecorator is not required.
        } else {
            mRecyclerView!!.addItemDecoration(
                ItemShadowDecorator(
                    resources.getDrawable(R.drawable.material_shadow_z1) as NinePatchDrawable
                )
            )
        }
        mRecyclerView!!.addItemDecoration(
            SimpleListDividerDecorator(resources.getDrawable(R.drawable.list_divider), true)
        )
        mRecyclerViewDragDropManager!!.attachRecyclerView(mRecyclerView!!)
        mPlaylistAdapter!!.setOnItemClickListener(this)
        val curListName = findViewById<View>(R.id.current_list_name) as TextView
        val curListDesc = findViewById<View>(R.id.current_list_description) as TextView
        curListName.text = name
        curListDesc.text = mPlaylist!!.comment
        registerForContextMenu(mRecyclerView)
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        mPlaylistAdapter!!.setUseFilename(mPrefs!!.getBoolean(Preferences.USE_FILENAME, false))
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
        mPlaylistAdapter = null
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
        get() = mPlaylistAdapter!!.filenameList

    public override fun update() {
        mPlaylistAdapter!!.notifyDataSetChanged()
    }

    // Playlist context menu
    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenuInfo) {
        val mode = mPrefs!!.getString(Preferences.PLAYLIST_MODE, "1")!!.toInt()
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
            0 -> {
                mPlaylist!!.remove(position)
                mPlaylist!!.commit()
                update()
            }
            1 -> addToQueue(mPlaylistAdapter!!.getFilename(position))
            2 -> addToQueue(mPlaylistAdapter!!.filenameList)
            3 -> playModule(mPlaylistAdapter!!.getFilename(position))
            4 -> playModule(mPlaylistAdapter!!.filenameList, position)
        }
        return true
    }
}
