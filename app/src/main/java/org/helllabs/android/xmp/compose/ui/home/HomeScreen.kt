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
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.ktx.darken
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.saket.extendedspans.ExtendedSpans
import me.saket.extendedspans.SquigglyUnderlineSpanPainter
import me.saket.extendedspans.drawBehind
import me.saket.extendedspans.rememberSquigglyUnderlineAnimator
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.EditPlaylistDialog
import org.helllabs.android.xmp.compose.components.ErrorScreen
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.NewPlaylistDialog
import org.helllabs.android.xmp.compose.components.ProgressbarIndicator
import org.helllabs.android.xmp.compose.components.themedText
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.theme.michromaFontFamily
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.FileItem
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

@Serializable
object NavigationHome

@Composable
fun HomeScreenImpl(
    viewModel: PlaylistMenuViewModel,
    snackBarHostState: SnackbarHostState,
    onNavFileList: () -> Unit,
    onNavPlaylist: (String) -> Unit,
    onNavPreferences: () -> Unit,
    onNavSearch: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isPlayerAlive by PlayerService.isAlive.collectAsStateWithLifecycle()
    val isPlayerPlaying by PlayerService.isPlaying.collectAsStateWithLifecycle()

    val appSettings = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.updateList()
    }

    val documentTreeResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        StorageManager.setPlaylistDirectory(uri).onSuccess {
            viewModel.setDefaultPath()
        }.onFailure {
            viewModel.showError(message = it.message ?: context.getString(R.string.error))
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
            viewModel.updateList()
        }
    }

    // Ask for Permissions
    LaunchedEffect(Unit) {
        val savedUri = PrefManager.safStoragePath.toUri()
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

    /**
     * Prompt and Explain Storage
     */
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

    /**
     * Error message Dialog
     */
    MenuErrorDialog(
        state = state,
        onConfirm = {
            viewModel.showError(null)
            viewModel.updateList()
        }
    )

    /**
     * Edit playlist dialog
     */
    MenuEditDialog(
        state = state,
        onConfirm = { res ->
            if (!res) {
                scope.launch {
                    val msg = "Failed to edit playlist"
                    snackBarHostState.showSnackbar(msg, "OK")
                }
            }

            viewModel.editPlaylist(null)
        },
        onDelete = { item ->
            PlaylistManager.delete(item.name)
            viewModel.editPlaylist(null)
        },
        onDismiss = { viewModel.editPlaylist(null) },
    )

    /**
     * New playlist dialog
     */
    MenuNewPlaylist(
        state = state,
        onConfirm = { res ->
            if (res) {
                viewModel.updateList()
            } else {
                viewModel.showError(
                    message = context.getString(R.string.dialog_message_error_create_playlist)
                )
            }

            viewModel.newPlaylist(false)
        },
        onDismiss = { viewModel.newPlaylist(false) }
    )

    LaunchedEffect(state.mediaPath) {
        if (state.mediaPath.isNotEmpty()) {
            viewModel.setupDataDir(
                name = context.getString(R.string.error_empty_playlist),
                comment = context.getString(R.string.error_empty_comment),
            ).onSuccess {
                viewModel.updateList()
            }.onFailure {
                viewModel.showError(it.message ?: context.getString(R.string.error))
            }
        }
    }

    LifecycleResumeEffect(Lifecycle.Event.ON_RESUME) {
        Timber.d("Lifecycle onResume")
        if (PrefManager.safStoragePath.isNotEmpty()) {
            viewModel.updateList()
        }
        onPauseOrDispose {
            Timber.d("Lifecycle onPause")
        }
    }

    HomeScreen(
        state = state,
        snackBarHostState = snackBarHostState,
        serviceAlive = isPlayerAlive,
        servicePlaying = isPlayerPlaying,
        onItemClick = { item ->
            if (item.isSpecial) {
                onNavFileList()
            } else {
                onNavPlaylist(item.docFile!!.uri.toString())
            }
        },
        onItemLongClick = { item ->
            if (item.isSpecial) {
                documentTreeResult.launch(null)
            } else {
                viewModel.editPlaylist(item)
            }
        },
        onRefresh = viewModel::updateList,
        onNewPlaylist = { viewModel.newPlaylist(true) },
        onTitleClicked = {
            if (PlayerService.isAlive.value) {
                Intent(context, PlayerActivity::class.java).also(playerResult::launch)
            }
        },
        onDownload = onNavSearch,
        onSettings = onNavPreferences,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: PlaylistMenuState,
    snackBarHostState: SnackbarHostState,
    serviceAlive: Boolean,
    servicePlaying: Boolean,
    onDownload: () -> Unit,
    onItemClick: (item: FileItem) -> Unit,
    onItemLongClick: (item: FileItem) -> Unit,
    onNewPlaylist: () -> Unit,
    onRefresh: () -> Unit,
    onRequestSettings: () -> Unit,
    onRequestStorage: () -> Unit,
    onSettings: () -> Unit,
    onTitleClicked: () -> Unit
) {
    val underlineAnimator = rememberSquigglyUnderlineAnimator(2.seconds)
    val extendedSpans = remember {
        ExtendedSpans(
            SquigglyUnderlineSpanPainter(
                wavelength = 32.sp,
                amplitude = 2.sp,
                bottomOffset = 4.sp,
                animator = underlineAnimator
            )
        )
    }

    val scrollState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0
        }
    }

    val view = LocalView.current
    val hasStorage = remember(state) {
        if (view.isInEditMode) {
            true
        } else {
            StorageManager.checkPermissions()
        }
    }

    Scaffold(
        modifier = Modifier,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            val topBarContainerColor = if (isScrolled) {
                MaterialTheme.colorScheme.surfaceVariant.darken(1.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }

            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarContainerColor,
                ),
                actions = {
                    IconButton(
                        enabled = hasStorage,
                        onClick = onDownload,
                        content = {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        }
                    )
                    IconButton(
                        enabled = hasStorage,
                        onClick = onSettings,
                        content = {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                        }
                    )
                },
                title = {
                    TextButton(
                        enabled = hasStorage,
                        onClick = onTitleClicked
                    ) {
                        ProvideTextStyle(
                            LocalTextStyle.current.merge(
                                TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        ) {
                            val text = themedText(
                                text = stringResource(id = R.string.app_name),
                                isAlive = serviceAlive,
                                isPlaying = servicePlaying

                            )

                            Text(
                                modifier = Modifier.drawBehind(extendedSpans),
                                text = remember(text, serviceAlive, servicePlaying) {
                                    extendedSpans.extend(text)
                                },
                                onTextLayout = { result ->
                                    extendedSpans.onTextLayout(result)
                                },
                                fontFamily = michromaFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (hasStorage) {
                ExtendedFloatingActionButton(
                    text = { Text(text = stringResource(id = R.string.menu_new_playlist)) },
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    expanded = !isScrolled,
                    onClick = onNewPlaylist
                )
            }
        }
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
                        top = 8.dp,
                        bottom = 96.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    state = scrollState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        content = { Text(text = "Set Directory") }
                    )
                    OutlinedButton(
                        onClick = onRequestSettings,
                        content = { Text(text = "Goto Settings") }
                    )
                }
            }

            ProgressbarIndicator(isLoading = state.isLoading)
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

@Composable
private fun MenuEditDialog(
    state: PlaylistMenuState,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (FileItem) -> Unit
) {
    EditPlaylistDialog(
        isShowing = state.editPlaylist != null,
        fileItem = state.editPlaylist,
        onConfirm = { item, newName, newComment ->
            val res = PlaylistManager().run {
                load(item.docFile!!.uri)
                rename(newName, newComment)
            }

            onConfirm(res.isSuccess)
        },
        onDismiss = onDismiss,
        onDelete = onDelete
    )
}

@Composable
private fun MenuNewPlaylist(
    state: PlaylistMenuState,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    NewPlaylistDialog(
        isShowing = state.newPlaylist,
        onConfirm = { name, comment ->
            val res = PlaylistManager().run {
                new(name, comment)
            }
            onConfirm(res.isSuccess)
        },
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuCardItem(
    item: FileItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            leadingContent = {
                Icon(
                    imageVector = if (item.isSpecial) {
                        Icons.Default.Folder
                    } else {
                        Icons.AutoMirrored.Filled.List
                    },
                    contentDescription = null
                )
            },
            headlineContent = { Text(text = item.name) },
            supportingContent = {
                Text(
                    text = item.comment.ifEmpty {
                        stringResource(id = R.string.error_no_comment)
                    }
                )
            }
        )
    }
}

@Preview
@Composable
private fun Preview_MenuCardItem() {
    XmpTheme(useDarkTheme = true) {
        MenuCardItem(
            item = FileItem(
                name = "Menu Card Item",
                comment = "Menu Card Comment",
                docFile = null
            ),
            onClick = { },
            onLongClick = { }
        )
    }
}

@Preview
@Composable
private fun Preview_PlaylistMenuScreen() {
    XmpTheme(useDarkTheme = true) {
        HomeScreen(
            state = PlaylistMenuState(
                mediaPath = "sdcard\\some\\path",
                isLoading = true,
                playlistItems = List(15) {
                    FileItem(
                        isSpecial = it >= 1,
                        name = "Name $it",
                        comment = "Comment $it",
                        docFile = null
                    )
                }
            ),
            snackBarHostState = SnackbarHostState(),
            serviceAlive = true,
            servicePlaying = false,
            onItemClick = {},
            onItemLongClick = {},
            onRefresh = {},
            onNewPlaylist = {},
            onTitleClicked = {},
            onDownload = {},
            onSettings = {},
            onRequestStorage = {},
            onRequestSettings = {}
        )
    }
}
