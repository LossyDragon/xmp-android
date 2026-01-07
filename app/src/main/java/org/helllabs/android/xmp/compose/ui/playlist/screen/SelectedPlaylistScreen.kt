package org.helllabs.android.xmp.compose.ui.playlist.screen

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toPersistentList
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.BottomBarButtons
import org.helllabs.android.xmp.compose.components.KoinPreview
import org.helllabs.android.xmp.compose.ui.playlist.components.PlaylistCardItem
import org.helllabs.android.xmp.compose.ui.playlist.components.PlaylistInfo
import org.helllabs.android.xmp.compose.ui.playlist.viewmodel.SelectedPlaylistViewModel
import org.helllabs.android.xmp.compose.ui.search.components.GuruFrame
import org.helllabs.android.xmp.compose.ui.search.components.GuruTextButton
import org.helllabs.android.xmp.di.appModule
import org.helllabs.android.xmp.model.DropDownSelection
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.rememberScroller
import timber.log.Timber

@Composable
fun SelectedPlaylistScreen(
    modifier: Modifier,
    viewModel: SelectedPlaylistViewModel,
    snackBarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onPlayAll: (List<Uri>, Boolean, Boolean) -> Unit,
    onAddQueue: (List<Uri>, Boolean, Boolean) -> Unit,
    onPlayModule: (List<Uri>, Int, Boolean, Boolean, Boolean) -> Unit,
    onItemClick: (List<Uri>, Int, Boolean, Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Lifecycle.Event.ON_RESUME) {
        Timber.d("Lifecycle onResume")
        viewModel.onRefresh()
        viewModel.useFileName()

        onPauseOrDispose {
            Timber.d("Lifecycle onPause")
            viewModel.save()
        }
    }

    PlaylistScreenContent(
        modifier = modifier,
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
                    viewModel.onRefresh()
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
private fun PlaylistScreenContent(
    modifier: Modifier,
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
        modifier = modifier,
        topBar = {
            PlaylistInfo(
                isScrolled = isScrolled.value,
                onBack = onBack,
                playlistName = state.name,
                playlistComment = state.comment
            )
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

            ReorderableColumn(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                list = state.list,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                onSettle = { fromIndex, toIndex ->
                    onMove(fromIndex, toIndex)
                },
                onMove = {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                },
                content = { index, item, isDragging ->
                    key(item.id) {
                        ReorderableItem {
                            val interactionSource = remember { MutableInteractionSource() }

                            PlaylistCardItem(
                                iconModifier = Modifier.draggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(
                                            HapticFeedbackType.GestureThresholdActivate
                                        )
                                    },
                                    onDragStopped = {
                                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                        onDragStopped()
                                    }
                                ),
                                interactionSource = interactionSource,
                                item = item,
                                isDragging = isDragging,
                                useFileName = state.useFileName,
                                onItemClick = { onItemClick(index) },
                                onMenuClick = { onMenuClick(item, index, it) },
                            )
                        }
                    }
                }
            )

            if (state.list.isEmpty()) {
                GuruFrame(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    message = stringResource(id = R.string.error_empty_playlist),
                    action = { GuruTextButton(text = "Go Back", onClick = onBack) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview_PlaylistScreenContent() {
    KoinPreview(modules = listOf(appModule)) {
        PlaylistScreenContent(
            modifier = Modifier,
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
