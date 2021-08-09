package org.helllabs.android.xmp.ui.browser

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import java.io.File
import java.util.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.databinding.ActivityModlistBinding
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.ui.BasePlaylistActivity
import org.helllabs.android.xmp.ui.BasePlaylistAdapter
import org.helllabs.android.xmp.ui.PlaylistLayoutType
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.util.dialogMessage
import org.helllabs.android.xmp.ui.util.toast
import org.helllabs.android.xmp.ui.util.yesNoDialog
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.FileUtils.basename
import org.helllabs.android.xmp.util.PlaylistUtils.AddFilesResult

// TODO: Replace current path with bread crumb trails
class FilelistActivity : BasePlaylistActivity() {

    private val viewModel: FilelistViewModel by viewModels()

    private lateinit var binder: ActivityModlistBinding

    override var isLoopMode = false
    override var isShuffleMode = false
    override val allFiles: List<String>
        get() = viewModel.recursiveList()

    // region [REGION] PlaylistChoice
    /**
     * For actions based on playlist selection made using choosePlaylist()
     */
    private interface PlaylistChoice {
        suspend fun execute(fileSelection: Int, playlistSelection: Int): AddFilesResult
    }

    /**
     * Recursively add current directory to playlist
     */
    private val addCurrentRecursiveChoice: PlaylistChoice = object : PlaylistChoice {
        override suspend fun execute(fileSelection: Int, playlistSelection: Int): AddFilesResult {
            return PlaylistUtils.filesToPlaylist(
                viewModel.recursiveList(),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Recursively add directory to playlist
     */
    private val addRecursiveToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override suspend fun execute(fileSelection: Int, playlistSelection: Int): AddFilesResult {
            return PlaylistUtils.filesToPlaylist(
                viewModel.recursiveList(mPlaylistAdapter.getFile(fileSelection)),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Add one file to playlist
     */
    private val addFileToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override suspend fun execute(fileSelection: Int, playlistSelection: Int): AddFilesResult {
            return PlaylistUtils.filesToPlaylist(
                mPlaylistAdapter.getFilename(fileSelection),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Add file list to playlist
     */
    private val addFileListToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override suspend fun execute(fileSelection: Int, playlistSelection: Int): AddFilesResult {
            return PlaylistUtils.filesToPlaylist(
                mPlaylistAdapter.getFilenameList(),
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

        mPlaylistAdapter = BasePlaylistAdapter(PlaylistLayoutType.TYPE_LIST, false)

        with(binder) {
            appbar.toolbarText.text = getString(R.string.browser_filelist_title)
            upButton.click { onNavBack() }
            modlistListview.apply {
                adapter = mPlaylistAdapter
                setHasFixedSize(true)
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
        val mediaPath = PrefManager.mediaPath!!
        val modDir = File(mediaPath)
        if (modDir.exists() && modDir.isDirectory) {
            viewModel.navigation.startNavigation(modDir)
            viewModel.updateModList(viewModel.navigation.currentDir)
        } else {
            pathNotFound(mediaPath)
        }

        isShuffleMode = PrefManager.getBooleanPref(OPTIONS_SHUFFLE_MODE, DEFAULT_SHUFFLE_MODE)
        isLoopMode = PrefManager.getBooleanPref(OPTIONS_LOOP_MODE, DEFAULT_LOOP_MODE)

        // Swipe
        setSwipeRefresh(binder.swipeContainer, binder.modlistListview)

        // Play buttons
        setupButtons(binder.listControls)

        lifecycleScope.launchWhenStarted {
            viewModel.listState.collect {
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
            if (!viewModel.navigation.isAtTopDir) {
                onNavBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onClick(position: Int) {
        val file = mPlaylistAdapter.getFile(position)
        val navigation = viewModel.navigation

        if (navigation.changeDirectory(file)) {
            navigation.saveListPosition(binder.modlistListview)
            viewModel.updateModList(navigation.currentDir)
        } else {
            onItemClick(mPlaylistAdapter, position)
        }
    }

    @SuppressLint("CheckResult")
    override fun onLongClick(position: Int) {
        val item = mPlaylistAdapter.getFile(position)
        if (item.isDirectory) {
            MaterialDialog(this).show {
                title(R.string.dialog_this_dir_title)
                listItemsSingleChoice(R.array.fileList_this_directory_array) { _, index, _ ->
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
            MaterialDialog(this).show {
                title(R.string.dialog_this_file_title)
                listItemsSingleChoice(R.array.fileList_this_files_array) { _, index, _ ->
                    when (index) {
                        0 -> choosePlaylist(position, addFileToPlaylistChoice)
                        1 -> addToQueue(mPlaylistAdapter.getFilename(position))
                        2 -> playModule(mPlaylistAdapter.getFilename(position))
                        3 -> playModule(mPlaylistAdapter.getFilenameList(), position)
                        4 -> {
                            val deleteName = mPlaylistAdapter.getFilename(position)
                            yesNoDialog(
                                this@FilelistActivity,
                                getString(R.string.dialog_this_file_title_confirm),
                                getString(R.string.dialog_this_file_message, basename(deleteName))
                            ) {
                                if (FileUtils.delete(deleteName)) {
                                    viewModel.updateModList(viewModel.navigation.currentDir)
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

    @SuppressLint("CheckResult")
    private fun onPathClick() {
        MaterialDialog(this).show {
            title(R.string.dialog_all_files_title)
            listItemsSingleChoice(R.array.fileList_all_files_array) { _, index, _ ->
                when (index) {
                    0 -> choosePlaylist(0, addFileListToPlaylistChoice)
                    1 -> choosePlaylist(0, addCurrentRecursiveChoice)
                    2 -> addToQueue(viewModel.recursiveList())
                    3 -> {
                        PrefManager.mediaPath = viewModel.navigation.currentDir!!.path
                        toast(R.string.msg_default_path_set)
                    }
                }
            }
            positiveButton(R.string.select)
        }
    }

    override fun update() {
        viewModel.updateModList(viewModel.navigation.currentDir)
    }

    private fun onLoad() {
        mPlaylistAdapter.submitList(null) // Stop flicker
        binder.errorLayout.layout.hide()
        binder.modlistSpinner.show()
        binder.currentPath.text = viewModel.navigation.currentDir?.path
            ?: "..." // Could be non-existent.
    }

    private fun onEmpty() {
        binder.apply {
            modlistSpinner.hide()
            errorLayout.layout.show()
            errorLayout.message.text = getString(R.string.msg_empty_directory)
        }
    }

    private fun onLoaded(list: List<PlaylistItem>) {
        binder.apply {
            modlistSpinner.hide()
            errorLayout.layout.hide()
        }
        mPlaylistAdapter.submitList(list)
    }

    private fun onError(error: String?) {
        binder.apply {
            modlistSpinner.hide()
            errorLayout.layout.show()
            errorLayout.message.text = error ?: getString(R.string.msg_unknown_error)
        }
    }

    private fun onNavBack() {
        viewModel.parentDir {
            it.restoreListPosition(binder.modlistListview)
        }
    }

    private fun pathNotFound(mediaPath: String) {
        MaterialDialog(this).show {
            title(R.string.dialog_no_path_title)
            message(text = getString(R.string.dialog_no_path_message, mediaPath))
            positiveButton(R.string.create) {
                val ret = FileUtils.installAssets(context, mediaPath, PrefManager.installExamples)
                if (ret < 0) {
                    dialogMessage(
                        lifecycleOwner = this@FilelistActivity,
                        message = getString(R.string.msg_error_create_directory, mediaPath)
                    )
                }
                viewModel.navigation.startNavigation(File(mediaPath))
                viewModel.updateModList(viewModel.navigation.currentDir)
            }
            negativeButton(R.string.cancel) {
                finish()
            }
        }
    }

    private fun deleteDirectory(position: Int) {
        val deleteName = mPlaylistAdapter.getFilename(position)
        val mediaPath = PrefManager.mediaPath!!

        if (deleteName.startsWith(mediaPath) && deleteName != mediaPath) {
            yesNoDialog(
                this,
                getString(R.string.dialog_title_delete_dir),
                getString(R.string.dialog_msg_delete_dir, basename(deleteName))
            ) {
                if (FileUtils.deleteRecursive(deleteName)) {
                    viewModel.updateModList(viewModel.navigation.currentDir)
                    toast(getString(R.string.msg_dir_deleted))
                } else {
                    toast(getString(R.string.msg_cant_delete_dir))
                }
            }
        } else {
            toast(R.string.error_dir_not_under_moddir)
        }
    }

    @SuppressLint("CheckResult")
    private fun choosePlaylist(fileSelection: Int, choice: PlaylistChoice) {
        var result: AddFilesResult = AddFilesResult.RESULT_OK

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
                lifecycleScope.launch {
                    result = choice.execute(fileSelection, index)
                }
            }
            positiveButton(R.string.ok)
            negativeButton(R.string.cancel)
        }

        when (result) {
            AddFilesResult.RESULT_OK -> Unit
            AddFilesResult.RESULT_IO_EXCEPTION -> {
                dialogMessage(
                    lifecycleOwner = this,
                    message = getString(R.string.error_write_to_playlist)
                )
            }
            AddFilesResult.RESULT_SINGLE_UNRECOGNIZED -> {
                dialogMessage(
                    lifecycleOwner = this,
                    message = getString(R.string.unrecognized_format)
                )
            }
            AddFilesResult.RESULT_OK_VALID_ONLY -> {
                toast(R.string.msg_only_valid_files_added)
            }
        }
    }

    companion object {
        private const val OPTIONS_SHUFFLE_MODE = "options_shuffleMode"
        private const val OPTIONS_LOOP_MODE = "options_loopMode"
        private const val DEFAULT_SHUFFLE_MODE = true
        private const val DEFAULT_LOOP_MODE = false
    }
}
