package org.helllabs.android.xmp.ui.explorer

import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.vanpra.composematerialdialogs.*
import java.io.File
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.BreadCrumb
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.ui.BasePlaylistActivity
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.ui.theme.sectionBackgroundDark
import org.helllabs.android.xmp.util.*
import org.helllabs.android.xmp.util.FileUtils.basename
import org.helllabs.android.xmp.util.PlaylistUtils.AddFilesResult

class FileExplorerActivity : BasePlaylistActivity() {

    private val viewModel: FileExplorerViewModel by viewModels()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        logD("onCreate")
        setContent {
            var isLoop: Boolean by remember { mutableStateOf(isLoopMode) }
            var isShuffle: Boolean by remember { mutableStateOf(isShuffleMode) }

            // TODO this is just dumb
            var _index: Int by remember { mutableStateOf(0) }
            var _file: File? by remember { mutableStateOf(null) }
            var _list: List<PlaylistItem> by remember { mutableStateOf(listOf()) }

            val choosePlaylistState = rememberMaterialDialogState()
            DialogChoosePlaylist(
                dialogState = choosePlaylistState,
                block = {
                    val result = _choice!!.execute(fileSelection, _index, _list)
                    logD("Select Playlist result: $result")
                    when (result) {
                        AddFilesResult.RESULT_OK -> Unit
                        AddFilesResult.RESULT_IO_EXCEPTION -> {
                            dialogMessage(
                                lifecycleOwner = this@FileExplorerActivity,
                                message = getString(R.string.error_write_to_playlist)
                            )
                        }
                        AddFilesResult.RESULT_SINGLE_UNRECOGNIZED -> {
                            dialogMessage(
                                lifecycleOwner = this@FileExplorerActivity,
                                message = getString(R.string.unrecognized_format)
                            )
                        }
                        AddFilesResult.RESULT_OK_VALID_ONLY -> {
                            toast(R.string.msg_only_valid_files_added)
                        }
                    }
                }
            )

            val deleteDirectoryState = rememberMaterialDialogState()
            DialogMessage(
                dialogState = deleteDirectoryState,
                title = R.string.dialog_title_delete_dir,
                messageText = stringResource(
                    id = R.string.dialog_msg_delete_dir,
                    basename(_file!!.name)
                ),
                positiveButtonText = R.string.ok,
                negativeButtonText = R.string.cancel,
                onPositiveButton = {
                    if (FileUtils.deleteRecursive(_file!!.path)) {
                        viewModel.getDirectoryList(File(viewModel.currentFile.value))
                        toast(getString(R.string.msg_dir_deleted))
                    } else {
                        toast(getString(R.string.msg_cant_delete_dir))
                    }
                }
            )

            val dialogDirectoryState = rememberMaterialDialogState()
            DialogDirectory(
                dialogState = dialogDirectoryState,
                block = {
                    when (it) {
                        // Add to playlist
                        0 -> {
                            _choice = addRecursiveToPlaylistChoice
                            choosePlaylistState.show()
                            choosePlaylist(_index, addRecursiveToPlaylistChoice, _list)
                        }
                        // Add to play queue
                        1 -> addToQueue(viewModel.recursiveList(_file))
                        // Play contents
                        2 -> playModule(viewModel.recursiveList(_file))
                        // Delete directory
                        3 -> {
                            val mediaPath = PrefManager.mediaPath!!
                            if (_file!!.path.contains(mediaPath) && _file!!.path != mediaPath)
                                dialogDirectoryState.show()
                            else
                                toast(R.string.error_dir_not_under_moddir)
                        }
                    }
                }
            )

            val deleteFileState = rememberMaterialDialogState()
            DialogMessage(
                dialogState = deleteFileState,
                title = R.string.dialog_this_file_title_confirm,
                messageText = stringResource(
                    id = R.string.dialog_this_file_message,
                    basename(_file!!.name)
                ),
                positiveButtonText = R.string.ok,
                negativeButtonText = R.string.cancel,
                onPositiveButton = {
                    if (FileUtils.delete(_file)) {
                        with(viewModel) {
                            getDirectoryList(File(currentFile.value))
                        }
                        toast(R.string.msg_file_deleted)
                    } else {
                        toast(R.string.msg_cant_delete)
                    }
                }
            )

            val dialogFileState = rememberMaterialDialogState()
            DialogFile(
                dialogState = dialogFileState,
                block = {
                    when (it) {
                        // Add to playlist
                        0 -> choosePlaylist(_index, addFileToPlaylistChoice, _list)
                        // Add to play queue
                        1 -> addToQueue(_file!!.path.toList())
                        // Play this file
                        2 -> playModule(_file!!.path.toList())
                        // Play all starting here
                        3 -> playModule(
                            modList = PlaylistUtils.getFilePathList(_list),
                            start = _index - PlaylistUtils.getDirectoryCount(_list),
                            keepFirst = true
                        )
                        // Delete file
                        4 -> deleteFileState.show()
                    }
                }
            )

            val dialogPathState = rememberMaterialDialogState()
            DialogPath(
                dialogState = dialogPathState,
                block = {
                    when (it) {
                        // Add to playlist
                        0 -> choosePlaylist(0, addFileListToPlaylistChoice, _list)
                        // Recursive add to playlist
                        1 -> {
                            val file = File(viewModel.currentFile.value)
                            PlaylistUtils.run {
                                filesToPlaylist(
                                    viewModel.recursiveList(file),
                                    getPlaylistName()
                                )
                                choosePlaylistState

                                choosePlaylist(0, addCurrentRecursiveChoice, _list)
                            }
                        }
                        // Add to play queue
                        2 -> addToQueue(viewModel.recursiveList(_file))
                        // Set as default path
                        3 -> {
                            PrefManager.mediaPath = _file!!.path
                            viewModel.getDirectoryList(File(PrefManager.mediaPath!!))
                            toast(R.string.msg_default_path_set)
                        }
                    }
                }
            )

            ProvideWindowInsets(consumeWindowInsets = false) {
                FileListScreen(
                    viewModel = viewModel,
                    isLoop = isLoop,
                    isShuffle = isShuffle,
                    onBack = { onBackPressed() },
                    onCrumbLongClick = { file, list ->
                        _file = File(file)
                        _list = list

                        dialogPathState.show()
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
                    onClick = { index, file, list ->
                        onItemClick(
                            index,
                            file.path,
                            PlaylistUtils.getDirectoryCount(list),
                            PlaylistUtils.getFilePathList(list)
                        )
                    },
                    onLongClick = { index, file, list ->
                        _index = index
                        _file = file
                        _list = list

                        if (file.isDirectory) {
                            dialogDirectoryState.show()
                        } else {
                            dialogFileState.show()
                        }
                    },
                )
            }
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
}

@Composable
private fun FileListScreen(
    viewModel: FileExplorerViewModel,
    isLoop: Boolean,
    isShuffle: Boolean,
    onBack: () -> Unit,
    onClick: (index: Int, file: File, list: List<PlaylistItem>) -> Unit,
    onLongClick: (index: Int, file: File, list: List<PlaylistItem>) -> Unit,
    onShuffle: (value: Boolean) -> Unit,
    onLoop: (value: Boolean) -> Unit,
    onPlay: (items: List<String>) -> Unit,
    onCrumbLongClick: (path: String, list: List<PlaylistItem>) -> Unit,
) {
    val context = LocalContext.current
    val fileListState = viewModel.listState.collectAsState()

    val getDirectoryList: (file: File) -> Unit = {
        viewModel.getDirectoryList(it)
    }

    FileListLayout(
        crumbList = viewModel.crumbState.value,
        listState = fileListState.value,
        currentPath = PrefManager.mediaPath!!,
        isLoop = isLoop,
        isShuffle = isShuffle,
        onBack = onBack,
        onClick = { index, file, list ->
            onClick(index, file, list)
        },
        onLongClick = { index, file, list ->
            onLongClick(index, file, list)
        },
        onLoop = { onLoop(it) },
        onShuffle = { onShuffle(it) },
        onGetDirectory = { getDirectoryList(it!!) },
        onCrumbLongClick = { path, list ->
            onCrumbLongClick(path, list)
        },
        onPlay = {
            val items = viewModel.recursiveList(File(viewModel.currentFile.value))
            if (items.isNullOrEmpty()) {
                context.toast(R.string.error_no_files_to_play)
                context.logD("onPlay empty")
            } else {
                context.logD("onPlay $items")
                onPlay(items)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileListLayout(
    crumbList: List<BreadCrumb>,
    currentPath: String,
    isLoop: Boolean,
    isShuffle: Boolean,
    listState: FileExplorerViewModel.FileListState,
    onBack: () -> Unit,
    onClick: (index: Int, file: File, list: List<PlaylistItem>) -> Unit,
    onCrumbLongClick: (path: String, list: List<PlaylistItem>) -> Unit,
    onGetDirectory: (file: File?) -> Unit,
    onLongClick: (index: Int, file: File, list: List<PlaylistItem>) -> Unit,
    onLoop: (value: Boolean) -> Unit,
    onPlay: () -> Unit,
    onShuffle: (value: Boolean) -> Unit,
) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }
    var itemList by remember { mutableStateOf(listOf<PlaylistItem>()) }

    val uiController = rememberSystemUiController()
    SideEffect {
        uiController.setNavigationBarColor(color = sectionBackgroundDark)
    }

    XmpTheme3 {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            // Top App Bar
            val rotation = LocalConfiguration.current.orientation
            val appBarModifier =
                if (rotation == Configuration.ORIENTATION_PORTRAIT) Modifier.statusBarsPadding()
                else Modifier.systemBarsPadding()

            XmpAppBar3(
                modifier = appBarModifier,
                titleText = stringResource(id = R.string.browser_filelist_title),
                scrollBehavior = scrollBehavior,
                onNavIconPressed = onBack,
            )

            Surface {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    BreadCrumbLayout(
                        scrollBehavior = scrollBehavior,
                        breadCrumbs = crumbList,
                        onCrumbClick = {
                            val file = File(it)
                            onGetDirectory(file)
                        },
                        onCrumbLongClick = { onCrumbLongClick(it, itemList) }
                    )

                    Box(
                        modifier = Modifier.weight(.5f)
                    ) {
                        ExplorerList(
                            itemList = itemList,
                            listState = listState,
                            onListChanged = {
                                itemList = it
                            },
                            currentPath = currentPath,
                            onClick = { index, file, list ->
                                onClick(index, file, list)
                            },
                            onLongClick = { index, file, list ->
                                onLongClick(index, file, list)
                            },
                            onGetDirectory = {
                                onGetDirectory(it)
                            }
                        )
                    }

                    LayoutControls(
                        modifier = Modifier,
                        onPlay = { onPlay() },
                        onLoop = { onLoop(!isLoop) },
                        onShuffle = { onShuffle(!isShuffle) },
                        isLoopEnabled = isLoop,
                        isShuffleEnabled = isShuffle,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplorerList(
    modifier: Modifier = Modifier,
    listState: FileExplorerViewModel.FileListState,
    itemList: List<PlaylistItem>,
    onListChanged: (list: List<PlaylistItem>) -> Unit,
    onClick: (index: Int, file: File, list: List<PlaylistItem>) -> Unit,
    onLongClick: (index: Int, file: File, list: List<PlaylistItem>) -> Unit,
    onGetDirectory: (file: File?) -> Unit,
    currentPath: String
) {
    val assets = LocalContext.current.assets
    val context = LocalContext.current

    val fileNotFoundState = rememberMaterialDialogState()
    DialogMessage(
        dialogState = fileNotFoundState,
        title = R.string.dialog_no_path_title,
        messageText = stringResource(id = R.string.dialog_no_path_message, currentPath),
        positiveButtonText = R.string.create,
        negativeButtonText = R.string.cancel,
        onPositiveButton = {
            val ret = FileUtils.installAssets(
                assets,
                currentPath,
                PrefManager.installExamples
            )
            if (ret < 0) {
                with(context) {
                    toast(getString(R.string.msg_error_create_directory, currentPath))
                }
            } else {
                val file = File(currentPath)
                onGetDirectory(file)
            }
        },
        onDismiss = { fileNotFoundState.hide() }
    )

    LazyList(
        modifier = modifier,
        shouldPadBottom = false,
        boxContent = {
            when (listState) {
                FileExplorerViewModel.FileListState.None -> Unit
                FileExplorerViewModel.FileListState.Load -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                FileExplorerViewModel.FileListState.NotFound -> {
                    ErrorLayout(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp)
                            .fillMaxSize(),
                        "Directory not found"
                    )
                    fileNotFoundState.show()
                }
                is FileExplorerViewModel.FileListState.Error -> {
                    ErrorLayout(
                        modifier = Modifier.fillMaxSize(),
                        message = listState.error
                    )
                }
                is FileExplorerViewModel.FileListState.Loaded -> {
                    onListChanged(listState.list)
                    if (itemList.isEmpty())
                        ErrorLayout(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp)
                                .fillMaxSize(),
                            message = stringResource(id = R.string.msg_empty_directory)
                        )
                }
            }
        },
        lazyContent = {
            itemsIndexed(items = itemList) { index, item ->
                ItemList(
                    item = item,
                    onClick = {
                        if (item.file!!.isDirectory)
                            onGetDirectory(item.file)
                        else
                            onClick(index, item.file!!, itemList)
                    },
                    onLongClick = { onLongClick(index, item.file!!, itemList) }
                )
            }
        }
    )
}

@Composable
private fun BreadCrumbLayout(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    breadCrumbs: List<BreadCrumb>,
    onCrumbClick: (path: String) -> Unit,
    onCrumbLongClick: (path: String) -> Unit,
) {
    val backgroundColors = TopAppBarDefaults.centerAlignedTopAppBarColors()
    val backgroundColor = backgroundColors.containerColor(
        scrollFraction = scrollBehavior?.scrollFraction ?: 0f
    ).value

    val listState = rememberLazyListState()
    Column(modifier.background(backgroundColor)) {
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.inverseSurface
        )
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
            state = listState,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(breadCrumbs) { item ->
                ItemBreadCrumb(
                    crumb = item.name,
                    onClick = { onCrumbClick(item.path) },
                    onLongClick = { onCrumbLongClick(item.path) }
                )
            }
        }
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.inverseSurface
        )
    }

    if (breadCrumbs.isNotEmpty()) // Stop a rare crash if crumbs is somehow empty.
        LaunchedEffect(breadCrumbs) {
            listState.animateScrollToItem(breadCrumbs.size)
        }
}

@Composable
private fun DialogDirectory(
    dialogState: MaterialDialogState,
    block: (index: Int) -> Unit
) {
    val resources = LocalContext.current.resources
    val array = resources.getStringArray(R.array.fileList_this_directory_array)

    MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton(res = R.string.select)
        },
    ) {
        title(res = R.string.dialog_this_dir_title)
        listItemsSingleChoice(list = array.toList()) { index ->
            block(index)
        }
    }
}

@Composable
fun DialogFile(
    dialogState: MaterialDialogState,
    block: (index: Int) -> Unit
) {
    val resources = LocalContext.current.resources
    val array = resources.getStringArray(R.array.fileList_this_files_array)

    MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton(res = R.string.select)
        }
    ) {
        title(res = R.string.dialog_this_file_title)
        listItemsSingleChoice(list = array.toList()) { index ->
            block(index)
        }
    }
}

@Composable
private fun DialogPath(
    dialogState: MaterialDialogState,
    block: (index: Int) -> Unit,
) {
    val resources = LocalContext.current.resources
    val array = resources.getStringArray(R.array.fileList_all_files_array)

    MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton(res = R.string.select)
        }
    ) {
        title(res = R.string.dialog_all_files_title)
        listItemsSingleChoice(list = array.toList()) { index ->
            block(index)
        }
    }
}

@Composable
private fun DialogChoosePlaylist(
    dialogState: MaterialDialogState,
    block: (index: Int) -> Unit
) {
    val context = LocalContext.current
    // Return if no playlists exist
    if (PlaylistUtils.list().isEmpty()) {
        context.toast(R.string.msg_no_playlists)
        dialogState.hide()
    }

    MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton(res = R.string.select)
            negativeButton(res = R.string.cancel)
        },
    ) {
        val playlists = PlaylistUtils.listNoSuffix().map { it }
        title(res = R.string.msg_select_playlist)
        listItemsSingleChoice(list = playlists.sortedBy { it.lowercase() }) { index ->
            block(index)
        }
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Light Theme", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
fun FileListPreview() {
    val listState = FileExplorerViewModel.FileListState.Loaded(fakeExplorerData())
    FileListLayout(
        crumbList = fakeBreadCrumbData(),
        currentPath = "\"Some\"Path\"To\"Explore",
        isLoop = true,
        isShuffle = true,
        listState = listState,
        onBack = { },
        onClick = { _, _, _ -> },
        onCrumbLongClick = { _, _ -> },
        onGetDirectory = {},
        onLongClick = { _, _, _ -> },
        onLoop = {},
        onPlay = {},
        onShuffle = {}
    )
}
