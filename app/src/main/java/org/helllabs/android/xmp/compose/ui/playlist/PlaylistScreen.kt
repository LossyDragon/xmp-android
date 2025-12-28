package org.helllabs.android.xmp.compose.ui.playlist

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.BottomBarButtons
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.XmpTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.playlist.components.PlaylistCardItem
import org.helllabs.android.xmp.compose.ui.playlist.components.PlaylistInfo
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.model.DropDownSelection
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.rememberScroller
import timber.log.Timber

@Composable
fun PlaylistScreenImpl(
    viewModel: PlaylistViewModel,
    snackBarHostState: SnackbarHostState,
    playlist: String,
    onBack: () -> Unit,
    onPlayAll: (List<Uri>, Boolean, Boolean) -> Unit,
    onAddQueue: (List<Uri>, Boolean, Boolean) -> Unit,
    onPlayModule: (List<Uri>, Int, Boolean, Boolean, Boolean) -> Unit,
    onItemClick: (List<Uri>, Int, Boolean, Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefManager = koinInject<PrefManager>()

    LifecycleResumeEffect(Lifecycle.Event.ON_RESUME) {
        Timber.d("Lifecycle onResume")
        viewModel.onRefresh(playlist)
        viewModel.useFileName()

        onPauseOrDispose {
            Timber.d("Lifecycle onPause")
            viewModel.save()
        }
    }

    PlaylistScreen(
        state = state,
        snackBarHostState = snackBarHostState,
        onBack = onBack,
        onItemClick = { index ->
            onItemClick(
                viewModel.getUriItems(),
                index,
                viewModel.uiState.value.isShuffle,
                viewModel.uiState.value.isLoop,
            )
        },
        onMenuClick = { item, index, selection ->
            when (selection) {
                DropDownSelection.DELETE -> {
                    viewModel.removeItem(index)
                    viewModel.onRefresh(playlist)
                }

                DropDownSelection.ADD_TO_QUEUE ->
                    onAddQueue(
                        listOf(item.uri),
                        viewModel.uiState.value.isShuffle,
                        viewModel.uiState.value.isLoop,
                    )

                DropDownSelection.FILE_PLAY_HERE ->
                    onPlayModule(
                        viewModel.getUriItems(),
                        index,
                        false,
                        viewModel.uiState.value.isShuffle,
                        viewModel.uiState.value.isLoop,
                    )

                DropDownSelection.FILE_PLAY_THIS_ONLY ->
                    onPlayModule(
                        listOf(item.uri),
                        0,
                        false,
                        viewModel.uiState.value.isShuffle,
                        viewModel.uiState.value.isLoop,
                    )

                else -> Unit
            }
        },
        onPlayAll = {
            onPlayAll(
                viewModel.getUriItems(),
                viewModel.uiState.value.isShuffle,
                viewModel.uiState.value.isLoop,
            )
        },
        onShuffle = viewModel::setShuffle,
        onLoop = viewModel::setLoop,
        onMove = viewModel::onMove,
        onDragStopped = viewModel::onDragStopped
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistScreen(
    state: Playlist,
    snackBarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onItemClick: (index: Int) -> Unit,
    onMenuClick: (item: PlaylistItem, index: Int, sel: DropDownSelection) -> Unit,
    onShuffle: (value: Boolean) -> Unit,
    onLoop: (value: Boolean) -> Unit,
    onPlayAll: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onDragStopped: () -> Unit
) {
    val listState = rememberLazyListState()
    val isScrolled = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        topBar = {
            Column {
                XmpTopBar(
                    title = stringResource(id = R.string.screen_title_playlist),
                    isScrolled = isScrolled.value,
                    onBack = onBack
                )
                PlaylistInfo(
                    isScrolled = isScrolled.value,
                    playlistName = state.name,
                    playlistComment = state.comment
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

        Box(
            modifier = modifier.padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            val pixelAmount by remember {
                derivedStateOf { listState.layoutInfo.viewportSize.height * 0.05f }
            }
            val haptic = LocalHapticFeedback.current
            val reorderState =
                rememberReorderableLazyListState(
                    lazyListState = listState,
                    scroller = rememberScroller(
                        scrollableState = listState,
                        pixelAmount = pixelAmount,
                    )
                ) { from, to ->
                    onMove(from.index - 1, to.index - 1)
                    haptic.performHapticFeedback(HapticFeedbackType(26))
                }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dummy item. Compose (and libs) cannot handle 1st item animations
                // https://github.com/Calvin-LL/Reorderable/issues/4
                item {
                    ReorderableItem(
                        state = reorderState,
                        key = "dummy",
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(.01.dp)
                    ) {}
                }
                itemsIndexed(state.list, key = { _, item -> item.id }) { index, item ->
                    ReorderableItem(
                        state = reorderState,
                        key = item.id
                    ) { isDragging ->
                        val elevation by animateDpAsState(
                            if (isDragging) 4.dp else 0.dp,
                            label = ""
                        )
                        PlaylistCardItem(
                            scope = this,
                            elevation = elevation,
                            item = item,
                            useFileName = state.useFileName,
                            onItemClick = { onItemClick(index) },
                            onMenuClick = { onMenuClick(item, index, it) },
                            onDragStopped = onDragStopped
                        )
                    }
                }
            }

            if (state.list.isEmpty()) {
                ErrorScreen(
                    text = stringResource(id = R.string.error_empty_playlist),
                    action = {
                        OutlinedButton(onClick = onBack) {
                            Text(text = stringResource(id = R.string.back))
                        }
                    }
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_PlaylistScreen() {
    XmpTheme {
        PlaylistScreen(
            state = Playlist(
                comment = stringResource(id = R.string.error_empty_comment),
                isLoop = true,
                isShuffle = false,
                name = stringResource(id = R.string.error_empty_playlist),
                list = List(15) {
                    PlaylistItem(
                        name = "Name $it",
                        type = "Comment $it",
                        uri = Uri.EMPTY,
                        id = it
                    )
                }.toPersistentList()
            ),
            snackBarHostState = SnackbarHostState(),
            onItemClick = { _ -> },
            onMenuClick = { _, _, _ -> },
            onBack = {},
            onShuffle = {},
            onLoop = {},
            onPlayAll = {},
            onMove = { _, _ -> },
            onDragStopped = { }
        )
    }
}
