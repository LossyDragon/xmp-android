package org.helllabs.android.xmp.compose.ui.playlist.screen

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
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
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.KoinPreview
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.XmpCenterTopBar
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.compose.ui.playlist.components.MenuCardItem
import org.helllabs.android.xmp.compose.ui.playlist.viewmodel.PlaylistsUiState
import org.helllabs.android.xmp.compose.ui.playlist.viewmodel.PlaylistsViewModel
import org.helllabs.android.xmp.compose.ui.search.components.GuruFrame
import org.helllabs.android.xmp.compose.ui.search.components.GuruTextButton
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
    onNavPlaylist: (Uri) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
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

    val appSettings = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { viewModel.refreshAll() }
    )

    val documentTreeResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { viewModel.handleStorageDirectorySelected(it) }
    )

    // Prompt and Explain Storage
    MessageDialog(
        isShowing = state.askForStorage,
        title = "Storage Request",
        text = "Xmp needs access to its own directory via Storage Access Framework.\n" +
            "Create or reuse an existing directory for 'mods' and 'playlists'.",
        confirmText = "Create",
        onConfirm = {
            documentTreeResult.launch(null)
        },
        dismissText = stringResource(id = android.R.string.cancel),
        onDismiss = {
            viewModel.showStorageRequest(false)
            viewModel.clearError()
        }
    )

    LaunchedEffect(state.mediaPath) {
        if (state.mediaPath.isNotEmpty()) {
            viewModel.initializeDataDirectory(
                name = resources.getString(R.string.error_empty_playlist),
                comment = resources.getString(R.string.error_empty_comment),
            )
        }
    }

    // Check storage on initial composition and lifecycle resume
    LifecycleResumeEffect(Lifecycle.Event.ON_RESUME) {
        Timber.d("Lifecycle onResume")
        if (state.hasStorageAccess && state.mediaPath.isNotEmpty()) {
            viewModel.loadPlaylistItems()
        }
        onPauseOrDispose {
            Timber.d("Lifecycle onPause")
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
        onItemClick = { item ->
            onNavPlaylist(item.uri)
        },
        onItemLongClick = { item ->
            onEditPlaylist(item)
        },
        onRefresh = viewModel::loadPlaylistItems,
        onNewPlaylist = { onEditPlaylist(null) },
        onRequestSettings = {
            Intent().apply {
                action = ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts(
                    "package",
                    BuildConfig.APPLICATION_ID,
                    null
                )
                addCategory(Intent.CATEGORY_DEFAULT)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }.also(appSettings::launch)
        },
        onRequestStorage = { documentTreeResult.launch(null) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaylistsScreenContent(
    modifier: Modifier = Modifier,
    state: PlaylistsUiState,
    onSettings: () -> Unit,
    onTitle: () -> Unit,
    onItemClick: (item: FileItem) -> Unit,
    onItemLongClick: (item: FileItem) -> Unit,
    onNewPlaylist: () -> Unit,
    onRefresh: () -> Unit,
    onRequestSettings: () -> Unit,
    onRequestStorage: () -> Unit
) {
    val serviceAlive by PlayerService.isAlive.collectAsStateWithLifecycle()
    val servicePlaying by PlayerService.isPlaying.collectAsStateWithLifecycle()

    val scrollState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
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
            if (state.hasStorageAccess) {
                ExtendedFloatingActionButton(
                    text = { Text(text = stringResource(id = R.string.menu_new_playlist)) },
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    expanded = !isScrolled,
                    onClick = onNewPlaylist,
                    shape = MaterialTheme.shapes.extraLarge
                )
            }
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

        PullToRefreshBox(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
            isRefreshing = state.isLoading,
            onRefresh = onRefresh
        ) {
            if (state.playlistItems.isNotEmpty() && state.hasStorageAccess) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 12.dp,
                        bottom = 96.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    state = scrollState,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.playlistItems) { item ->
                        MenuCardItem(
                            item = item,
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) }
                        )
                    }
                }
            }

            if (!state.isLoading) {
                if (state.playlistItems.isEmpty() && state.hasStorageAccess) {
                    GuruFrame(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        message = "No playlists found"
                    )
                } else if (!state.hasStorageAccess) {
                    GuruFrame(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        message = "Unable to access playlists from storage",
                        action = {
                            GuruTextButton(text = "Set Directory", onClick = onRequestStorage)
                            GuruTextButton(text = "Go to Settings", onClick = onRequestSettings)
                        },
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
            2 -> "No Storage"
            3 -> "Empty Playlists"
            else -> "Unknown"
        }
    }

    override val values: Sequence<PlaylistsUiState>
        get() = sequenceOf(
            PlaylistsUiState(
                mediaPath = "sdcard\\some\\path",
                isLoading = false,
                hasStorageAccess = true,
                playlistItems = List(15) {
                    FileItem(
                        name = "Name $it",
                        comment = "Comment $it",
                        uri = Uri.EMPTY
                    )
                }.toPersistentList()
            ),
            PlaylistsUiState(
                mediaPath = "sdcard\\some\\path",
                isLoading = true,
                hasStorageAccess = true,
                playlistItems = List(15) {
                    FileItem(
                        name = "Name $it",
                        comment = "Comment $it",
                        uri = Uri.EMPTY
                    )
                }.toPersistentList()
            ),
            PlaylistsUiState(
                isLoading = false,
            ),
            PlaylistsUiState(
                mediaPath = "sdcard\\some\\path",
                isLoading = false,
                hasStorageAccess = true,
            )
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
            onRequestStorage = {},
            onRequestSettings = {}
        )
    }
}
