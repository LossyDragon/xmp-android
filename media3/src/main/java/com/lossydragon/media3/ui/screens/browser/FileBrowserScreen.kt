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
import androidx.compose.foundation.text.input.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.tooling.preview.*
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
fun FileBrowserScreenRoute(
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

    FileBrowserScreen(
        modifier = modifier,
        browserState = browserState,
        playerState = playerState,
        filterQuery = browserState.filterQuery,
        onFilter = browserViewModel::setFilter,
        onNavigateToPlayer = onNavigateToPlayer,
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
private fun FileBrowserScreen(
    modifier: Modifier = Modifier,
    browserState: BrowserUiState,
    playerState: PlayerUiState,
    filterQuery: String,
    onFilter: (String) -> Unit,
    onNavigateToPlayer: () -> Unit,
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
        floatingActionButton = {
            AnimatedVisibility(
                visible = !hasModule && browserState.hasStorageAccess && !browserState.isLoading,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                FloatingActionButton(onClick = onPlayAll) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play All")
                }
            }
        },
        bottomBar = {
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

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        FileBrowserScreen(
            modifier = Modifier,
            browserState = BrowserUiState(
                currentPath = "primary:Xmp/Modules",
                hasStorageAccess = true,
                isLoading = false,
                isShuffle = true,
                isLoop = false,
                breadcrumbs = persistentListOf("Xmp", "Modules"),
                directories = persistentListOf(
                    FileItem(
                        name = "TheModArchive",
                        uri = "1".toUri(),
                        isDirectory = true,
                        size = 0L
                    ),
                    FileItem(name = "Demos", uri = "2".toUri(), isDirectory = true, size = 0L),
                ),
                files = persistentListOf(
                    ModuleFile(
                        uri = "content://preview/1".toUri(),
                        name = "a_journey_into_sound.far",
                        sizeBytes = 123456L,
                        extension = "far"
                    ),
                    ModuleFile(
                        uri = "content://preview/2".toUri(),
                        name = "aegis_-_beneath_the_fallen_stars.it",
                        sizeBytes = 1820792L,
                        extension = "it"
                    ),
                    ModuleFile(
                        uri = "content://preview/3".toUri(),
                        name = "alpharapii.mod",
                        sizeBytes = 45678L,
                        extension = "mod"
                    ),
                    ModuleFile(
                        uri = "content://preview/4".toUri(),
                        name = "chiptune_no_184.mod",
                        sizeBytes = 6658L,
                        extension = "mod"
                    ),
                ),
            ),
            playerState = PlayerUiState(
                status = PlaybackStatus.PLAYING,
                currentModule = ModuleFile(
                    uri = "content://preview/1".toUri(),
                    name = "a_journey_into_sound.far",
                    sizeBytes = 123456L,
                    extension = "far",
                ),
                moduleName = "A Journey Into Sound",
                moduleType = "FAR",
                positionMs = 62000L,
                durationMs = 252849L,
                currentQueueIndex = 0,
            ),
            filterQuery = "",
            onFilter = {},
            onNavigateToPlayer = {},
            onBack = {},
            onFolderPick = {},
            onSortOrder = {},
            onNavigateUp = {},
            canNavigateUp = { false },
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
