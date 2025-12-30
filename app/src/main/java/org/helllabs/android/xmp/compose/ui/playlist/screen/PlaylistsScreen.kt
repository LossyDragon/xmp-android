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
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.XmpCenterTopBar
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.compose.ui.playlist.components.MenuCardItem
import org.helllabs.android.xmp.compose.ui.playlist.viewmodel.PlaylistsUiState
import org.helllabs.android.xmp.compose.ui.playlist.viewmodel.PlaylistsViewModel
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.di.appModule
import org.helllabs.android.xmp.di.playlistModule
import org.helllabs.android.xmp.model.FileItem
import org.helllabs.android.xmp.service.PlayerService
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration
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

    val prefManager = koinInject<PrefManager>()
    val storageManager = koinInject<StorageManager>()

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
        if (result.resultCode == 1) {
            result.data?.getStringExtra("error")?.let {
                Timber.w("Result with error: $it")
                scope.launch {
                    snackBarHostState.showSnackbar(message = it)
                }
            }
        }
        if (result.resultCode == 2) {
            Timber.d("Result with 2")
        }
    }

    val appSettings = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.updateList()
    }

    val documentTreeResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        scope.launch {
            storageManager.setPlaylistDirectory(uri).onSuccess {
                viewModel.setDefaultPath()
            }.onFailure {
                viewModel.emitError(message = it.message ?: resources.getString(R.string.error))
            }
        }
    }

    // Ask for Permissions
    LaunchedEffect(Unit) {
        val savedUri = prefManager.getSafStoragePath().toUri()
        val persistedUris = context.contentResolver.persistedUriPermissions
        val hasAccess = persistedUris.any {
            it.uri == savedUri && it.isWritePermission
        }

        if (!hasAccess) {
            viewModel.askForStorage(true)
        } else {
            viewModel.setDefaultPath()
        }
    }

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
            viewModel.askForStorage(false)
            viewModel.clearError()
            viewModel.updateList()
        }
    )

    LaunchedEffect(state.mediaPath) {
        if (state.mediaPath.isNotEmpty()) {
            viewModel.setupDataDir(
                name = resources.getString(R.string.error_empty_playlist),
                comment = resources.getString(R.string.error_empty_comment),
            ).onSuccess {
                viewModel.updateList()
            }.onFailure {
                viewModel.emitError(it.message ?: resources.getString(R.string.error))
            }
        }
    }

    LifecycleResumeEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch {
            Timber.d("Lifecycle onResume")
            if (prefManager.getSafStoragePath().isNotEmpty()) {
                viewModel.updateList()
            }
        }
        onPauseOrDispose {
            Timber.d("Lifecycle onPause")
        }
    }

    HomeScreenContent(
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
        onRefresh = viewModel::updateList,
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
private fun HomeScreenContent(
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

    val storageManager = koinInject<StorageManager>()

    val scrollState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }

    val view = LocalView.current
    val hasStorage by produceState(initialValue = false, state) {
        value = if (view.isInEditMode) {
            true
        } else {
            storageManager.checkPermissions()
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
            if (hasStorage) {
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
            if (state.playlistItems.isNotEmpty()) {
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

            if (!state.isLoading && !hasStorage) {
                ErrorScreen(text = "Unable to access files for playlist or file browser") {
                    Button(
                        onClick = onRequestStorage,
                        shape = MaterialTheme.shapes.extraLarge,
                        content = { Text(text = "Set Directory") }
                    )
                    OutlinedButton(
                        onClick = onRequestSettings,
                        shape = MaterialTheme.shapes.extraLarge,
                        content = { Text(text = "Goto Settings") }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview_PlaylistMenuScreen() {
    KoinApplication(
        configuration = koinConfiguration { modules(listOf(appModule, playlistModule)) }
    ) {
        XmpTheme(useDarkTheme = true) {
            HomeScreenContent(
                state = PlaylistsUiState(
                    mediaPath = "sdcard\\some\\path",
                    isLoading = true,
                    playlistItems = List(15) {
                        FileItem(
                            name = "Name $it",
                            comment = "Comment $it",
                            uri = Uri.EMPTY
                        )
                    }.toPersistentList()
                ),
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
}
