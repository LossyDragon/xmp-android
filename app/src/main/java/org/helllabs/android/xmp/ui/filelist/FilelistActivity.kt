package org.helllabs.android.xmp.ui.filelist

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.view.WindowCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.accompanist.insets.navigationBarsPadding
import java.io.File
import java.util.*
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.ui.BasePlaylistActivity
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.theme.XmpTheme
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.FileUtils.basename
import org.helllabs.android.xmp.util.PlaylistUtils.AddFilesResult

class FilelistActivity : BasePlaylistActivity() {

    private val viewModel: FilelistViewModel by viewModels()

    override var isLoopMode: Boolean
        get() = PrefManager.fileListLoop
        set(value) {
            PrefManager.fileListLoop = value
        }
    override var isShuffleMode: Boolean
        get() = PrefManager.fileListShuffle
        set(value) {
            PrefManager.fileListShuffle = value
        }

    // region [REGION] PlaylistChoice
    /**
     * For actions based on playlist selection made using choosePlaylist()
     */
    private interface PlaylistChoice {
        fun execute(
            fileSelection: Int,
            playlistSelection: Int,
            list: List<PlaylistItem>
        ): AddFilesResult
    }

    /**
     * Recursively add current directory to playlist
     */
    private val addCurrentRecursiveChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(
            fileSelection: Int,
            playlistSelection: Int,
            list: List<PlaylistItem>
        ): AddFilesResult {
            return PlaylistUtils.filesToPlaylist(
                viewModel.recursiveList(File(viewModel.currentFile.value)),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Recursively add directory to playlist
     */
    private val addRecursiveToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(
            fileSelection: Int,
            playlistSelection: Int,
            list: List<PlaylistItem>
        ): AddFilesResult {
            return PlaylistUtils.filesToPlaylist(
                viewModel.recursiveList(list[fileSelection].file!!),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Add one file to playlist
     */
    private val addFileToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(
            fileSelection: Int,
            playlistSelection: Int,
            list: List<PlaylistItem>
        ): AddFilesResult {
            return PlaylistUtils.filesToPlaylist(
                list[fileSelection].file!!.path,
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }

    /**
     * Add file list to playlist
     */
    private val addFileListToPlaylistChoice: PlaylistChoice = object : PlaylistChoice {
        override fun execute(
            fileSelection: Int,
            playlistSelection: Int,
            list: List<PlaylistItem>
        ): AddFilesResult {
            return PlaylistUtils.filesToPlaylist(
                PlaylistUtils.getFilePathList(list),
                PlaylistUtils.getPlaylistName(playlistSelection)
            )
        }
    }
// endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        logD("onCreate")
        setContent {
            var isLoop: Boolean by remember { mutableStateOf(isLoopMode) }
            var isShuffle: Boolean by remember { mutableStateOf(isShuffleMode) }

            FileListLayout(
                viewModel = viewModel,
                onBack = { onBackPressed() },
                onClick = { index, file, list ->
                    onItemClick(
                        index,
                        file.path,
                        PlaylistUtils.getDirectoryCount(list),
                        PlaylistUtils.getFilePathList(list)
                    )
                },
                onLongClick = { index, file, list ->
                    if (file.isDirectory) {
                        dialogDirectory(index, file, list)
                    } else {
                        dialogFile(index, file, list)
                    }
                },
                onCrumbLongClick = { path, list ->
                    dialogPath(path, list)
                },
                onPlay = {
                    playModule(it)
                },
                onLoop = {
                    isLoop = it // Force recompose
                    isLoopMode = it
                },
                onShuffle = {
                    isShuffle = it // Force recompose
                    isShuffleMode = it
                },
                isLoop = isLoop,
                isShuffle = isShuffle,
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            File(viewModel.currentFile.value).parentFile?.let {
                if (it.path != "/") {
                    viewModel.getDirectoryList(it)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    @SuppressLint("CheckResult")
    private fun dialogDirectory(position: Int, file: File, list: List<PlaylistItem>) {
        MaterialDialog(this).show {
            lifecycleOwner(this@FilelistActivity)
            title(R.string.dialog_this_dir_title)
            listItemsSingleChoice(R.array.fileList_this_directory_array) { _, index, _ ->
                when (index) {
                    // Add to playlist
                    0 -> choosePlaylist(position, addRecursiveToPlaylistChoice, list)
                    // Add to play queue
                    1 -> addToQueue(viewModel.recursiveList(file))
                    // Play contents
                    2 -> playModule(viewModel.recursiveList(file))
                    // Delete directory
                    3 -> deleteDirectory(file)
                }
            }
            positiveButton(R.string.select)
        }
    }

    @SuppressLint("CheckResult")
    private fun dialogFile(position: Int, file: File, list: List<PlaylistItem>) {
        MaterialDialog(this).show {
            lifecycleOwner(this@FilelistActivity)
            title(R.string.dialog_this_file_title)
            listItemsSingleChoice(R.array.fileList_this_files_array) { _, index, _ ->
                when (index) {
                    // Add to playlist
                    0 -> choosePlaylist(position, addFileToPlaylistChoice, list)
                    // Add to play queue
                    1 -> addToQueue(file.path.toList())
                    // Play this file
                    2 -> playModule(file.path.toList())
                    // Play all starting here
                    3 -> {
                        val dirCount = PlaylistUtils.getDirectoryCount(list)
                        playModule(
                            modList = PlaylistUtils.getFilePathList(list),
                            start = position - dirCount,
                            keepFirst = true
                        )
                    }
                    // Delete file
                    4 -> {
                        val deleteName = file.name
                        yesNoDialog(
                            lifecycleOwner = this@FilelistActivity,
                            title = getString(R.string.dialog_this_file_title_confirm),
                            message = getString(
                                R.string.dialog_this_file_message,
                                basename(deleteName)
                            ),
                            onPositiveButton = {
                                if (FileUtils.delete(file)) {
                                    with(viewModel) {
                                        getDirectoryList(File(currentFile.value))
                                    }
                                    toast(R.string.msg_file_deleted)
                                } else {
                                    toast(R.string.msg_cant_delete)
                                }
                            }
                        )
                    }
                }
            }
            positiveButton(R.string.select)
        }
    }

    @SuppressLint("CheckResult")
    private fun dialogPath(filePath: String, list: List<PlaylistItem>) {
        val file = File(filePath)
        MaterialDialog(this).show {
            lifecycleOwner(this@FilelistActivity)
            title(R.string.dialog_all_files_title)
            listItemsSingleChoice(R.array.fileList_all_files_array) { _, index, _ ->
                when (index) {
                    // Add to playlist
                    0 -> choosePlaylist(0, addFileListToPlaylistChoice, list)
                    // Recursive add to playlist
                    1 -> choosePlaylist(0, addCurrentRecursiveChoice, list)
                    // Add to play queue
                    2 -> addToQueue(viewModel.recursiveList(file))
                    // Set as default path
                    3 -> {
                        PrefManager.mediaPath = file.path
                        viewModel.getDirectoryList(File(PrefManager.mediaPath!!))
                        toast(R.string.msg_default_path_set)
                    }
                }
            }
            positiveButton(R.string.select)
        }
    }

    private fun deleteDirectory(file: File) {
        val deletePath = file.path
        val mediaPath = PrefManager.mediaPath!!
        if (deletePath.contains(mediaPath) && deletePath != mediaPath) {
            yesNoDialog(
                lifecycleOwner = this@FilelistActivity,
                title = getString(R.string.dialog_title_delete_dir),
                message = getString(R.string.dialog_msg_delete_dir, basename(file.name)),
                onPositiveButton = {
                    if (FileUtils.deleteRecursive(deletePath)) {
                        viewModel.getDirectoryList(File(viewModel.currentFile.value))
                        toast(getString(R.string.msg_dir_deleted))
                    } else {
                        toast(getString(R.string.msg_cant_delete_dir))
                    }
                }
            )
        } else {
            toast(R.string.error_dir_not_under_moddir)
        }
    }

    @SuppressLint("CheckResult")
    private fun choosePlaylist(
        fileSelection: Int,
        choice: PlaylistChoice,
        list: List<PlaylistItem>
    ) {
        // Return if no playlists exist
        if (PlaylistUtils.list().isEmpty()) {
            toast(getString(R.string.msg_no_playlists))
            return
        }

        val playlists = mutableListOf<CharSequence>()
        PlaylistUtils.listNoSuffix().forEach { playlists.add(it) }
        playlists.sortBy { it.toString().lowercase() }
        MaterialDialog(this).show {
            lifecycleOwner(this@FilelistActivity)
            title(R.string.msg_select_playlist)
            listItemsSingleChoice(items = playlists) { _, index, _ ->
                val result = choice.execute(fileSelection, index, list)
                logD("Select Playlist result: $result")
                when (result) {
                    AddFilesResult.RESULT_OK -> Unit
                    AddFilesResult.RESULT_IO_EXCEPTION -> {
                        dialogMessage(
                            lifecycleOwner = this@FilelistActivity,
                            message = getString(R.string.error_write_to_playlist)
                        )
                    }
                    AddFilesResult.RESULT_SINGLE_UNRECOGNIZED -> {
                        dialogMessage(
                            lifecycleOwner = this@FilelistActivity,
                            message = getString(R.string.unrecognized_format)
                        )
                    }
                    AddFilesResult.RESULT_OK_VALID_ONLY -> {
                        toast(R.string.msg_only_valid_files_added)
                    }
                }
            }
            positiveButton(R.string.ok)
            negativeButton(R.string.cancel)
        }
    }
}

@Composable
private fun FileListLayout(
    viewModel: FilelistViewModel,
    onBack: () -> Unit,
    onClick: (index: Int, file: File, list: List<PlaylistItem>) -> Unit,
    onLongClick: (index: Int, file: File, list: List<PlaylistItem>) -> Unit,
    onCrumbLongClick: (path: String, list: List<PlaylistItem>) -> Unit,
    onPlay: (items: List<String>) -> Unit,
    onLoop: (value: Boolean) -> Unit,
    onShuffle: (value: Boolean) -> Unit,
    isLoop: Boolean,
    isShuffle: Boolean,
) {
    XmpTheme(onlyStyleStatusBar = true) {
        val scope = rememberCoroutineScope()
        val scaffoldState = rememberScaffoldState()
        val context = LocalContext.current
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            scaffoldState = scaffoldState,
            snackbarHost = { scaffoldState.snackbarHostState },
            topBar = {
                AppBar(
                    title = stringResource(id = R.string.browser_filelist_title),
                    navIconClick = { onBack() },
                )
            },
            bottomBar = {
                LayoutControls(
                    modifier = Modifier,
                    onPlay = {
                        val items = viewModel.recursiveList(File(viewModel.currentFile.value))
                        if (items.isNullOrEmpty()) {
                            scope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.error_no_files_to_play),
                                    actionLabel = context.getString(R.string.ok)
                                )
                            }
                            context.logD("onPlay Empty")
                        } else {
                            context.logD("onPlay $items")
                            onPlay(items)
                        }
                    },
                    onLoop = { onLoop(!isLoop) },
                    onShuffle = { onShuffle(!isShuffle) },
                    isLoopEnabled = isLoop,
                    isShuffleEnabled = isShuffle,
                )
            },
        ) {
            ConstraintLayout(
                modifier = Modifier.fillMaxHeight()
            ) {
                val (crumb, list, snack) = createRefs()
                var itemList by remember { mutableStateOf(listOf<PlaylistItem>()) }
                val currentPath = viewModel.currentFile.value
                val fileListState = viewModel.listState.collectAsState()

                BreadCrumbLayout(
                    modifier = Modifier.constrainAs(crumb) {
                        width = Dimension.fillToConstraints
                        top.linkTo(parent.top)
                    },
                    viewModel = viewModel,
                    onCrumbLongClick = { onCrumbLongClick(it, itemList) }
                )

                LazyList(
                    modifier = Modifier
                        .padding(it)
                        .constrainAs(list) {
                            height = Dimension.fillToConstraints
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            top.linkTo(crumb.bottom)
                            bottom.linkTo(parent.bottom)
                        },
                    showScrollAt = 10,
                    shouldPadBottom = false,
                    boxContent = {
                        when (val state = fileListState.value) {
                            FilelistViewModel.FileListState.None -> Unit
                            FilelistViewModel.FileListState.Load -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            FilelistViewModel.FileListState.NotFound -> {
                                ErrorLayout("Directory not found")
                                context.yesNoDialog(
                                    lifecycleOwner = LocalLifecycleOwner.current,
                                    title = stringResource(id = R.string.dialog_no_path_title),
                                    message = stringResource(
                                        id = R.string.dialog_no_path_message,
                                        currentPath
                                    ),
                                    positiveButton = R.string.create,
                                    negativeButton = R.string.cancel,
                                    onPositiveButton = {
                                        val ret = FileUtils.installAssets(
                                            context,
                                            currentPath,
                                            PrefManager.installExamples
                                        )
                                        if (ret < 0) {
                                            scope.launch {
                                                scaffoldState.snackbarHostState.showSnackbar(
                                                    message = context.getString(
                                                        R.string.msg_error_create_directory,
                                                        currentPath
                                                    ),
                                                    actionLabel = context.getString(R.string.ok)
                                                )
                                            }
                                        } else {
                                            val file = File(viewModel.currentFile.value)
                                            viewModel.getDirectoryList(file)
                                        }
                                    }
                                )
                            }
                            is FilelistViewModel.FileListState.Error -> {
                                ErrorLayout(state.error)
                            }
                            is FilelistViewModel.FileListState.Loaded -> {
                                itemList = state.list
                                if (itemList.isEmpty())
                                    ErrorLayout(stringResource(id = R.string.msg_empty_directory))
                            }
                        }
                    },
                    lazyContent = {
                        itemsIndexed(items = itemList) { index, item ->
                            ItemList(
                                item = item,
                                isDraggable = false,
                                onClick = {
                                    if (item.file!!.isDirectory)
                                        viewModel.getDirectoryList(item.file!!)
                                    else
                                        onClick(index, item.file!!, itemList)
                                },
                                onLongClick = { onLongClick(index, item.file!!, itemList) }
                            )
                        }
                    }
                )

                Snackbar(
                    modifier = Modifier
                        .padding(it)
                        .constrainAs(snack) {
                            width = Dimension.fillToConstraints
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(list.bottom)
                        },
                    snackBarState = scaffoldState.snackbarHostState,
                    onDismiss = {
                        scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun BreadCrumbLayout(
    modifier: Modifier,
    viewModel: FilelistViewModel,
    onCrumbLongClick: (path: String) -> Unit,
) {
    val listState = rememberLazyListState()
    val crumbs = viewModel.crumbState.value
    Column(modifier) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(crumbs) { item ->
                ItemBreadCrumb(
                    crumb = item.name,
                    onClick = { viewModel.getDirectoryList(File(item.path)) },
                    onLongClick = { onCrumbLongClick(item.path) }
                )
            }
        }
        Divider(Modifier.fillMaxWidth())
    }

    if (crumbs.isNotEmpty()) // Stop a rare crash if crumbs is somehow empty.
        LaunchedEffect(crumbs) {
            listState.animateScrollToItem(crumbs.size)
        }
}

/************
 * Previews *
 ************/

// @Preview(name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
// @Composable
// fun FileListPreview() {
//    FileListLayout(
//        viewModel =,
//        onBack = { },
//        onClick = { _, _, _ -> },
//        onLongClick = { _, _, _ -> },
//        onCrumbLongClick = { _, _ -> },
//        onPlay = {},
//        onLoop = {},
//        onShuffle = {},
//        isLoop = true,
//        isShuffle = true,
//    )
// }
