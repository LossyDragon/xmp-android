package com.lossydragon.media3.ui.browser

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lossydragon.media3.model.BrowserUiState
import com.lossydragon.media3.model.FileItem
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.model.PlaybackStatus
import com.lossydragon.media3.model.PlayerUiState
import com.lossydragon.media3.player.XmpPlayerViewModel
import com.lossydragon.media3.ui.browser.components.BreadCrumbs
import com.lossydragon.media3.ui.browser.components.EmptyPrompt
import com.lossydragon.media3.ui.browser.components.ModuleList
import com.lossydragon.media3.ui.player.components.MiniPlayerBar
import com.lossydragon.media3.ui.theme.XmpTheme
import kotlinx.collections.immutable.persistentListOf
import org.helllabs.libxmp.model.ModInfo
import org.koin.androidx.compose.koinViewModel

@Composable
fun FileBrowserScreenRoute(
    modifier: Modifier = Modifier,
    onNavigateToPlayer: () -> Unit,
    onBack: () -> Unit
) {
    val browserViewModel = koinViewModel<FileBrowserViewModel>()
    val playerViewModel = koinViewModel<XmpPlayerViewModel>(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )

    val browserState by browserViewModel.state.collectAsStateWithLifecycle()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { it?.let { browserViewModel.onRootFolderPicked(it) } }
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
        onNavigateToPlayer = onNavigateToPlayer,
        onBack = onBack,
        onFolderPick = { folderPicker.launch(null) },
        onNavigateUp = { browserViewModel.navigateUp() },
        canNavigateUp = { browserViewModel.canNavigateUp() },
        onBreadcrumb = { browserViewModel.navigateToBreadcrumb(it) },
        onPlayAll = {
            if (browserState.files.isNotEmpty()) {
                playerViewModel.playAll(
                    files = browserState.files,
                    startAt = 0,
                    isShuffle = browserState.isShuffle,
                    isLoop = browserState.isLoop,
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
                isLoop = browserState.isLoop,
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

@Composable
private fun FileBrowserScreen(
    modifier: Modifier = Modifier,
    browserState: BrowserUiState,
    playerState: PlayerUiState,
    onNavigateToPlayer: () -> Unit,
    onBack: () -> Unit,
    onFolderPick: () -> Unit,
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    val hasModule = playerState.currentModule != null

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (browserState.currentPath.isEmpty()) {
                                "Module Browser"
                            } else {
                                browserState.currentPath.substringAfterLast(
                                    '/'
                                ).substringAfterLast(':')
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        if (canNavigateUp()) {
                            IconButton(onClick = onNavigateUp) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onFolderPick) {
                            Icon(Icons.Default.FolderOpen, null)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
                if (browserState.breadcrumbs.isNotEmpty()) {
                    BreadCrumbs(
                        breadcrumbs = browserState.breadcrumbs,
                        onCrumbClick = onBreadcrumb,
                    )
                    HorizontalDivider()
                }
            }
        },
        floatingActionButton = {
            // Hide FAB when mini player is visible to avoid overlap
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
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                    content = { CircularProgressIndicator() }
                )

                !browserState.hasStorageAccess -> EmptyPrompt(
                    padding = padding,
                    onPick = onFolderPick,
                )

                browserState.files.isEmpty() && browserState.directories.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
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
            onNavigateToPlayer = {},
            onBack = {},
            onFolderPick = {},
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
