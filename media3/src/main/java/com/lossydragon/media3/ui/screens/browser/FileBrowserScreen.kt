package com.lossydragon.media3.ui.screens.browser

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.lossydragon.media3.model.BrowserSortOrder
import com.lossydragon.media3.model.BrowserUiState
import com.lossydragon.media3.model.FileItem
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.model.PlaybackStatus
import com.lossydragon.media3.model.PlayerUiState
import com.lossydragon.media3.player.XmpPlayerViewModel
import com.lossydragon.media3.ui.screens.browser.components.BreadCrumbs
import com.lossydragon.media3.ui.screens.browser.components.BrowserInputField
import com.lossydragon.media3.ui.screens.browser.components.EmptyPrompt
import com.lossydragon.media3.ui.screens.browser.components.ModuleList
import com.lossydragon.media3.ui.screens.player.components.MiniPlayerBar
import com.lossydragon.media3.ui.theme.XmpTheme
import kotlinx.collections.immutable.persistentListOf
import org.koin.androidx.compose.koinViewModel

@Composable
fun FileBrowserScreen(
    modifier: Modifier = Modifier,
    onNavigateToPlayer: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val browserViewModel = koinViewModel<FileBrowserViewModel>()
    val playerViewModel = koinViewModel<XmpPlayerViewModel>(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )

    val browserState by browserViewModel.state.collectAsStateWithLifecycle()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let {
                // Take both read and write permissions
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, flags)
                browserViewModel.onRootFolderPicked(it)
            }
        }
    )

    BackHandler {
        if (!browserViewModel.navigateUp()) {
            onBack()
        }
    }

    FileBrowserScreenContent(
        modifier = modifier,
        browserState = browserState,
        playerState = playerState,
        filterQuery = browserState.filterQuery,
        onFilter = browserViewModel::setFilter,
        onNavigateToPlayer = onNavigateToPlayer,
        onShuffle = browserViewModel::setShuffle,
        onRepeatMode = browserViewModel::setRepeatMode,
        onBack = onBack,
        onFolderPick = { folderPicker.launch(null) },
        onSortOrder = browserViewModel::setSortOrder,
        onNavigateUp = browserViewModel::navigateUp,
        canNavigateUp = browserViewModel::canNavigateUp,
        onBreadcrumb = browserViewModel::navigateToBreadcrumb,
        onPlayAll = {
            if (browserState.files.isNotEmpty()) {
                playerViewModel.playAll(
                    files = browserState.files,
                    startAt = 0,
                    isShuffle = browserState.isShuffle,
                    repeatMode = browserState.repeatMode,
                )
                onNavigateToPlayer()
            }
        },
        onSelect = { file ->
            val index = browserState.files.indexOf(file)
            playerViewModel.playAll(
                files = browserState.files,
                startAt = if (index >= 0) index else 0,
                isShuffle = browserState.isShuffle,
                repeatMode = browserState.repeatMode,
            )
            onNavigateToPlayer()
        },
        onDir = browserViewModel::navigateInto,
        onMiniPlayerTap = onNavigateToPlayer,
        onMiniPlayerToggle = playerViewModel::togglePlayPause,
        onMiniPlayerNext = playerViewModel::next,
        onMiniPlayerPrev = playerViewModel::previous,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FileBrowserScreenContent(
    modifier: Modifier = Modifier,
    browserState: BrowserUiState,
    playerState: PlayerUiState,
    filterQuery: String,
    onFilter: (String) -> Unit,
    onNavigateToPlayer: () -> Unit,
    onShuffle: (Boolean) -> Unit,
    onRepeatMode: (Int) -> Unit,
    onBack: () -> Unit,
    onFolderPick: () -> Unit,
    onSortOrder: (BrowserSortOrder) -> Unit,
    onNavigateUp: () -> Unit,
    canNavigateUp: () -> Boolean,
    onBreadcrumb: (Int) -> Unit,
    onPlayAll: () -> Unit,
    onSelect: (ModuleFile) -> Unit,
    onDir: (FileItem) -> Unit,
    onMiniPlayerTap: () -> Unit,
    onMiniPlayerToggle: () -> Unit,
    onMiniPlayerNext: () -> Unit,
    onMiniPlayerPrev: () -> Unit
) {
    val searchBarState = rememberSearchBarWithGapState()
    val textFieldState = rememberTextFieldState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(
        scrolledSearchBarContainerColor = Color.Unspecified,
        scrolledAppBarContainerColor = Color.Unspecified,
    )
    val listState = rememberLazyListState()
    val hasModule = playerState.currentModule != null

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { onFilter(it) }
    }

    val inputField: @Composable () -> Unit = {
        BrowserInputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            colors = appBarWithSearchColors.searchBarColors.inputFieldColors,
            sortOrder = browserState.sortOrder,
            onSortOrder = onSortOrder,
            onFolderPick = onFolderPick,
            onFilter = onFilter,
        )
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                AppBarWithSearch(
                    scrollBehavior = scrollBehavior,
                    state = searchBarState,
                    colors = appBarWithSearchColors,
                    inputField = inputField,
                    shape = RoundedCornerShape(16.dp)
                )
                ExpandedDockedSearchBarWithGap(
                    state = searchBarState,
                    inputField = inputField,
                    content = { /* TODO maybe add this, for single plays */ }
                )
                if (browserState.breadcrumbs.isNotEmpty()) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        BreadCrumbs(
                            breadcrumbs = browserState.breadcrumbs,
                            onCrumbClick = onBreadcrumb,
                        )
                    }
                    HorizontalDivider()
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !hasModule && browserState.hasStorageAccess && !browserState.isLoading,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    HorizontalFloatingToolbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = -ScreenOffset)
                            .zIndex(1f),
                        expanded = true,
                        shape = RoundedCornerShape(16.dp),
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = onPlayAll,
                                content = {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null
                                    )
                                }
                            )
                        },
                        content = {
                            IconButton(
                                onClick = { onShuffle(!browserState.isShuffle) },
                                content = {
                                    val icon = if (browserState.isShuffle) {
                                        Icons.Default.ShuffleOn
                                    } else {
                                        Icons.Default.Shuffle
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (browserState.isShuffle) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            )

                            IconButton(
                                onClick = {
                                    val next = when (browserState.repeatMode) {
                                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                                        Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                                        else -> Player.REPEAT_MODE_OFF
                                    }
                                    onRepeatMode(next)
                                },
                                content = {
                                    Icon(
                                        imageVector = when (browserState.repeatMode) {
                                            Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOneOn
                                            Player.REPEAT_MODE_ALL -> Icons.Default.RepeatOn
                                            else -> Icons.Default.Repeat
                                        },
                                        contentDescription = "Repeat",
                                        tint = if (browserState.repeatMode !=
                                            Player.REPEAT_MODE_OFF
                                        ) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = hasModule,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                MiniPlayerBar(
                    state = playerState,
                    onTap = onMiniPlayerTap,
                    onPlayPause = onMiniPlayerToggle,
                    onNext = onMiniPlayerNext,
                    onPrevious = onMiniPlayerPrev,
                )
            }
        },
        content = { padding ->
            when {
                browserState.isLoading -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                    content = { CircularProgressIndicator() }
                )

                !browserState.hasStorageAccess -> EmptyPrompt(
                    padding = padding,
                    onPick = onFolderPick,
                )

                browserState.files.isEmpty() && browserState.directories.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                    content = { Text(text = "No module files found in this folder.") }
                )

                else -> ModuleList(
                    state = browserState,
                    padding = padding,
                    listState = listState,
                    onDir = onDir,
                    onSelect = onSelect,
                )
            }
        }
    )
}

/**
 * Preview
 */
private data class BrowserPreviewState(
    val browserState: BrowserUiState,
    val playerState: PlayerUiState,
    val description: String
)

private class BrowserPreviewParameter : PreviewParameterProvider<BrowserPreviewState> {

    private val sampleFiles = persistentListOf(
        ModuleFile(
            uri = "content://preview/1".toUri(),
            name = "a_journey_into_sound.far",
            sizeBytes = 123_456L,
            extension = "far",
            resolvedName = "A Journey Into Sound",
            resolvedType = "Farandole Composer",
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
            name = "alpharapii.mod",
            sizeBytes = 45_678L,
            extension = "mod",
            resolvedName = "alpharapii",
            resolvedType = "Amiga Protracker/Compatible",
        ),
        ModuleFile(
            uri = "content://preview/4".toUri(),
            name = "chiptune_no_184.mod",
            sizeBytes = 6_658L,
            extension = "mod",
            resolvedName = "Chiptune No. 184",
            resolvedType = "Amiga Protracker/Compatible",
        ),
    )

    private val sampleDirs = persistentListOf(
        FileItem(name = "TheModArchive", uri = "1".toUri(), isDirectory = true, size = 0L),
        FileItem(name = "Demos", uri = "2".toUri(), isDirectory = true, size = 0L),
    )

    private val playingState = PlayerUiState(
        status = PlaybackStatus.PLAYING,
        currentModule = sampleFiles[1],
        moduleName = "Beneath the Fallen Stars",
        moduleType = "Impulse Tracker",
        positionMs = 62_000L,
        durationMs = 252_849L,
        currentQueueIndex = 1,
    )

    private val baseBrowserState = BrowserUiState(
        currentPath = "primary:Xmp/Modules",
        hasStorageAccess = true,
        isLoading = false,
        isShuffle = false,
        breadcrumbs = persistentListOf("Xmp", "Modules"),
        directories = sampleDirs,
        files = sampleFiles,
        sortOrder = BrowserSortOrder.NAME,
    )

    override val values = sequenceOf(
        // Normal browsing, no playback
        BrowserPreviewState(
            description = "Browsing — idle",
            browserState = baseBrowserState.copy(
                isShuffle = true,
                repeatMode = Player.REPEAT_MODE_ONE
            ),
            playerState = PlayerUiState(),
        ),
        // Browsing with mini player visible
        BrowserPreviewState(
            description = "Browsing — playing",
            browserState = baseBrowserState,
            playerState = playingState,
        ),
        // No storage access yet
        BrowserPreviewState(
            description = "No storage access",
            browserState = BrowserUiState(hasStorageAccess = false, isLoading = false),
            playerState = PlayerUiState(),
        ),
        // Loading state
        BrowserPreviewState(
            description = "Loading",
            browserState = BrowserUiState(hasStorageAccess = true, isLoading = true),
            playerState = PlayerUiState(),
        ),
        // Empty directory
        BrowserPreviewState(
            description = "Empty directory",
            browserState = baseBrowserState.copy(
                files = persistentListOf(),
                directories = persistentListOf(),
                breadcrumbs = persistentListOf("Xmp", "Empty"),
            ),
            playerState = PlayerUiState(),
        ),
        // Filtered results
        BrowserPreviewState(
            description = "Filtered — 'mod'",
            browserState = baseBrowserState.copy(
                filterQuery = "mod",
                files = persistentListOf(sampleFiles[2], sampleFiles[3]),
                directories = persistentListOf(),
            ),
            playerState = playingState,
        ),
        // Sorted by size
        BrowserPreviewState(
            description = "Sorted by size",
            browserState = baseBrowserState.copy(
                sortOrder = BrowserSortOrder.SIZE,
                files = persistentListOf(
                    sampleFiles[3], // 6KB
                    sampleFiles[2], // 45KB
                    sampleFiles[0], // 123KB
                    sampleFiles[1], // 1.8MB
                ),
            ),
            playerState = PlayerUiState(),
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview(
    @PreviewParameter(BrowserPreviewParameter::class) params: BrowserPreviewState
) {
    XmpTheme {
        FileBrowserScreenContent(
            browserState = params.browserState,
            playerState = params.playerState,
            filterQuery = params.browserState.filterQuery,
            onFilter = {},
            onShuffle = {},
            onRepeatMode = {},
            onNavigateToPlayer = {},
            onBack = {},
            onFolderPick = {},
            onSortOrder = {},
            onNavigateUp = {},
            canNavigateUp = { params.browserState.breadcrumbs.size > 1 },
            onBreadcrumb = {},
            onPlayAll = {},
            onSelect = {},
            onDir = {},
            onMiniPlayerTap = {},
            onMiniPlayerToggle = {},
            onMiniPlayerNext = {},
            onMiniPlayerPrev = {},
        )
    }
}
