package com.lossydragon.media3.ui.browser

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
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
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.player.XmpPlayerViewModel
import com.lossydragon.media3.ui.player.MiniPlayerBar
import com.lossydragon.media3.util.formatSize
import kotlinx.collections.immutable.ImmutableList
import org.koin.androidx.compose.koinViewModel

@Composable
fun FileBrowserScreen(
    modifier: Modifier,
    onNavigateToPlayer: () -> Unit,
    onBack: () -> Unit
) {
    val browserViewModel = koinViewModel<FileBrowserViewModel>()
    val playerViewModel = koinViewModel<XmpPlayerViewModel>(
        viewModelStoreOwner = LocalActivity.current as ComponentActivity
    )

    val browserState by browserViewModel.state.collectAsStateWithLifecycle()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { it?.let { browserViewModel.onRootFolderPicked(it) } }
    )

    BackHandler {
        if (!browserViewModel.navigateUp()) {
            onBack()
        }
    }

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
                                browserState.currentPath.substringAfterLast('/')
                                    .substringAfterLast(':')
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        if (browserViewModel.canNavigateUp()) {
                            IconButton(
                                onClick = { browserViewModel.navigateUp() },
                                content = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { folderPicker.launch(null) },
                            content = {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
                if (browserState.breadcrumbs.isNotEmpty()) {
                    BreadCrumbs(
                        breadcrumbs = browserState.breadcrumbs,
                        onCrumbClick = { browserViewModel.navigateToBreadcrumb(it) },
                    )
                    HorizontalDivider()
                }
            }
        },
        bottomBar = {
            if (playerState.currentModule != null) {
                MiniPlayerBar(
                    state = playerState,
                    onTap = onNavigateToPlayer,
                    onToggle = playerViewModel::togglePlayPause,
                )
            } else {
                BrowserBottomBar(
                    isShuffle = browserState.isShuffle,
                    isLoop = browserState.isLoop,
                    onShuffle = browserViewModel::setShuffle,
                    onLoop = browserViewModel::setLoop,
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
                    onPick = { folderPicker.launch(null) },
                )

                browserState.files.isEmpty() && browserState.directories.isEmpty() ->
                    Box(
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
                    onDir = browserViewModel::navigateInto,
                    onSelect = { file ->
                        // Play all files in directory, starting at the tapped file
                        val index = browserState.files.indexOf(file)
                        playerViewModel.playAll(
                            files = browserState.files,
                            startAt = if (index >= 0) index else 0,
                            isShuffle = browserState.isShuffle,
                            isLoop = browserState.isLoop,
                        )
                        onNavigateToPlayer()
                    },
                )
            }
        }
    )
}

@Composable
private fun EmptyPrompt(padding: PaddingValues, onPick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "No folder selected",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = "Tap the folder icon to browse your tracker modules",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    )
}

@Composable
private fun ModuleList(
    state: BrowserUiState,
    padding: PaddingValues,
    listState: LazyListState,
    onDir: (FileItem) -> Unit,
    onSelect: (ModuleFile) -> Unit
) {
    LazyColumn(
        state = listState,
        contentPadding = padding,
        modifier = Modifier.fillMaxSize(),
        content = {
            items(
                items = state.directories,
                key = { it.uri.toString() },
                itemContent = { dir ->
                    DirectoryListItem(dir = dir, onClick = { onDir(dir) })
                }
            )
            items(
                items = state.files,
                key = { it.uri.toString() },
                itemContent = { file ->
                    ModuleListItem(file = file, onClick = { onSelect(file) })
                }
            )
        }
    )
}

@Composable
private fun DirectoryListItem(dir: FileItem, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(dir.name) },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
private fun BreadCrumbs(
    breadcrumbs: ImmutableList<String>,
    onCrumbClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()

    // Auto-scroll to end
    LaunchedEffect(breadcrumbs.size) {
        if (breadcrumbs.isNotEmpty()) {
            scrollState.animateScrollToItem(breadcrumbs.lastIndex)
        }
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        state = scrollState,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            itemsIndexed(breadcrumbs) { index, crumb ->
                val isLast = index == breadcrumbs.lastIndex
                AssistChip(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    enabled = !isLast,
                    onClick = { onCrumbClick(index) },
                    label = { Text(text = crumb, style = MaterialTheme.typography.labelMedium) },
                    trailingIcon = if (!isLast) {
                        {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    )
}

@Composable
fun BrowserBottomBar(
    isShuffle: Boolean,
    isLoop: Boolean,
    onShuffle: (Boolean) -> Unit,
    onLoop: (Boolean) -> Unit,
    onPlayAll: () -> Unit
) {
    BottomAppBar(
        actions = {
            IconToggleButton(
                checked = isShuffle,
                onCheckedChange = onShuffle,
                colors = IconButtonDefaults.iconToggleButtonColors(
                    checkedContentColor = MaterialTheme.colorScheme.primary
                ),
                content = { Icon(imageVector = Icons.Default.Shuffle, contentDescription = null) }
            )
            IconToggleButton(
                checked = isLoop,
                onCheckedChange = onLoop,
                colors = IconButtonDefaults.iconToggleButtonColors(
                    checkedContentColor = MaterialTheme.colorScheme.primary
                ),
                content = { Icon(imageVector = Icons.Default.Repeat, contentDescription = null) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onPlayAll,
                containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                content = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null) }
            )
        }
    )
}

@Composable
private fun ModuleListItem(file: ModuleFile, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = file.extension.uppercase(), color = MaterialTheme.colorScheme.primary)
                Text(text = file.sizeBytes.formatSize())
            }
        },
        leadingContent = {
            Icon(
                Icons.Default.AudioFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}
