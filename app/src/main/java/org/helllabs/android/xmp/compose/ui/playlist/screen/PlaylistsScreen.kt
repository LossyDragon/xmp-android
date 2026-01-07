package org.helllabs.android.xmp.compose.ui.playlist.screen

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.KoinPreview
import org.helllabs.android.xmp.compose.components.XmpCenterTopBar
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.compose.ui.playlist.components.MenuCardItem
import org.helllabs.android.xmp.compose.ui.playlist.viewmodel.PlaylistsUiState
import org.helllabs.android.xmp.compose.ui.playlist.viewmodel.PlaylistsViewModel
import org.helllabs.android.xmp.compose.ui.search.components.GuruFrame
import org.helllabs.android.xmp.model.FileItem
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

@Composable
fun PlaylistsScreen(
    modifier: Modifier,
    viewModel: PlaylistsViewModel,
    snackBarHostState: SnackbarHostState,
    onSettings: () -> Unit,
    onEditPlaylist: (FileItem?) -> Unit,
    onNavPlaylist: (FileItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val snackMessage by viewModel.snackMessage.collectAsStateWithLifecycle()
    LaunchedEffect(snackMessage) {
        snackMessage?.let { message ->
            scope.launch {
                snackBarHostState.showSnackbar(
                    message = message,
                    actionLabel = "OK"
                )
            }
        }
    }

    val playerResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            1 -> result.data?.getStringExtra("error")?.let {
                Timber.w("Result with error: $it")
                scope.launch {
                    snackBarHostState.showSnackbar(message = it)
                }
            }

            else -> Timber.w("Unknown handled result code ${result.resultCode}")
        }
    }

    PlaylistsScreenContent(
        modifier = modifier,
        state = state,
        onSettings = onSettings,
        onTitle = {
            if (PlayerService.isAlive.value) {
                Intent(context, PlayerActivity::class.java).also(playerResult::launch)
            }
        },
        onItemClick = onNavPlaylist,
        onItemLongClick = onEditPlaylist,
        onRefresh = viewModel::refreshPlaylists,
        onNewPlaylist = { onEditPlaylist(null) },
    )
}

@Composable
private fun PlaylistsScreenContent(
    modifier: Modifier = Modifier,
    state: PlaylistsUiState,
    onSettings: () -> Unit,
    onTitle: () -> Unit,
    onItemClick: (item: FileItem) -> Unit,
    onItemLongClick: (item: FileItem) -> Unit,
    onNewPlaylist: () -> Unit,
    onRefresh: () -> Unit
) {
    val serviceAlive by PlayerService.isAlive.collectAsStateWithLifecycle()
    val servicePlaying by PlayerService.isPlaying.collectAsStateWithLifecycle()

    val scrollState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }
    val isLastItemVisible by remember {
        derivedStateOf {
            val lastVisibleItemIndex = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val totalItemsCount = scrollState.layoutInfo.totalItemsCount
            lastVisibleItemIndex != null &&
                lastVisibleItemIndex >= totalItemsCount - 1 &&
                totalItemsCount > 0
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            XmpCenterTopBar(
                onSettings = onSettings,
                onTitle = onTitle,
                isAlive = serviceAlive,
                isPlaying = servicePlaying,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(text = stringResource(id = R.string.menu_new_playlist)) },
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                expanded = !isScrolled,
                onClick = onNewPlaylist,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = isLastItemVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                content = {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp, bottom = 80.dp, start = 16.dp, end = 16.dp),
                        text = "Playlists location:\n${state.playlistLocation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primaryFixedDim.copy(alpha = .25f)
                    )
                }
            )

            PullToRefreshBox(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                isRefreshing = state.isLoading,
                onRefresh = onRefresh
            ) {
                if (state.playlists.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 12.dp,
                            bottom = 136.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.playlists) { item ->
                            MenuCardItem(
                                item = item,
                                onClick = { onItemClick(item) },
                                onLongClick = { onItemLongClick(item) }
                            )
                        }
                    }
                }

                if (!state.isLoading && state.playlists.isEmpty()) {
                    GuruFrame(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        message = "No playlists found"
                    )
                }
            }
        }
    }
}

private class PlaylistPreview : PreviewParameterProvider<PlaylistsUiState> {
    override fun getDisplayName(index: Int): String? {
        return when (index) {
            0 -> "Loaded with Playlists"
            1 -> "Loading"
            3 -> "Empty Playlists"
            else -> "Unknown"
        }
    }

    override val values: Sequence<PlaylistsUiState>
        get() = sequenceOf(
            PlaylistsUiState(
                isLoading = false,
                playlistLocation = "file://Android/data/org.helllabs.android.xmp/files/playlists",
                playlists = List(15) {
                    FileItem(
                        name = "Name $it",
                        comment = "Comment $it",
                        uri = Uri.EMPTY
                    )
                }.toPersistentList()
            ),
            PlaylistsUiState(
                isLoading = true,
                playlistLocation = "file://Android/data/org.helllabs.android.xmp/files/playlists",
                playlists = List(1) {
                    FileItem(
                        name = "Name $it",
                        comment = "Comment $it",
                        uri = Uri.EMPTY
                    )
                }.toPersistentList()
            ),
            PlaylistsUiState(
                isLoading = false,
                playlistLocation = "file://Android/data/org.helllabs.android.xmp/files/playlists",
            ),
        )
}

@Preview
@Composable
private fun Preview(@PreviewParameter(PlaylistPreview::class) state: PlaylistsUiState) {
    KoinPreview {
        PlaylistsScreenContent(
            state = state,
            onSettings = {},
            onTitle = {},
            onItemClick = {},
            onItemLongClick = {},
            onRefresh = {},
            onNewPlaylist = {},
        )
    }
}
