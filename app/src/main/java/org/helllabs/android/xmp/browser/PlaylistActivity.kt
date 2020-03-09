package org.helllabs.android.xmp.browser

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.list.listItems
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import kotlinx.android.synthetic.main.activity_playlist.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.Playlist
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.util.Log
import java.io.IOException

class PlaylistActivity :
        BasePlaylistActivity(),
        PlaylistAdapter.OnItemClickListener {

    private var mPlaylist: Playlist? = null
    private var mWrappedAdapter: RecyclerView.Adapter<*>? = null
    private var mRecyclerViewDragDropManager: RecyclerViewDragDropManager? = null
    private var name: String? = null

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

        val extras = intent.extras ?: return
        name = extras.getString(PLAYLIST_NAME)

        val useFilename = PrefManager.useFilename

        try {
            mPlaylist = Playlist(this, name!!)
        } catch (e: IOException) {
            Log.e(TAG, "Can't read playlist " + name!!)
        }

        // drag & drop manager
        mRecyclerViewDragDropManager = RecyclerViewDragDropManager()

        // adapter
        mPlaylistAdapter = PlaylistAdapter(
                context = this,
                playlist = mPlaylist!!,
                useFilename = useFilename,
                layoutType = PlaylistAdapter.LAYOUT_DRAG
        )
        mWrappedAdapter = mRecyclerViewDragDropManager!!.createWrappedAdapter(mPlaylistAdapter)

        setSwipeRefresh(swipeContainer)

        plist_list.apply {
            layoutManager = LinearLayoutManager(this@PlaylistActivity)
            adapter = mWrappedAdapter
            itemAnimator = RefactoredDefaultItemAnimator()
            addItemDecoration(
                    DividerItemDecoration(
                            this@PlaylistActivity,
                            DividerItemDecoration.VERTICAL
                    )
            )
        }

        mRecyclerViewDragDropManager!!.attachRecyclerView(plist_list)
        mPlaylistAdapter.clickListener = this

//        current_list_name.text = name
//        current_list_description.text = mPlaylist!!.comment

        supportActionBar!!.title = getString(R.string.title_playlist) + ": " + name
        supportActionBar!!.subtitle = mPlaylist!!.comment

        setupButtons()
    }

    override fun onResume() {
        super.onResume()

        mPlaylistAdapter.setUseFilename(PrefManager.useFilename)
        update()
    }

    override fun onPause() {
        super.onPause()

        mRecyclerViewDragDropManager!!.cancelDrag()
        mPlaylist!!.commit()
    }

    override fun onDestroy() {
        mRecyclerViewDragDropManager?.release()
        mRecyclerViewDragDropManager = null

        plist_list?.itemAnimator = null
        plist_list?.adapter = null

        if (mWrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(mWrappedAdapter)
            mWrappedAdapter = null
        }

        mPlaylistAdapter.clear()

        super.onDestroy()
    }

    public override fun update() {
        mPlaylistAdapter.notifyDataSetChanged()

        if (mPlaylistAdapter.items.isEmpty()) {
            plist_empty.visibility = View.VISIBLE
            plist_list.visibility = View.GONE
        } else {
            plist_empty.visibility = View.GONE
            plist_list.visibility = View.VISIBLE
        }
    }

    override fun onItemLongClick(adapter: PlaylistAdapter, view: View, position: Int) {
        MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(text = String.format(getString(R.string.title_playlist_name), name))
            listItems(R.array.dialog_playlist) { _, index, _ ->
                when (index) {
                    0 -> {
                        mPlaylist!!.remove(position)
                        mPlaylist!!.commit()
                        update()
                    }
                    1 -> addToQueue(mPlaylistAdapter.getFilename(position))
                    2 -> addToQueue(mPlaylistAdapter.filenameList)
                    3 -> playModule(mod = mPlaylistAdapter.getFilename(position))
                    4 -> playModule(
                            modList = mPlaylistAdapter.filenameList,
                            start = position,
                            keepFirst = true
                    )
                }
            }
        }
    }

    companion object {
        private val TAG = PlaylistActivity::class.java.simpleName

        const val PLAYLIST_NAME = "name"
    }
}
