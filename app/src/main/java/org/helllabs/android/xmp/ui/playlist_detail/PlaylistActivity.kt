package org.helllabs.android.xmp.ui.playlist_detail

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import java.io.IOException
import kotlinx.coroutines.flow.collect
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.databinding.ActivityPlaylistBinding
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.ui.BasePlaylistActivity
import org.helllabs.android.xmp.ui.BasePlaylistAdapter
import org.helllabs.android.xmp.ui.PlaylistLayoutType
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.util.recyclerview.OnStartDragListener
import org.helllabs.android.xmp.ui.util.recyclerview.SimpleItemTouchHelperCallback
import org.helllabs.android.xmp.util.hide
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.logE
import org.helllabs.android.xmp.util.show

class PlaylistActivity :
    BasePlaylistActivity(),
    OnStartDragListener {

    private lateinit var viewModel: PlaylistActivityViewModel

    private lateinit var binder: ActivityPlaylistBinding
    private lateinit var mItemTouchHelper: ItemTouchHelper

    override var isShuffleMode: Boolean
        get() = viewModel.playlist.isShuffleMode
        set(shuffleMode) {
            viewModel.playlist.isShuffleMode = shuffleMode
        }
    override var isLoopMode: Boolean
        get() = viewModel.playlist.isLoopMode
        set(loopMode) {
            viewModel.playlist.isLoopMode = loopMode
        }
    override val allFiles: List<String>
        get() = mPlaylistAdapter.getFilenameList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder = ActivityPlaylistBinding.inflate(layoutInflater)

        setContentView(binder.root)
        setSupportActionBar(binder.appbar.toolbar)

        val name = intent.extras?.getString("name") ?: return

        try {
            viewModel = ViewModelProvider(
                this,
                viewModelFactory { PlaylistActivityViewModel(name) }
            ).get(PlaylistActivityViewModel::class.java)
        } catch (e: IOException) {
            logE("Can't read playlist $name")
            onBackPressed()
        }

        mPlaylistAdapter = BasePlaylistAdapter(
            PlaylistLayoutType.TYPE_DRAG,
            PrefManager.useFilename
        ).apply {
            dragListener = this@PlaylistActivity
        }

        lifecycleScope.launchWhenStarted {
            viewModel.playlistState.collect {
                when (it) {
                    PlaylistActivityViewModel.PlaylistState.None -> Unit
                    PlaylistActivityViewModel.PlaylistState.Load -> onLoad()
                    is PlaylistActivityViewModel.PlaylistState.Loaded -> onLoaded(it.list)
                }
            }
        }

        val comment = viewModel.playlist.comment.ifEmpty { getString(R.string.no_comment) }
        with(binder) {
            appbar.toolbarText.text = getString(R.string.browser_playlist_title)
            currentListName.text = name
            currentListDescription.text = comment
            plistList.apply {
                adapter = mPlaylistAdapter
                setHasFixedSize(true)
            }
        }

        val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(mPlaylistAdapter)
        mItemTouchHelper = ItemTouchHelper(callback)
        mItemTouchHelper.attachToRecyclerView(binder.plistList)

        setSwipeRefresh(binder.swipeContainer, binder.plistList)
        setupButtons(binder.listControls)
    }

    override fun onResume() {
        mPlaylistAdapter.setUseFilename(PrefManager.useFilename)
        super.onResume() // Call super last to update the list.
    }

    override fun onPause() {
        super.onPause()
        viewModel.playlist.commit()
    }

    override fun update() {
        viewModel.getPlaylist()
    }

    override fun onClick(position: Int) {
        onItemClick(mPlaylistAdapter, position)
    }

    @SuppressLint("CheckResult")
    override fun onLongClick(position: Int) {
        MaterialDialog(this).show {
            title(R.string.dialog_playlist_edit_title)
            listItemsSingleChoice(R.array.edit_playlist_dialog_array) { _, index, _ ->
                when (index) {
                    0 -> {
                        viewModel.playlist.remove(position)
                        viewModel.playlist.setListChanged(true)
                        viewModel.playlist.commit()
                        update()
                    }
                    1 -> addToQueue(mPlaylistAdapter.getFilename(position))
                    2 -> addToQueue(mPlaylistAdapter.getFilenameList())
                    3 -> playModule(mPlaylistAdapter.getFilename(position))
                    4 -> playModule(mPlaylistAdapter.getFilenameList(), position)
                }
            }
            positiveButton(R.string.select)
        }
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        mItemTouchHelper.startDrag(viewHolder)
        viewModel.playlist.setListChanged(true)
    }

    override fun onStopDrag(playlist: MutableList<PlaylistItem>) {
        viewModel.playlist.list.clear()
        viewModel.playlist.list.addAll(playlist)
        viewModel.playlist.commit()
    }

    override fun disableSwipe(isDisabled: Boolean) {
        binder.swipeContainer.isEnabled = isDisabled
    }

    private fun onLoad() {
        binder.errorLayout.layout.hide()
        binder.spinner.show()
    }

    private fun onLoaded(list: List<PlaylistItem>) {
        logD("Updating List")
        mPlaylistAdapter.submitList(list)
        with(binder) {
            spinner.hide()
            if (mPlaylistAdapter.getItems().isEmpty()) {
                errorLayout.layout.show()
                errorLayout.message.text = getString(R.string.msg_empty_playlist)
            } else {
                errorLayout.layout.hide()
            }
        }
    }
}
