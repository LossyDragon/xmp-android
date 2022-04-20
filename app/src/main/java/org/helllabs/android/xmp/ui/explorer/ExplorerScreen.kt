package org.helllabs.android.xmp.ui.explorer

import android.content.res.Configuration
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.player.PlayerUtil
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.util.*

@Composable
fun ExplorerScreen(
    navController: NavController,
    onBackPressedCallback: OnBackPressedDispatcher,
    bindService: () -> Unit,
    viewModel: ExplorerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val assets = context.assets

    var isLoading by rememberSaveable { mutableStateOf(true) }

    val callback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFile = File(viewModel.state.value.currentFile)
                currentFile.parentFile?.let { file ->
                    if (file.path != "/") {
                        viewModel.onEvent(ExplorerEvent.DirectoryList(file))
                    } else {
                        this.isEnabled = false
                        navController.popBackStack()
                    }
                }
            }
        }
    }

    DisposableEffect(onBackPressedCallback) {
        onBackPressedCallback.addCallback(callback)
        onDispose {
            callback.remove()
        }
    }

    val fileNotFoundState = rememberMaterialDialogState()
    DialogMessage(
        dialogState = fileNotFoundState,
        title = R.string.dialog_no_path_title,
        messageText = stringResource(
            id = R.string.dialog_no_path_message,
            viewModel.state.value.currentFile
        ),
        positiveButtonText = R.string.create,
        negativeButtonText = R.string.cancel,
        onPositiveButton = {
            val ret = Files.installAssets(
                assets,
                viewModel.state.value.currentFile,
                PrefManager.installExamples
            )
            if (ret < 0) {
                context.toast(R.string.msg_error_create_directory)
            } else {
                val file = File(viewModel.state.value.currentFile)
                viewModel.onEvent(ExplorerEvent.DirectoryList(file))
            }
        },
        onNegativeButton = {},
        onDismiss = { fileNotFoundState.hide() }
    )

    LaunchedEffect(true) {
        delay(25) // Why do I need this?! Stops the loading bar from sticking...
        val file = File(PrefManager.mediaPath!!)
        viewModel.onEvent(ExplorerEvent.DirectoryList(file))

        viewModel.uiState.collectLatest { event ->
            when (event) {
                is ExplorerUiState.Error ->
                    context.toast(event.error ?: "An Error as occurred.")
                is ExplorerUiState.FileNotFound ->
                    fileNotFoundState.show()
                is ExplorerUiState.Loading -> {
                    isLoading = event.isLoading
                }
            }
        }
    }

    FileListLayout(
        onBack = { navController.popBackStack() },
        state = viewModel.state.value,
        crumbState = viewModel.crumbState.value,
        isLoading = isLoading,
        isLoopMode = viewModel.isLoopMode,
        isShuffleMode = viewModel.isShuffleMode,
        onLoopMode = { viewModel.isLoopMode = it },
        onShuffleMode = { viewModel.isShuffleMode = it },
        onPlay = {
            val file = File(viewModel.state.value.currentFile)
            val items = Files.recursiveList(file)
            if (items.isNullOrEmpty()) {
                context.toast(R.string.error_no_files_to_play)
            } else {
                // TODO: test this
                PlayerUtil.playModule(
                    context = context,
                    modList = items,
                    start = 0,
                    keepFirst = !viewModel.isShuffleMode,
                    isShuffle = viewModel.isShuffleMode,
                    isLoop = viewModel.isLoopMode
                )
            }
        },
        onItemClick = { index ->
            val list = viewModel.state.value.list
            val item = list[index]

            if (item.isDirectory()) {
                val event = ExplorerEvent.DirectoryList(item.file!!)
                viewModel.onEvent(event)
            } else {
                // TODO still not done, need to collect items in current dir fist
                //  then collect folders after.
                //  then start at the offset.
                PlayerUtil.playModule(
                    context = context,
                    modList = list.map { it.file!!.path }, // o.o?
                    isLoop = viewModel.isLoopMode,
                    isShuffle = viewModel.isShuffleMode,
                    keepFirst = true,
                )
            }
        },
        onItemLongClick = {
            // TODO start multi select
        },
        onMenuClick = { index ->
            val list = viewModel.state.value.list
            val item = list[index]

            if (item.file!!.isDirectory) {
                context.logD("onMenuClick isDirectory item: $item")
                context.toast("onMenuClick") // TODO
            } else {
                context.logD("onMenuClick item: $item")
                context.toast("onMenuClick") // TODO
            }
        },
        onCrumbClick = {
            val file = File(it)
            val event = ExplorerEvent.DirectoryList(file)
            viewModel.onEvent(event)
        },
        onCrumbLongClick = {
            context.logD("onCrumbLongClick $it")
            context.toast("onCrumbLongClick") // TODO
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileListLayout(
    onBack: () -> Unit,
    state: ExplorerState,
    crumbState: ExplorerCrumbState,
    isLoading: Boolean,
    isLoopMode: Boolean,
    isShuffleMode: Boolean,
    onPlay: () -> Unit,
    onLoopMode: (value: Boolean) -> Unit,
    onShuffleMode: (value: Boolean) -> Unit,
    onMenuClick: (index: Int) -> Unit,
    onItemClick: (index: Int) -> Unit,
    onItemLongClick: (index: Int) -> Unit,
    onCrumbClick: (path: String) -> Unit,
    onCrumbLongClick: (path: String) -> Unit,
) {
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    Scaffold(
        topBar = {
            Column {
                XmpAppBar3(
                    scrollBehavior = scrollBehavior,
                    onNavIconPressed = onBack,
                    titleText = stringResource(id = R.string.browser_filelist_title)
                )

                BreadCrumbLayout(
                    modifier = Modifier,
                    crumbState = crumbState,
                    scrollBehavior = scrollBehavior,
                    onCrumbClick = { onCrumbClick(it) },
                    onCrumbLongClick = { onCrumbLongClick(it) }
                )
            }
        },
        bottomBar = {
            LayoutControls(
                modifier = Modifier,
                onPlay = onPlay,
                onLoop = { onLoopMode(!isLoopMode) },
                onShuffle = { onShuffleMode(!isShuffleMode) },
                isLoopEnabled = isLoopMode,
                isShuffleEnabled = isShuffleMode,
            )
        },
    ) {
        LazyList(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = layoutControlsHeight)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            scrollModifier = Modifier,
            shouldPadBottom = false,
            boxContent = {
                if (state.list.isEmpty()) {
                    ErrorLayout(message = stringResource(id = R.string.msg_empty_directory))
                }

                ProgressbarIndicator(isLoading)
            },
            lazyContent = {
                itemsIndexed(items = state.list) { index, item ->
                    ItemList(
                        item = item,
                        showMenu = true,
                        onMenu = { onMenuClick(index) },
                        onClick = { onItemClick(index) },
                        onLongClick = { onItemLongClick(index) }
                    )
                }
            }
        )
    }
}

@Composable
private fun BreadCrumbLayout(
    modifier: Modifier,
    crumbState: ExplorerCrumbState,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onCrumbClick: (path: String) -> Unit,
    onCrumbLongClick: (path: String) -> Unit,
) {
    val backgroundColors = TopAppBarDefaults.centerAlignedTopAppBarColors()
    val backgroundColor = backgroundColors.containerColor(
        scrollFraction = scrollBehavior?.scrollFraction ?: 0f
    ).value

    val listState = rememberLazyListState()
    Column(modifier.background(backgroundColor)) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(crumbState.crumbList) { item ->
                ItemBreadCrumb(
                    crumb = item.name,
                    onClick = { onCrumbClick(item.path) },
                    onLongClick = { onCrumbLongClick(item.path) }
                )
            }
        }
        Divider(color = MaterialTheme.colorScheme.inverseSurface)
    }

    LaunchedEffect(crumbState.crumbList.size) {
        listState.animateScrollToItem(crumbState.crumbList.size)
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FileListPreview() {
    val explorerState = ExplorerState(
        list = fakeExplorerData(),
        currentFile = "\\Current\\File"
    )
    val crumbState = ExplorerCrumbState(crumbList = fakeBreadCrumbData())

    XmpTheme3 {
        FileListLayout(
            onBack = { },
            state = explorerState,
            crumbState = crumbState,
            isLoading = true,
            isLoopMode = true,
            isShuffleMode = true,
            onPlay = { },
            onLoopMode = {},
            onShuffleMode = {},
            onMenuClick = {},
            onItemClick = { },
            onItemLongClick = { },
            onCrumbClick = {},
            onCrumbLongClick = {}
        )
    }
}
