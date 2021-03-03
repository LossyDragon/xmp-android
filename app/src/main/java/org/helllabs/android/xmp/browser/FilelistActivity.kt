package org.helllabs.android.xmp.browser

import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import java.io.File
import java.util.*
import kotlinx.coroutines.flow.collect
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter.Companion.LAYOUT_LIST
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.databinding.ActivityModlistBinding
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.FileUtils.basename
import org.helllabs.android.xmp.util.InfoCache.delete
import org.helllabs.android.xmp.util.InfoCache.deleteRecursive

// TODO: Replace current path with bread crumb trails
class FilelistActivity : BasePlaylistActivity() {

    private lateinit var binder: ActivityModlistBinding
    private lateinit var mNavigation: FilelistNavigation
    private val viewModel: FilelistViewModel by viewModels()

    override var isLoopMode = false
    override var isShuffleMode = false
    override val allFiles: List<String>
        get() = viewModel.recursiveList(mNavigation.currentDir)

    // region [REGION] PlaylistChoice
    /**
     * For actions based on playlist selection made using choosePlaylist()
     */
    private interface PlaylistChoice {
        fun execute(fileSelection: Int, playlistSelection: Int)
    }

    /**
     * Recursively add current directory to playlist
     */
    private val addCurrentRecursiveChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FilelistActivity,
                viewModel.recursiveList(mNavigation.currentDir),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Recursively add directory to playlist
     */
    private val addRecursiveToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FilelistActivity,
                viewModel.recursiveList(mPlaylistAdapter.getFile(fileSelection)),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Add one file to playlist
     */
    private val addFileToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FilelistActivity,
                mPlaylistAdapter.getFilename(fileSelection),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Add file list to playlist
     */
    private val addFileListToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(fileSelection: Int, playlistSelection: Int) {
            PlaylistUtils.filesToPlaylist(
                this@FilelistActivity,
                mPlaylistAdapter.filenameList,
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }
    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder = ActivityModlistBinding.inflate(layoutInflater)

        setContentView(binder.root)
        setSupportActionBar(binder.appbar.toolbar)
        binder.appbar.toolbarText.text = getString(R.string.browser_filelist_title)

        mNavigation = FilelistNavigation()

        mPlaylistAdapter = PlaylistAdapter(LAYOUT_LIST, false)
        mPlaylistAdapter.onClick = { position -> onClick(position) }
        mPlaylistAdapter.onLongClick = { position -> onLongClick(position) }

        binder.apply {
            upButton.click { parentDir() }
            modlistListview.apply {
                adapter = mPlaylistAdapter
                setHasFixedSize(true)
                addItemDecoration(
                    DividerItemDecoration(this@FilelistActivity, LinearLayoutManager.HORIZONTAL)
                )
            }
            currentPath.apply {
                val textColor = currentTextColor
                setOnTouchListener { view, event ->
                    when (event?.action) {
                        MotionEvent.ACTION_UP -> setTextColor(textColor)
                        else -> setTextColor(resources.color(R.color.pressed_color))
                    }
                    view?.performClick() ?: false
                }
                longClick {
                    onPathClick()
                    true
                }
            }
        }

        // Check if directory exists
        val modDir = File(PrefManager.mediaPath)
        if (modDir.isDirectory) {
            mNavigation.startNavigation(modDir)
            viewModel.updateModList(mNavigation.currentDir)
        } else {
            pathNotFound(PrefManager.mediaPath)
        }

        isShuffleMode = PrefManager.getBooleanPref(OPTIONS_SHUFFLE_MODE, DEFAULT_SHUFFLE_MODE)
        isLoopMode = PrefManager.getBooleanPref(OPTIONS_LOOP_MODE, DEFAULT_LOOP_MODE)

        // Swipe
        setSwipeRefresh(binder.swipeContainer, binder.modlistListview)

        // Play buttons
        setupButtons(binder.listControls)

        lifecycleScope.launchWhenStarted {
            viewModel.listState.collect {
                logD("List State: $it")
                when (it) {
                    FilelistViewModel.FilelistState.None -> Unit // Do nothing
                    FilelistViewModel.FilelistState.Empty -> onEmpty()
                    FilelistViewModel.FilelistState.Load -> onLoad()
                    is FilelistViewModel.FilelistState.AllFiles -> update()
                    is FilelistViewModel.FilelistState.Error -> onError(it.error)
                    is FilelistViewModel.FilelistState.Loaded -> onLoaded(it.list)
                }
            }
        }
    }

    public override fun onDestroy() {
        super.onDestroy()

        logI("Save file list preferences")
        PrefManager.setBooleanPref(OPTIONS_SHUFFLE_MODE, isShuffleMode)
        PrefManager.setBooleanPref(OPTIONS_LOOP_MODE, isLoopMode)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!mNavigation.isAtTopDir) {
                parentDir()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun onClick(position: Int) {
        val file = mPlaylistAdapter.getFile(position)
        if (mNavigation.changeDirectory(file)) {
            mNavigation.saveListPosition(binder.modlistListview)
            viewModel.updateModList(mNavigation.currentDir)
        } else {
            onItemClick(mPlaylistAdapter, position)
        }
    }

    private fun onLongClick(position: Int) {
        val item = mPlaylistAdapter.getFile(position)
        if (item.isDirectory) {
            val items = listOf(
                "Add to playlist",
                "Add to play queue",
                "Play contents",
                "Delete directory"
            )

            MaterialDialog(this).show {
                title(text = "This directory")
                listItemsSingleChoice(items = items) { _, index, _ ->

                    when (index) {
                        0 -> choosePlaylist(position, addRecursiveToPlaylistChoice)
                        1 -> addToQueue(viewModel.recursiveList(mPlaylistAdapter.getFile(position)))
                        2 -> playModule(viewModel.recursiveList(mPlaylistAdapter.getFile(position)))
                        3 -> deleteDirectory(position)
                    }
                }
                positiveButton(R.string.select)
            }
        } else {
            val items = listOf(
                "Add to playlist",
                "Add to play queue",
                "Play this file",
                "Play all starting here",
                "Delete file"
            )

            MaterialDialog(this).show {
                title(text = "This file")
                listItemsSingleChoice(items = items) { _, index, _ ->
                    when (index) {
                        0 -> choosePlaylist(position, addFileToPlaylistChoice)
                        1 -> addToQueue(mPlaylistAdapter.getFilename(position))
                        2 -> playModule(mPlaylistAdapter.getFilename(position))
                        3 -> playModule(mPlaylistAdapter.filenameList, position)
                        4 -> {
                            val deleteName = mPlaylistAdapter.getFilename(position)
                            yesNoDialog(
                                "Delete",
                                "Are you sure you want to delete ${basename(deleteName)}?"
                            ) {
                                if (delete(deleteName)) {
                                    viewModel.updateModList(mNavigation.currentDir)
                                    toast(R.string.msg_file_deleted)
                                } else {
                                    toast(R.string.msg_cant_delete)
                                }
                            }
                        }
                    }
                }
                positiveButton(R.string.select)
            }
        }
    }

    private fun onPathClick() {
        val items = listOf(
            "Add to playlist",
            "Recursive add to playlist",
            "Add to play queue",
            "Set as default path",
            "Clear cache"
        )

        MaterialDialog(this).show {
            title(text = "All files")
            listItemsSingleChoice(items = items) { _, index, _ ->
                when (index) {
                    0 -> choosePlaylist(0, addFileListToPlaylistChoice)
                    1 -> choosePlaylist(0, addCurrentRecursiveChoice)
                    2 -> addToQueue(viewModel.recursiveList(mNavigation.currentDir))
                    3 -> {
                        PrefManager.mediaPath = mNavigation.currentDir!!.path
                        toast("Set as default module path")
                    }
                    4 -> viewModel.clearCachedEntries(mPlaylistAdapter.filenameList)
                }
            }
            positiveButton(R.string.select)
        }
    }

    override fun update() {
        viewModel.updateModList(mNavigation.currentDir)
    }

    private fun pathNotFound(mediaPath: String) {
        MaterialDialog(this).show {
            title(text = "Path not found")
            message(text = "$mediaPath not found. Create this directory or change the module path.")
            positiveButton(R.string.create) {
                val ret = installAssets(mediaPath, PrefManager.installExamples)
                if (ret < 0) {
                    generalError("Error creating directory $mediaPath.")
                }
                mNavigation.startNavigation(File(mediaPath))
                viewModel.updateModList(mNavigation.currentDir)
            }
            negativeButton(R.string.cancel) {
                finish()
            }
        }
    }

    private fun parentDir() {
        if (mNavigation.parentDir()) {
            viewModel.updateModList(mNavigation.currentDir)
            mNavigation.restoreListPosition(binder.modlistListview)
        }
    }

    private fun onLoad() {
        mPlaylistAdapter.submitList(null) // Stop flicker
        binder.modlistSpinner.show()
        binder.currentPath.text = mNavigation.currentDir!!.path
    }

    private fun onEmpty() {
        binder.apply {
            modlistSpinner.hide()
            emptyMessage.show()
            emptyMessage.text = getString(R.string.msg_empty_directory)
        }
    }

    private fun onLoaded(list: List<PlaylistItem>) {
        binder.apply {
            modlistSpinner.hide()
            emptyMessage.hide()
        }
        mPlaylistAdapter.submitList(list)
    }

    private fun onError(error: String?) {
        binder.apply {
            modlistSpinner.hide()
            emptyMessage.show()
            emptyMessage.text = error ?: getString(R.string.msg_unknown_error)
        }
    }

    private fun deleteDirectory(position: Int) {
        val deleteName = mPlaylistAdapter.getFilename(position)
        val mediaPath = PrefManager.mediaPath
        val title = getString(R.string.dialog_title_delete_dir)
        val message = getString(R.string.dialog_msg_delete_dir, basename(deleteName))

        if (deleteName.startsWith(mediaPath) && deleteName != mediaPath) {
            yesNoDialog(title, message) {
                if (deleteRecursive(deleteName)) {
                    viewModel.updateModList(mNavigation.currentDir)
                    toast(getString(R.string.msg_dir_deleted))
                } else {
                    toast(getString(R.string.msg_cant_delete_dir))
                }
            }
        } else {
            toast(R.string.error_dir_not_under_moddir)
        }
    }

    private fun choosePlaylist(fileSelection: Int, choice: PlaylistChoice) {

        // Return if no playlists exist
        if (PlaylistUtils.list().isEmpty()) {
            toast(getString(R.string.msg_no_playlists))
            return
        }

        val playlists = mutableListOf<CharSequence>()
        PlaylistUtils.listNoSuffix().forEach { playlists.add(it) }
        MaterialDialog(this).show {
            title(R.string.msg_select_playlist)
            listItemsSingleChoice(items = playlists) { _, index, _ ->
                choice.execute(fileSelection, index)
            }
            positiveButton(R.string.ok)
            negativeButton(R.string.cancel)
        }
    }

    companion object {
        private const val OPTIONS_SHUFFLE_MODE = "options_shuffleMode"
        private const val OPTIONS_LOOP_MODE = "options_loopMode"
        private const val DEFAULT_SHUFFLE_MODE = true
        private const val DEFAULT_LOOP_MODE = false
    }
}
