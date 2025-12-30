package org.helllabs.android.xmp.compose.ui.home

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.EditPlaylistDialog
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.NewPlaylistDialog
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.home.components.MenuCardItem
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.di.appModule
import org.helllabs.android.xmp.di.playlistModule
import org.helllabs.android.xmp.model.FileItem
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration
import timber.log.Timber

@Composable
fun HomeScreen(
    modifier: Modifier,
    viewModel: PlaylistMenuViewModel,
    snackBarHostState: SnackbarHostState,
    onEditPlaylist: (FileItem?) -> Unit,
    onNavPlaylist: (Uri) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val prefManager = koinInject<PrefManager>()
    val storageManager = koinInject<StorageManager>()
    val playlistManager = koinInject<PlaylistManager>()

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
                viewModel.showError(message = it.message ?: resources.getString(R.string.error))
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
            viewModel.showError(null)
            viewModel.updateList()
        }
    )

    // Error message Dialog
    MenuErrorDialog(
        state = state,
        onConfirm = {
            viewModel.showError(null)
            viewModel.updateList()
        }
    )

    // Edit playlist dialog
//    MenuEditDialog(
//        state = state,
//        onConfirm = { res ->
//            if (!res) {
//                scope.launch {
//                    val msg = "Failed to edit playlist"
//                    snackBarHostState.showSnackbar(msg, "OK")
//                }
//            }
//
//            viewModel.editPlaylist(null)
//        },
//        onDelete = { item ->
//            scope.launch {
//                playlistManager.delete(item.name)
//                viewModel.editPlaylist(null)
//            }
//        },
//        onDismiss = { viewModel.editPlaylist(null) },
//    )

    // New playlist dialog
//    MenuNewPlaylist(
//        state = state,
//        onConfirm = { res ->
//            if (res) {
//                viewModel.updateList()
//            } else {
//                viewModel.showError(
//                    message = resources.getString(R.string.dialog_message_error_create_playlist)
//                )
//            }
//
//            viewModel.newPlaylist(false)
//        },
//        onDismiss = { viewModel.newPlaylist(false) }
//    )

    LaunchedEffect(state.mediaPath) {
        if (state.mediaPath.isNotEmpty()) {
            viewModel.setupDataDir(
                name = resources.getString(R.string.error_empty_playlist),
                comment = resources.getString(R.string.error_empty_comment),
            ).onSuccess {
                viewModel.updateList()
            }.onFailure {
                viewModel.showError(it.message ?: resources.getString(R.string.error))
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
    state: PlaylistMenuState,
    onItemClick: (item: FileItem) -> Unit,
    onItemLongClick: (item: FileItem) -> Unit,
    onNewPlaylist: () -> Unit,
    onRefresh: () -> Unit,
    onRequestSettings: () -> Unit,
    onRequestStorage: () -> Unit
) {
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

@Composable
private fun MenuErrorDialog(
    state: PlaylistMenuState,
    onConfirm: () -> Unit
) {
    MessageDialog(
        isShowing = state.errorText != null,
        title = stringResource(id = R.string.error),
        text = state.errorText.orEmpty(),
        confirmText = stringResource(id = android.R.string.ok),
        onConfirm = onConfirm
    )
}

// @Composable
// private fun MenuEditDialog(
//    state: PlaylistMenuState,
//    onConfirm: (Boolean) -> Unit,
//    onDismiss: () -> Unit,
//    onDelete: (FileItem) -> Unit
// ) {
//    val playlistManager = koinInject<PlaylistManager>()
//    val scope = rememberCoroutineScope()
//
//    EditPlaylistDialog(
//        isShowing = state.editPlaylist != null,
//        fileItem = state.editPlaylist,
//        onConfirm = { item, newName, newComment ->
//            scope.launch {
//                val res = playlistManager.run {
//                    load(item.docFile!!.uri)
//                    rename(newName, newComment)
//                }.isSuccess
//
//                onConfirm(res)
//            }
//        },
//        onDismiss = onDismiss,
//        onDelete = onDelete
//    )
// }
//
// @Composable
// private fun MenuNewPlaylist(
//    state: PlaylistMenuState,
//    onConfirm: (Boolean) -> Unit,
//    onDismiss: () -> Unit
// ) {
//    val playlistManager = koinInject<PlaylistManager>()
//    val scope = rememberCoroutineScope()
//
//    NewPlaylistDialog(
//        isShowing = state.newPlaylist,
//        onConfirm = { name, comment ->
//            scope.launch {
//                val res = playlistManager.new(name, comment).isSuccess
//                onConfirm(res)
//            }
//        },
//        onDismiss = onDismiss
//    )
// }

@Preview
@Composable
private fun Preview_PlaylistMenuScreen() {
    KoinApplication(
        configuration = koinConfiguration { modules(listOf(appModule, playlistModule)) }
    ) {
        XmpTheme(useDarkTheme = true) {
            HomeScreenContent(
                state = PlaylistMenuState(
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
