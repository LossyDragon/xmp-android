package com.lossydragon.media3.ui.downloads

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lossydragon.media3.model.DownloadStatus
import com.lossydragon.media3.model.Module
import com.lossydragon.media3.model.SearchResult
import com.lossydragon.media3.model.SearchType
import com.lossydragon.media3.ui.downloads.components.ArtistListItem
import com.lossydragon.media3.ui.downloads.components.GuruBox
import com.lossydragon.media3.ui.downloads.components.ModuleListItem
import com.lossydragon.media3.ui.theme.XmpTheme
import com.lossydragon.media3.ui.theme.topazFontFamily
import com.lossydragon.media3.ui.util.annotatedLinkString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DownloadSearchScreen(
    modifier: Modifier = Modifier,
    hasApiKey: Boolean,
    snackbarHostState: SnackbarHostState,
    onSearch: (String, SearchType) -> Unit,
    onRandom: () -> Unit,
    onHistory: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(SearchType.TITLE) }
    var hasInteracted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500L) // Chill
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Search") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // TODO
                        },
                        content = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        if (!hasApiKey) {
                            snackbarHostState.showSnackbar(
                                message = "No API key used to search."
                            )
                        } else if (query.length < 3) {
                            snackbarHostState.showSnackbar(
                                message = "At least 3 characters required to search"
                            )
                        } else {
                            onSearch(query, type)
                            focusManager.clearFocus()
                        }
                    }
                },
                text = { Text(text = "Search") },
                icon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) }
            )
        },
        bottomBar = {
            ShortNavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
                content = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                        content = {
                            Text(
                                text = annotatedLinkString(
                                    text = "Powered by The Mod Archive",
                                    url = "https://modarchive.org/"
                                ),
                            )
                        }
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            content = {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    enabled = hasApiKey,
                    value = query,
                    onValueChange = {
                        query = it
                        hasInteracted = true
                    },
                    isError = hasInteracted && query.length < 3,
                    singleLine = true,
                    label = { Text(text = "Search") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.length >= 3) {
                                onSearch(query, type)
                                focusManager.clearFocus()
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "At least 3 characters required to search"
                                    )
                                }
                            }
                        }
                    ),
                )

                Spacer(modifier = Modifier.height(24.dp))

                ButtonGroup(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    expandedRatio = .20f,
                    overflowIndicator = {},
                    content = {
                        toggleableItem(
                            checked = type == SearchType.TITLE,
                            label = "Title or Filename",
                            onCheckedChange = { type = SearchType.TITLE },
                            weight = 1f,
                        )
                        toggleableItem(
                            checked = type == SearchType.ARTIST,
                            label = "Artist",
                            onCheckedChange = { type = SearchType.ARTIST },
                            weight = 1f,
                        )
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                ButtonGroup(
                    overflowIndicator = {},
                    content = {
                        customItem(
                            buttonGroupContent = {
                                OutlinedButton(
                                    onClick = onRandom,
                                    enabled = hasApiKey,
                                    modifier = Modifier.weight(1f),
                                    content = {
                                        Icon(
                                            imageVector = Icons.Default.Shuffle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Random")
                                    }
                                )
                            },
                            menuContent = {}
                        )
                        customItem(
                            buttonGroupContent = {
                                OutlinedButton(
                                    onClick = onHistory,
                                    content = {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "History")
                                    }
                                )
                            },
                            menuContent = {}
                        )
                    }
                )
            }
        )
    }
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        DownloadSearchScreen(
            hasApiKey = true,
            snackbarHostState = remember { SnackbarHostState() },
            onSearch = { _, _ -> },
            onRandom = {},
            onHistory = {},
        )
    }
}

@Composable
fun DownloadResultScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadViewModel,
    searchType: SearchType,
    query: String,
    onBack: () -> Unit,
    onModuleClick: (Int) -> Unit,
    onArtistClick: (Int) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (searchType == SearchType.ARTIST) {
            viewModel.searchArtist(query)
        } else {
            viewModel.searchFileOrTitle(query)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.title.ifBlank {
                            "Results"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isLoading) CircularProgressIndicator()

            state.error?.let {
                GuruBox(message = it, onBack = onBack)
            }

            if (!state.isLoading && state.error == null) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    when (val result = state.result) {
                        is SearchResult.Modules -> items(result.data.module) { module ->
                            ModuleListItem(module = module, onClick = { onModuleClick(module.id) })
                        }

                        is SearchResult.Artists -> items(result.data.listItems) { artist ->
                            ArtistListItem(alias = artist.alias, onClick = {
                                onArtistClick(artist.id)
                            })
                        }

                        null -> Unit
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadModuleScreen(
    modifier: Modifier = Modifier,
    viewModel: ModuleResultViewModel,
    moduleId: Int,
    onBack: () -> Unit,
    onPlay: (Module) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Only fetch if we don't already have a module loaded
        // or if the requested ID differs from what's currently loaded
        if (state.module == null || (moduleId >= 0 && state.module?.module?.id != moduleId)) {
            viewModel.getModuleById(moduleId)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Delete Module") },
            text = { Text("Delete ${state.module?.module?.filename}?") },
            confirmButton = {
                TextButton(onClick = {
                    state.module?.module?.let { viewModel.deleteModule(it) }
                    showDeleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isRandom) "Random Module" else "Module Details",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    if (state.moduleExists) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, null)
                        }
                    }
                    state.module?.module?.infopage?.let { url ->
                        if (url.isNotBlank()) {
                            IconButton(onClick = { /* share */ }) {
                                Icon(Icons.Default.Share, null)
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Progress bar — only visible while downloading
                    when (val status = state.downloadStatus) {
                        is DownloadStatus.Progress -> LinearProgressIndicator(
                            progress = { status.percent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        is DownloadStatus.Loading -> LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )

                        else -> Spacer(Modifier.height(4.dp)) // keep layout stable
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val module = state.module?.module
                        val isDownloading = state.downloadStatus is DownloadStatus.Loading ||
                            state.downloadStatus is DownloadStatus.Progress
                        val buttonLabel = when {
                            state.isLoading -> "Loading..."

                            isDownloading -> when (val s = state.downloadStatus) {
                                is DownloadStatus.Progress -> "%.0f%%".format(s.percent)
                                else -> "Downloading..."
                            }

                            state.moduleExists -> "Play"

                            module?.isSupported == false -> "Unsupported"

                            else -> "Download"
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            enabled =
                                !state.isLoading && !isDownloading && module?.isSupported != false,
                            onClick = {
                                if (state.moduleExists) {
                                    module?.let { onPlay(it) }
                                } else {
                                    module?.let { viewModel.downloadModule(it) }
                                }
                            }
                        ) { Text(buttonLabel) }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = !state.isLoading && !isDownloading,
                            onClick = { viewModel.getRandomModule() },
                        ) { Text("Random") }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isLoading) CircularProgressIndicator()

            state.softError?.let {
                GuruBox(message = it, onBack = onBack)
            }

            state.module?.let { result ->
                ModuleDetailLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    moduleResult = result,
                )
            }
        }
    }
}
