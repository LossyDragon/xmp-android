package org.helllabs.android.xmp.compose.ui.explorer

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.BottomBarButtons
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.SingleChoiceListDialog
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.explorer.components.BreadCrumbs
import org.helllabs.android.xmp.compose.ui.explorer.components.ExplorerListCard
import org.helllabs.android.xmp.compose.ui.search.components.GuruFrame
import org.helllabs.android.xmp.compose.ui.search.components.GuruTextButton
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.DropDownSelection
import org.helllabs.android.xmp.model.FileItem
import org.koin.compose.koinInject
import timber.log.Timber

@Composable
fun ExplorerScreen(
    modifier: Modifier = Modifier,
    viewModel: ExplorerViewModel,
    snackBarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onPlayAll: (List<Uri>, Boolean, Boolean) -> Unit,
    onAddQueue: (List<Uri>, Boolean, Boolean) -> Unit,
    onPlayModule: (List<Uri>, Int, Boolean, Boolean, Boolean) -> Unit,
    onItemClick: (List<Uri>, Int, Boolean, Boolean) -> Unit
) {
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val storageManager = koinInject<StorageManager>()

    LaunchedEffect(state.crumbs.lastOrNull()?.path) {
        val currentPath = state.crumbs.lastOrNull()?.path?.toString()
        if (currentPath != null) {
            val savedPosition = viewModel.getScrollPosition(currentPath)
            if (savedPosition > 0) {
                viewModel.listState.animateScrollToItem(savedPosition)
            }
        }
    }

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

    BackHandler(enabled = true) {
        if (!viewModel.navigateBack()) {
            onBack()
        }
    }

    // Playlist choice dialog
    val choice by viewModel.playlistChoice.collectAsStateWithLifecycle()
    val playlists by viewModel.playlistList.collectAsStateWithLifecycle()
    SingleChoiceListDialog(
        isShowing = choice != null,
        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
        title = stringResource(id = R.string.dialog_title_select_playlist),
        selectedIndex = -1,
        textList = playlists.map { it.name }.toPersistentList(),
        onConfirm = viewModel::addToPlaylist,
        onDismiss = viewModel::clearPlaylist,
        onEmpty = {
            scope.launch {
                snackBarHostState.showSnackbar(
                    resources.getString(R.string.error_snack_no_playlists)
                )
                viewModel.clearPlaylist()
            }
        }
    )

    // Delete directory dialog
    val deleteDir by viewModel.deleteDirChoice.collectAsStateWithLifecycle()
    MessageDialog(
        isShowing = deleteDir != null,
        icon = Icons.Default.QuestionMark,
        title = "Delete directory",
        text = "Are you sure you want to delete ${viewModel.getDirName()}?",
        confirmText = stringResource(id = R.string.delete),
        onConfirm = {
            if (!viewModel.deleteDir()) {
                scope.launch {
                    snackBarHostState.showSnackbar(
                        message = "Error deleting directory",
                        actionLabel = "OK"
                    )
                }
            }
            viewModel.clearDeleteDir()
        },
        onDismiss = viewModel::clearDeleteDir
    )

    // Delete file dialog
    val deleteFile by viewModel.deleteFileChoice.collectAsStateWithLifecycle()
    MessageDialog(
        isShowing = deleteFile != null,
        icon = Icons.Default.QuestionMark,
        title = stringResource(id = R.string.delete_file_title),
        text = stringResource(
            id = R.string.delete_file_message,
            viewModel.getFileName()
        ),
        confirmText = stringResource(id = R.string.delete),
        onConfirm = {
            if (!viewModel.deleteFile()) {
                scope.launch {
                    snackBarHostState.showSnackbar(
                        message = "Error deleting file",
                        actionLabel = "OK"
                    )
                }
            }
            viewModel.onRefresh()
            viewModel.clearFileDir()
        },
        onDismiss = viewModel::clearFileDir
    )

    ExplorerScreenContent(
        modifier = modifier,
        state = state,
        listState = viewModel.listState,
        onBack = onBack,
        onRefresh = viewModel::onRefresh,
        onRestore = viewModel::onRestore,
        onShuffle = viewModel::onShuffle,
        onLoop = viewModel::onLoop,
        onPlayAll = {
            scope.launch {
                onPlayAll(
                    viewModel.onAllFiles(),
                    state.isShuffle,
                    state.isLoop,
                )
            }
        },
        onCrumbMenu = { selection ->
            when (selection) {
                DropDownSelection.ADD_TO_PLAYLIST -> viewModel.dropDownAddToPlaylist()

                DropDownSelection.ADD_TO_QUEUE -> onAddQueue(
                    viewModel.getItems(),
                    state.isShuffle,
                    state.isLoop,
                )

                DropDownSelection.DIR_PLAY_CONTENTS -> onPlayModule(
                    viewModel.getItems(),
                    0,
                    false,
                    state.isShuffle,
                    state.isLoop,
                )

                else -> Unit
            }
        },
        onCrumbClick = { crumb, _ ->
            viewModel.onNavigate(crumb.path)
        },
        onItemClick = { item, index ->
            if (item.isDirectory) {
                viewModel.onNavigate(item.uri)
            } else {
                val (dirCount, fileUris) = viewModel.getFileItems()
                onItemClick(
                    fileUris,
                    index - dirCount,
                    state.isShuffle,
                    state.isLoop,
                )
            }
        },
        onItemLongClick = { item, index, selection ->
            when (selection) {
                DropDownSelection.DELETE -> viewModel.dropDownDelete(item)

                DropDownSelection.ADD_TO_PLAYLIST -> viewModel.dropDownAddToPlaylist(item.uri)

                DropDownSelection.ADD_TO_QUEUE -> {
                    if (item.isDirectory) {
                        onPlayModule(
                            storageManager.walkDownDirectory(item.uri, includeDirectories = false),
                            0,
                            false,
                            state.isShuffle,
                            state.isLoop,
                        )
                    } else {
                        onAddQueue(
                            listOf(item.uri),
                            state.isShuffle,
                            state.isLoop,
                        )
                    }
                }

                DropDownSelection.DIR_PLAY_CONTENTS -> onPlayModule(
                    storageManager.walkDownDirectory(item.uri, includeDirectories = false),
                    0,
                    false,
                    state.isShuffle,
                    state.isLoop,
                )

                DropDownSelection.FILE_PLAY_HERE -> onPlayModule(
                    viewModel.getItems(),
                    index,
                    true,
                    state.isShuffle,
                    state.isLoop,
                )

                DropDownSelection.FILE_PLAY_THIS_ONLY -> onPlayModule(
                    listOf(item.uri),
                    0,
                    false,
                    state.isShuffle,
                    state.isLoop,
                )
            }
        }
    )
}

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
private fun ExplorerScreenContent(
    modifier: Modifier,
    state: ExplorerState,
    listState: LazyListState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRestore: () -> Unit,
    onShuffle: (Boolean) -> Unit,
    onLoop: (Boolean) -> Unit,
    onPlayAll: () -> Unit,
    onCrumbMenu: (DropDownSelection) -> Unit,
    onCrumbClick: (BreadCrumb, Int) -> Unit,
    onItemClick: (FileItem, Int) -> Unit,
    onItemLongClick: (FileItem, Int, DropDownSelection) -> Unit
) {
    val crumbScrollState = rememberLazyListState()

    // Auto-scroll breadcrumbs when navigation changes
    LaunchedEffect(state.crumbs) {
        if (state.crumbs.isNotEmpty()) {
            crumbScrollState.animateScrollToItem(state.crumbs.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                XmpTopBar(
                    title = stringResource(id = R.string.screen_title_filelist),
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
        }
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        val contentModifier = remember(configuration.orientation) {
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Modifier
            } else {
                Modifier.displayCutoutPadding()
            }
        }

        PullToRefreshBox(
            modifier = contentModifier.padding(paddingValues),
            contentAlignment = Alignment.Center,
            isRefreshing = state.isLoading,
            onRefresh = onRefresh,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(state.list) { index, item ->
                    ExplorerListCard(
                        item = item,
                        onItemClick = { onItemClick(item, index) },
                        onItemLongClick = { onItemLongClick(item, index, it) }
                    )
                }
            }

            if (state.list.isEmpty() && !state.isLoading) {
                GuruFrame(
                    message = "Directory is empty",
                    action = {
                        GuruTextButton(
                            text = stringResource(id = R.string.back),
                            onClick = onRestore
                        )
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview_ExplorerScreenContent() {
    XmpTheme(useDarkTheme = true) {
        ExplorerScreenContent(
            modifier = Modifier,
            state = ExplorerState(
                isLoading = false,
                list = List(10) {
                    FileItem(
                        name = "Name $it",
                        comment = "Comment $it",
                        uri = Uri.EMPTY
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
            listState = rememberLazyListState(),
            onBack = {},
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
