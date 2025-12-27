package org.helllabs.android.xmp.compose.ui.filelist

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toPersistentList
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.BottomBarButtons
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.SingleChoiceListDialog
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.filelist.components.BreadCrumbs
import org.helllabs.android.xmp.compose.ui.filelist.components.FileListCard
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.DropDownSelection
import org.helllabs.android.xmp.model.FileItem
import timber.log.Timber

@Serializable
object NavFileList

@Composable
fun FileListScreenImpl(
    viewModel: FileListViewModel,
    snackBarHostState: SnackbarHostState,
    onBackPressedDispatcher: OnBackPressedDispatcher,
    onBack: () -> Unit,
    onPlayAll: (List<Uri>, Boolean, Boolean) -> Unit,
    onAddQueue: (List<Uri>, Boolean, Boolean) -> Unit,
    onPlayModule: (List<Uri>, Int, Boolean, Boolean, Boolean) -> Unit,
    onItemClick: (List<Uri>, Int, Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Lifecycle.Event.ON_RESUME) {
        Timber.d("Lifecycle onResume")
        viewModel.onRefresh()

        onPauseOrDispose {
            Timber.d("Lifecycle onPause")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.softError.collectLatest {
            snackBarHostState.showSnackbar(it)
        }
    }

    // On back pressed handler
    val callback = remember {
        object : OnBackPressedCallback(true) {
            private fun goBack() {
                this.remove()
                onBack()
            }

            override fun handleOnBackPressed() {
                if (PrefManager.backButtonNavigation) {
                    if (!viewModel.onBackPressed()) {
                        goBack()
                    }
                } else {
                    goBack()
                }
            }
        }
    }

    // Set up and override on back pressed.
    DisposableEffect(onBackPressedDispatcher) {
        onBackPressedDispatcher.addCallback(callback)
        onDispose {
            callback.remove()
        }
    }

    /**
     * Playlist choice dialog
     */
    val choice by viewModel.playlistChoice.collectAsStateWithLifecycle()
    val playlists by viewModel.playlistList.collectAsStateWithLifecycle()
    SingleChoiceListDialog(
        isShowing = choice != null,
        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
        title = stringResource(id = R.string.dialog_title_select_playlist),
        selectedIndex = -1,
        list = playlists.map { it.name }.toPersistentList(),
        onConfirm = viewModel::addToPlaylist,
        onDismiss = {
            viewModel.clearPlaylist()
        },
        onEmpty = {
            scope.launch {
                snackBarHostState.showSnackbar(
                    message = context.getString(R.string.error_snack_no_playlists)
                )
                viewModel.clearPlaylist()
            }
        }
    )

    /**
     * Delete directory dialog
     */
    val deleteDir by viewModel.deleteDirChoice.collectAsStateWithLifecycle()
    MessageDialog(
        isShowing = deleteDir != null,
        icon = Icons.Default.QuestionMark,
        title = "Delete directory",
        text = "Are you sure you want to delete ${viewModel.getDirName()} and all its contents?",
        confirmText = stringResource(id = R.string.delete),
        onConfirm = {
            val res = viewModel.deleteDir()
            if (!res) {
                scope.launch {
                    snackBarHostState.showSnackbar("Unable to delete directory")
                }
            }
            viewModel.clearDeleteDir()
        },
        onDismiss = {
            viewModel.clearDeleteDir()
        }
    )

    /**
     * Delete file dialog
     */
    val deleteFile by viewModel.deleteFileChoice.collectAsStateWithLifecycle()
    MessageDialog(
        isShowing = deleteFile != null,
        icon = Icons.Default.QuestionMark,
        title = "Delete File",
        text = "Are you sure you want to delete ${viewModel.getFileName()}?",
        confirmText = stringResource(id = R.string.delete),
        onConfirm = {
            val res = viewModel.deleteFile()
            if (!res) {
                scope.launch {
                    snackBarHostState.showSnackbar("Unable to delete item")
                }
            }
            viewModel.onRefresh()
            viewModel.clearFileDir()
        },
        onDismiss = {
            viewModel.clearFileDir()
        }
    )

    FileListScreen(
        state = state,
        snackBarHostState = snackBarHostState,
        onBack = {
            callback.remove()
            onBackPressedDispatcher.onBackPressed()
        },
        onScrollPosition = viewModel::setScrollPosition,
        onRefresh = viewModel::onRefresh,
        onRestore = viewModel::onRestore,
        onShuffle = viewModel::onShuffle,
        onLoop = viewModel::onLoop,
        onPlayAll = {
            scope.launch {
                onPlayAll(
                    viewModel.onAllFiles(),
                    viewModel.uiState.value.isShuffle,
                    viewModel.uiState.value.isLoop,
                )
            }
        },
        onCrumbMenu = { selection ->
            when (selection) {
                DropDownSelection.ADD_TO_PLAYLIST -> {
                    viewModel.dropDownAddToPlaylist()
                }

                DropDownSelection.ADD_TO_QUEUE -> onAddQueue(
                    viewModel.getItems(),
                    viewModel.uiState.value.isShuffle,
                    viewModel.uiState.value.isLoop,
                )

                DropDownSelection.DIR_PLAY_CONTENTS -> onPlayModule(
                    viewModel.getItems(),
                    0,
                    false,
                    viewModel.uiState.value.isShuffle,
                    viewModel.uiState.value.isLoop,
                )

                else -> Unit
            }
        },
        onCrumbClick = { crumb, _ ->
            viewModel.onNavigate(crumb.path)
        },
        onItemClick = { item, index ->
            if (item.docFile!!.isFile()) {
                val items = viewModel.getFileItems()
                onItemClick(
                    items.second,
                    index - items.first,
                    viewModel.uiState.value.isShuffle,
                    viewModel.uiState.value.isLoop,
                )
            } else {
                viewModel.onNavigate(item.docFile)
            }
        },
        onItemLongClick = { item, index, sel ->
            when (sel) {
                DropDownSelection.DELETE -> {
                    viewModel.dropDownDelete(item)
                }

                DropDownSelection.ADD_TO_PLAYLIST -> {
                    viewModel.dropDownAddToPlaylist(item.docFile)
                }

                DropDownSelection.ADD_TO_QUEUE -> {
                    if (item.isFile) {
                        onAddQueue(
                            item.docFile?.uri?.let { listOf(it) }.orEmpty(),
                            viewModel.uiState.value.isShuffle,
                            viewModel.uiState.value.isLoop,
                        )
                    } else {
                        onPlayModule(
                            StorageManager.walkDownDirectory(
                                uri = item.docFile?.uri,
                                includeDirectories = false
                            ),
                            0,
                            false,
                            viewModel.uiState.value.isShuffle,
                            viewModel.uiState.value.isLoop,
                        )
                    }
                }

                DropDownSelection.DIR_PLAY_CONTENTS -> onPlayModule(
                    StorageManager.walkDownDirectory(
                        uri = item.docFile?.uri,
                        includeDirectories = false
                    ),
                    0,
                    false,
                    viewModel.uiState.value.isShuffle,
                    viewModel.uiState.value.isLoop,
                )

                DropDownSelection.FILE_PLAY_HERE -> onPlayModule(
                    viewModel.getItems(),
                    index,
                    true,
                    viewModel.uiState.value.isShuffle,
                    viewModel.uiState.value.isLoop,
                )

                DropDownSelection.FILE_PLAY_THIS_ONLY -> onPlayModule(
                    item.docFile?.uri?.let { listOf(it) }.orEmpty(),
                    0,
                    false,
                    viewModel.uiState.value.isShuffle,
                    viewModel.uiState.value.isLoop,
                )
            }
        }
    )
}

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
private fun FileListScreen(
    state: FileListState,
    snackBarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onScrollPosition: (Int) -> Unit,
    onRefresh: () -> Unit,
    onRestore: () -> Unit,
    onShuffle: (value: Boolean) -> Unit,
    onLoop: (value: Boolean) -> Unit,
    onPlayAll: () -> Unit,
    onCrumbMenu: (DropDownSelection) -> Unit,
    onCrumbClick: (crumb: BreadCrumb, index: Int) -> Unit,
    onItemClick: (item: FileItem, index: Int) -> Unit,
    onItemLongClick: (item: FileItem, index: Int, sel: DropDownSelection) -> Unit
) {
    val scrollState = rememberLazyListState(initialFirstVisibleItemIndex = state.lastScrollPosition)
    val crumbScrollState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }

    // Save our scroll position
    LaunchedEffect(scrollState) {
        snapshotFlow {
            scrollState.firstVisibleItemIndex
        }.debounce(1.seconds).collectLatest {
            onScrollPosition(it)
        }
    }

    // Refresh our list's scroll position if our crumbs changes.
    LaunchedEffect(state.crumbs) {
        if (state.crumbs.isEmpty()) return@LaunchedEffect

        scrollState.animateScrollToItem(state.lastScrollPosition)
        crumbScrollState.animateScrollToItem(state.crumbs.lastIndex)
    }

    Scaffold(
        topBar = {
            Column {
                XmpTopBar(
                    title = stringResource(id = R.string.screen_title_filelist),
                    isScrolled = isScrolled,
                    onBack = onBack
                )
                BreadCrumbs(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    crumbScrollState = crumbScrollState,
                    crumbs = state.crumbs,
                    onCrumbMenu = onCrumbMenu,
                    onCrumbClick = onCrumbClick
                )
            }
        },
        bottomBar = {
            BottomBarButtons(
                isShuffle = state.isShuffle,
                isLoop = state.isLoop,
                onShuffle = onShuffle,
                onLoop = onLoop,
                onPlayAll = onPlayAll
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        val modifier = remember(configuration.orientation) {
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Modifier
            } else {
                Modifier.displayCutoutPadding()
            }
        }

        PullToRefreshBox(
            modifier = modifier.padding(paddingValues),
            contentAlignment = Alignment.Center,
            isRefreshing = state.isLoading, // TODO Won't hide when false
            onRefresh = onRefresh,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = scrollState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(state.list) { index, item ->
                    FileListCard(
                        item = item,
                        onItemClick = { onItemClick(item, index) },
                        onItemLongClick = { onItemLongClick(item, index, it) }
                    )
                }
            }

            if (state.list.isEmpty() && !state.isLoading) {
                ErrorScreen(
                    text = stringResource(id = R.string.error_empty_directory),
                    action = {
                        OutlinedButton(onClick = onRestore) {
                            Text(text = stringResource(id = R.string.back))
                        }
                    }
                )
            }

            ProgressbarIndicator(isLoading = state.isLoading)
        }
    }
}

@Preview
@Composable
private fun Preview_FileListScreen() {
    XmpTheme(useDarkTheme = true) {
        FileListScreen(
            state = FileListState(
                isLoading = true,
                list = List(10) {
                    FileItem(
                        name = "Name $it",
                        comment = "Comment $it",
                        docFile = null
                    )
                },
                crumbs = List(4) {
                    BreadCrumb(
                        name = "Crumb $it",
                        path = null
                    )
                }.toPersistentList(),
                isLoop = true,
                isShuffle = false
            ),
            snackBarHostState = SnackbarHostState(),
            onBack = {},
            onScrollPosition = {},
            onRefresh = {},
            onRestore = {},
            onLoop = {},
            onShuffle = {},
            onPlayAll = {},
            onCrumbClick = { _, _ -> },
            onCrumbMenu = {},
            onItemClick = { _, _ -> },
            onItemLongClick = { _, _, _ -> },
        )
    }
}
