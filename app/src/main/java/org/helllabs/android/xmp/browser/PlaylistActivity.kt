package org.helllabs.android.xmp.browser

import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import java.io.IOException
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.Playlist
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter.Companion.LAYOUT_DRAG
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.databinding.ActivityPlaylistBinding
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.util.hide
import org.helllabs.android.xmp.util.logE
import org.helllabs.android.xmp.util.recyclerview.OnStartDragListener
import org.helllabs.android.xmp.util.recyclerview.SimpleItemTouchHelperCallback
import org.helllabs.android.xmp.util.show

class PlaylistActivity :
    BasePlaylistActivity(),
    OnStartDragListener {

    private lateinit var binder: ActivityPlaylistBinding
    private lateinit var mItemTouchHelper: ItemTouchHelper
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

        binder = ActivityPlaylistBinding.inflate(layoutInflater)

        setContentView(binder.root)
        setSupportActionBar(binder.appbar.toolbar)
        binder.appbar.toolbarText.text = getString(R.string.browser_playlist_title)

        val name = intent.extras?.getString("name") ?: return

        try {
            mPlaylist = Playlist(name)
        } catch (e: IOException) {
            logE("Can't read playlist $name")
        }

        mPlaylistAdapter = PlaylistAdapter(LAYOUT_DRAG, PrefManager.useFilename)
        mPlaylistAdapter.onClick = { position -> onItemClick(mPlaylistAdapter, position) }
        mPlaylistAdapter.onLongClick = { position -> onItemLongClick(position) }
        mPlaylistAdapter.dragListener = this

        binder.apply {
            currentListName.text = name
            currentListDescription.text = mPlaylist!!.comment
            plistList.apply {
                adapter = mPlaylistAdapter
                setHasFixedSize(true)
                addItemDecoration(
                    DividerItemDecoration(this@PlaylistActivity, LinearLayoutManager.HORIZONTAL)
                )
            }
        }

        val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(mPlaylistAdapter)
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper.attachToRecyclerView(binder.plistList)

        setSwipeRefresh(binder.swipeContainer, binder.plistList)
        setupButtons(binder.listControls)
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

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        mItemTouchHelper.startDrag(viewHolder)
    }

    override fun onStopDrag(playlist: MutableList<PlaylistItem>) {
        mPlaylist!!.list.clear()
        mPlaylist!!.list.addAll(playlist)
        mPlaylist!!.setListChanged(true)
        mPlaylist!!.commit()
        mPlaylistAdapter.submitList(mPlaylist!!.list)
    }

    override fun disableSwipe(isDisabled: Boolean) {
        binder.swipeContainer.isEnabled = isDisabled
    }

    private fun onItemLongClick(position: Int) {
        MaterialDialog(this).show {
            title(R.string.dialog_playlist_edit_title)
            listItemsSingleChoice(R.array.edit_playlist_dialog_array) { _, index, _ ->
                when (index) {
                    0 -> {
                        mPlaylist!!.remove(position)
                        mPlaylist!!.setListChanged(true)
                        mPlaylist!!.commit()
                        update()
                    }
                    1 -> addToQueue(mPlaylistAdapter.getFilename(position))
                    2 -> addToQueue(mPlaylistAdapter.filenameList)
                    3 -> playModule(mPlaylistAdapter.getFilename(position))
                    4 -> playModule(mPlaylistAdapter.filenameList, position)
                }
            }
            positiveButton(R.string.select)
        }
    }

    public override fun update() {
        mPlaylistAdapter.submitList(mPlaylist!!.list)
        binder.emptyMessage.apply {
            if (mPlaylistAdapter.getItems().isEmpty()) show() else hide()
        }
    }
}
