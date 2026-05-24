package com.lossydragon.media3.ui.screens.playlists

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.lossydragon.media3.db.entity.PlaylistEntity
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.model.PlaybackStatus
import com.lossydragon.media3.model.PlayerUiState
import com.lossydragon.media3.player.XmpPlayerViewModel
import com.lossydragon.media3.ui.NavKeyPlaylists
import com.lossydragon.media3.ui.components.GuruBox
import com.lossydragon.media3.ui.components.MessageBox
import com.lossydragon.media3.ui.components.ProgressbarIndicator
import com.lossydragon.media3.ui.screens.player.components.MiniPlayerBar
import com.lossydragon.media3.ui.screens.playlists.components.DeletePlaylistDialog
import com.lossydragon.media3.ui.screens.playlists.components.NewPlaylistDialog
import com.lossydragon.media3.ui.screens.playlists.components.PlaylistEntriesFabMenu
import com.lossydragon.media3.ui.screens.playlists.components.PlaylistEntryItem
import com.lossydragon.media3.ui.screens.playlists.components.PlaylistListItem
import com.lossydragon.media3.ui.screens.playlists.components.PlaylistsFabMenu
import com.lossydragon.media3.ui.theme.XmpTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun NavPlaylists(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val playerViewModel = koinViewModel<XmpPlayerViewModel>(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )
    val playlistsViewModel = koinViewModel<PlaylistsViewModel>()

    val playerState by playerViewModel.state.collectAsStateWithLifecycle()
    val playlistsState by playlistsViewModel.state.collectAsStateWithLifecycle()

    val backStack = rememberNavBackStack(NavKeyPlaylists.List)

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<NavKeyPlaylists.List> {
                PlaylistListScreen(
                    modifier = modifier,
                    state = playlistsState,
                    playerState = playerState,
                    snackbarHostState = snackbarHostState,
                    onBack = onBack,
                    onNavigateToPlayer = onNavigateToPlayer,
                    onMiniPlayerToggle = playerViewModel::togglePlayPause,
                    onMiniPlayerNext = playerViewModel::next,
                    onMiniPlayerPrev = playerViewModel::previous,
                    onSelectPlaylist = { playlist ->
                        val entry = NavKeyPlaylists.Entries(
                            playlistId = playlist.id,
                            playlistName = playlist.name,
                            playlistComment = playlist.comment,
                        )
                        playlistsViewModel.selectPlaylist(playlist)
                        backStack.add(entry)
                    },
                    onCreatePlaylist = playlistsViewModel::createPlaylist,
                    onDeletePlaylist = playlistsViewModel::deletePlaylist,
                    onExport = playlistsViewModel::exportPlaylists,
                    onImport = playlistsViewModel::importPlaylists,
                    onDismissError = playlistsViewModel::dismissError,
                    onDismissImport = playlistsViewModel::dismissImportResult,
                    onDismissExport = playlistsViewModel::dismissExportSuccess,
                    onRenamePlaylist = { playlist, name, comment ->
                        playlistsViewModel.renamePlaylist(playlist, name, comment)
                    },
                )
            }

            entry<NavKeyPlaylists.Entries> { key ->
                PlaylistEntriesScreen(
                    modifier = modifier,
                    playlistName = key.playlistName,
                    state = playlistsState,
                    playerState = playerState,
                    snackbarHostState = snackbarHostState,
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToPlayer = onNavigateToPlayer,
                    onMiniPlayerToggle = playerViewModel::togglePlayPause,
                    onMiniPlayerNext = playerViewModel::next,
                    onMiniPlayerPrev = playerViewModel::previous,
                    onPlayEntry = playlistsViewModel::playEntry,
                    onPlayAll = { playlistsViewModel.playPlaylist() },
                    onShuffle = { playlistsViewModel.playPlaylist(isShuffle = true) },
                    onRemoveEntry = playlistsViewModel::removeFromPlaylist,
                    onReorderEntry = playlistsViewModel::reorderEntry,
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaylistListScreen(
    modifier: Modifier,
    state: PlaylistsUiState,
    playerState: PlayerUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onMiniPlayerToggle: () -> Unit,
    onMiniPlayerNext: () -> Unit,
    onMiniPlayerPrev: () -> Unit,
    onSelectPlaylist: (PlaylistEntity) -> Unit,
    onCreatePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onExport: (Uri) -> Unit,
    onImport: (Uri) -> Unit,
    onDismissError: () -> Unit,
    onDismissImport: () -> Unit,
    onDismissExport: () -> Unit,
    onRenamePlaylist: (PlaylistEntity, String, String) -> Unit
) {
    val hasModule = playerState.currentModule != null

    var isFabExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val showSnackBar: (String) -> Unit = {
        scope.launch { snackbarHostState.showSnackbar(message = it) }
    }

    // Import result snackbar
    LaunchedEffect(state.importResult) {
        state.importResult?.let { result ->
            val message = buildString {
                append("Imported ${result.playlistsImported} playlists, ")
                append("${result.entriesImported} entries")
                if (result.skipped > 0) append(", ${result.skipped} skipped")
            }
            snackbarHostState.showSnackbar(message = message, actionLabel = "Close")
            onDismissImport()
        }
    }

    // Export success snackbar
    LaunchedEffect(state.exportSuccess) {
        if (state.exportSuccess) {
            snackbarHostState.showSnackbar("Playlists exported successfully")
            onDismissExport()
        }
    }

    // New Playlist
    var isNewPlaylistDialog by remember { mutableStateOf(false) }
    if (isNewPlaylistDialog) {
        NewPlaylistDialog(
            onDismiss = { isNewPlaylistDialog = false },
            onCreate = onCreatePlaylist,
            onError = showSnackBar
        )
    }

    // Delete Playlist
    var isDeletePlaylistDialog by remember { mutableStateOf(false) }
    var deletePlaylistItem by remember { mutableStateOf<PlaylistEntity?>(null) }
    val showDeletePlaylistDialog: (PlaylistEntity) -> Unit = {
        deletePlaylistItem = it
        isDeletePlaylistDialog = true
    }
    val dismissDeletePlaylistDialog: () -> Unit = {
        isDeletePlaylistDialog = false
        deletePlaylistItem = null
    }
    if (isDeletePlaylistDialog && deletePlaylistItem != null) {
        DeletePlaylistDialog(
            playlist = deletePlaylistItem!!,
            onDismiss = dismissDeletePlaylistDialog,
            onConfirm = { onDeletePlaylist(deletePlaylistItem!!) },
        )
    }

    // Rename
    var isRenamePlaylistDialog by remember { mutableStateOf(false) }
    var renamePlaylistItem by remember { mutableStateOf<PlaylistEntity?>(null) }

    if (isRenamePlaylistDialog && renamePlaylistItem != null) {
        NewPlaylistDialog(
            initialName = renamePlaylistItem!!.name,
            initialComment = renamePlaylistItem!!.comment,
            onDismiss = {
                isRenamePlaylistDialog = false
                renamePlaylistItem = null
            },
            onCreate = { name, comment ->
                onRenamePlaylist(renamePlaylistItem!!, name, comment)
            },
            onError = showSnackBar,
        )
    }

    // Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri == null) {
                showSnackBar("Import Cancelled")
                return@rememberLauncherForActivityResult
            }
            onImport(uri)
        }
    )

    // Export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri == null) {
                showSnackBar("Export Cancelled")
                return@rememberLauncherForActivityResult
            }
            onExport(uri)
        }
    )

    // Content
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Playlists") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        content = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    )
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !state.isLoading && state.error == null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                content = {
                    PlaylistsFabMenu(
                        expanded = isFabExpanded,
                        onExpand = { isFabExpanded = !isFabExpanded },
                        onImport = {
                            importLauncher.launch(arrayOf("application/json"))
                            isFabExpanded = false
                        },
                        onExport = {
                            exportLauncher.launch("xmp_playlists.json")
                            isFabExpanded = false
                        },
                        onNewPlaylist = {
                            isNewPlaylistDialog = true
                            isFabExpanded = false
                        },
                    )
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = hasModule,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                content = {
                    MiniPlayerBar(
                        state = playerState,
                        onTap = onNavigateToPlayer,
                        onPlayPause = onMiniPlayerToggle,
                        onNext = onMiniPlayerNext,
                        onPrevious = onMiniPlayerPrev,
                    )
                }
            )
        },
    ) { padding ->
        val listState = rememberLazyListState()

        Crossfade(
            targetState = state.isLoading,
            content = { isLoading ->
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center,
                        content = { ProgressbarIndicator(isLoading = true) }
                    )
                } else {
                    if (!state.error.isNullOrBlank()) {
                        GuruBox(
                            modifier = Modifier.padding(padding),
                            message = state.error,
                            onBack = {
                                onDismissError()
                                onBack()
                            },
                        )
                    } else if (state.playlists.isEmpty()) {
                        MessageBox(
                            modifier = Modifier.padding(padding),
                            text = "No Playlists found",
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .padding(padding)
                                .fillMaxSize(),
                            state = listState,
                            content = {
                                items(
                                    items = state.playlists,
                                    key = { it.id },
                                    itemContent = { item ->
                                        PlaylistListItem(
                                            item = item,
                                            onSelect = { onSelectPlaylist(item) },
                                            onDelete = { showDeletePlaylistDialog(item) },
                                            onRename = {
                                                renamePlaylistItem = item
                                                isRenamePlaylistDialog = true
                                            },
                                        )
                                    }
                                )
                            }
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaylistEntriesScreen(
    modifier: Modifier,
    playlistName: String,
    state: PlaylistsUiState,
    playerState: PlayerUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onMiniPlayerToggle: () -> Unit,
    onMiniPlayerNext: () -> Unit,
    onMiniPlayerPrev: () -> Unit,
    onPlayEntry: (ModuleFile) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onRemoveEntry: (ModuleFile) -> Unit,
    onReorderEntry: (Int, Int) -> Unit
) {
    val hasModule = playerState.currentModule != null
    var isFabExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(playlistName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.entries.isNotEmpty(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                PlaylistEntriesFabMenu(
                    expanded = isFabExpanded,
                    onExpand = { isFabExpanded = !isFabExpanded },
                    onPlayAll = onPlayAll,
                    onShuffle = onShuffle,
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = hasModule,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                MiniPlayerBar(
                    state = playerState,
                    onTap = onNavigateToPlayer,
                    onPlayPause = onMiniPlayerToggle,
                    onNext = onMiniPlayerNext,
                    onPrevious = onMiniPlayerPrev,
                )
            }
        },
    ) { padding ->
        val hapticFeedback = LocalHapticFeedback.current
        val lazyListState = rememberLazyListState()
        val reorderableLazyListState = rememberReorderableLazyListState(
            lazyListState = lazyListState,
            onMove = { from, to ->
                onReorderEntry(from.index, to.index)
                hapticFeedback.performHapticFeedback(
                    HapticFeedbackType.SegmentFrequentTick
                )
            }
        )

        Box(modifier = Modifier.padding(padding)) {
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                    content = { ProgressbarIndicator(isLoading = true) }
                )

                state.entries.isEmpty() -> MessageBox(text = "No entries in this playlist")

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        itemsIndexed(
                            items = state.entries,
                            key = { _, f -> f.uri.toString() },
                        ) { index, file ->
                            ReorderableItem(
                                state = reorderableLazyListState,
                                key = file.uri.toString()
                            ) { isDragging ->
                                val elevation by animateDpAsState(
                                    if (isDragging) 4.dp else 0.dp,
                                    label = "elevation"
                                )
                                Surface(shadowElevation = elevation) {
                                    with(this@ReorderableItem) {
                                        PlaylistEntryItem(
                                            index = index,
                                            file = file,
                                            onPlay = { onPlayEntry(file) },
                                            onRemove = { onRemoveEntry(file) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preview
 */

private val samplePlaylists = persistentListOf(
    PlaylistEntity(
        id = 1L,
        name = "Amiga Classics",
        comment = "Comment Amiga Classics",
        createdAt = System.currentTimeMillis() - 86_400_000 * 7
    ),
    PlaylistEntity(
        id = 2L,
        name = "Fast Tracker II",
        comment = "Comment Fast Tracker II",
        createdAt = System.currentTimeMillis() - 86_400_000 * 3
    ),
    PlaylistEntity(
        id = 3L,
        name = "Impulse Tracker",
        comment = "Comment Impulse Tracker",
        createdAt = System.currentTimeMillis() - 86_400_000
    ),
    PlaylistEntity(
        id = 4L,
        name = "Scream Tracker 3",
        comment = "Comment Scream Tracker 3",
        createdAt = System.currentTimeMillis()
    ),
)

private val sampleEntries = persistentListOf(
    ModuleFile(
        uri = "content://preview/1".toUri(),
        name = "alpharapii.mod",
        sizeBytes = 45_678L,
        extension = "mod",
        resolvedName = "alpharapii",
        resolvedType = "Amiga Protracker/Compatible",
    ),
    ModuleFile(
        uri = "content://preview/2".toUri(),
        name = "aegis_-_beneath_the_fallen_stars.it",
        sizeBytes = 1_820_792L,
        extension = "it",
        resolvedName = "Beneath the Fallen Stars",
        resolvedType = "Impulse Tracker",
    ),
    ModuleFile(
        uri = "content://preview/3".toUri(),
        name = "ballada-remix.mod",
        sizeBytes = 22_334L,
        extension = "mod",
        resolvedName = "ballada-remix",
        resolvedType = "Amiga Protracker/Compatible",
    ),
    ModuleFile(
        uri = "content://preview/4".toUri(),
        name = "KRAKEN.IT",
        sizeBytes = 312_456L,
        extension = "it",
        resolvedName = "Kraken of the Sea",
        resolvedType = "Impulse Tracker",
    ),
    ModuleFile(
        uri = "content://preview/5".toUri(),
        name = "SONG0.S3M",
        sizeBytes = 98_123L,
        extension = "s3m",
        resolvedName = "Escape",
        resolvedType = "Scream Tracker 3",
    ),
)

private val playingState = PlayerUiState(
    status = PlaybackStatus.PLAYING,
    currentModule = sampleEntries[1],
    moduleName = "Beneath the Fallen Stars",
    moduleType = "Impulse Tracker",
    positionMs = 45_000L,
    durationMs = 252_849L,
    currentQueueIndex = 1,
)

private data class PlaylistPreviewState(
    val playlistsState: PlaylistsUiState,
    val playerState: PlayerUiState,
    val description: String
)

private class PlaylistListPreviewParameter : PreviewParameterProvider<PlaylistPreviewState> {
    override val values = sequenceOf(
        PlaylistPreviewState(
            description = "Empty",
            playlistsState = PlaylistsUiState(),
            playerState = PlayerUiState(),
        ),
        PlaylistPreviewState(
            description = "Playlists — idle",
            playlistsState = PlaylistsUiState(playlists = samplePlaylists),
            playerState = PlayerUiState(),
        ),
        PlaylistPreviewState(
            description = "Playlists — playing",
            playlistsState = PlaylistsUiState(playlists = samplePlaylists),
            playerState = playingState,
        ),
        PlaylistPreviewState(
            description = "Error",
            playlistsState = PlaylistsUiState(error = "Failed to load."),
            playerState = PlayerUiState(),
        ),
    )

    override fun getDisplayName(index: Int): String =
        "$index -" + values.toList()[index].description
}

private class PlaylistEntriesPreviewParameter : PreviewParameterProvider<PlaylistPreviewState> {
    override val values = sequenceOf(
        PlaylistPreviewState(
            description = "Loading",
            playlistsState = PlaylistsUiState(
                playlists = samplePlaylists,
                selectedPlaylist = samplePlaylists[0],
                isLoading = true,
            ),
            playerState = PlayerUiState(),
        ),
        PlaylistPreviewState(
            description = "Entries — idle",
            playlistsState = PlaylistsUiState(
                playlists = samplePlaylists,
                selectedPlaylist = samplePlaylists[0],
                entries = sampleEntries,
            ),
            playerState = PlayerUiState(),
        ),
        PlaylistPreviewState(
            description = "Entries — playing",
            playlistsState = PlaylistsUiState(
                playlists = samplePlaylists,
                selectedPlaylist = samplePlaylists[1],
                entries = sampleEntries,
            ),
            playerState = playingState,
        ),
        PlaylistPreviewState(
            description = "Empty playlist",
            playlistsState = PlaylistsUiState(
                playlists = samplePlaylists,
                selectedPlaylist = samplePlaylists[2],
                entries = persistentListOf(),
            ),
            playerState = PlayerUiState(),
        ),
    )

    override fun getDisplayName(index: Int): String =
        "$index -" + values.toList()[index].description
}

@Preview(showBackground = true, name = "Playlist List Screen")
@Composable
private fun PreviewPlaylistListScreen(
    @PreviewParameter(PlaylistListPreviewParameter::class) params: PlaylistPreviewState
) {
    XmpTheme {
        PlaylistListScreen(
            modifier = Modifier,
            state = params.playlistsState,
            playerState = params.playerState,
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onNavigateToPlayer = {},
            onMiniPlayerToggle = {},
            onMiniPlayerNext = {},
            onMiniPlayerPrev = {},
            onSelectPlaylist = {},
            onCreatePlaylist = { _, _ -> },
            onDeletePlaylist = {},
            onExport = {},
            onImport = {},
            onDismissError = {},
            onDismissImport = {},
            onDismissExport = {},
            onRenamePlaylist = { _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Playlist Entries Screen")
@Composable
private fun PreviewPlaylistEntriesScreen(
    @PreviewParameter(PlaylistEntriesPreviewParameter::class) params: PlaylistPreviewState
) {
    XmpTheme {
        PlaylistEntriesScreen(
            modifier = Modifier,
            playlistName = params.playlistsState.selectedPlaylist?.name ?: "AAAAA",
            state = params.playlistsState,
            playerState = params.playerState,
            snackbarHostState = SnackbarHostState(),
            onBack = {},
            onNavigateToPlayer = {},
            onMiniPlayerToggle = {},
            onMiniPlayerNext = {},
            onMiniPlayerPrev = {},
            onPlayEntry = {},
            onPlayAll = {},
            onShuffle = {},
            onRemoveEntry = {},
            onReorderEntry = { _, _ -> },
        )
    }
}
