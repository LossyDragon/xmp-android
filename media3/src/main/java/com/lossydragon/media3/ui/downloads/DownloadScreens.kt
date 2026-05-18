package com.lossydragon.media3.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lossydragon.media3.model.DownloadStatus
import com.lossydragon.media3.model.Module
import com.lossydragon.media3.model.ModuleResult
import com.lossydragon.media3.model.SearchResult
import com.lossydragon.media3.model.SearchType
import com.lossydragon.media3.util.fromHtml

@Composable
fun DownloadSearchScreen(
    modifier: Modifier = Modifier,
    hasApiKey: Boolean,
    onSearch: (String, SearchType) -> Unit,
    onRandom: () -> Unit,
    onHistory: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(SearchType.TITLE) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Powered by The Mod Archive",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
        ) {
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                enabled = hasApiKey,
                value = query,
                onValueChange = { query = it },
                isError = query.isEmpty(),
                singleLine = true,
                label = { Text("Search") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotEmpty()) onSearch(query, type)
                    focusManager.clearFocus()
                }),
            )

            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SearchType.entries.forEachIndexed { index, searchType ->
                    SegmentedButton(
                        selected = type == searchType,
                        onClick = { type = searchType },
                        shape = SegmentedButtonDefaults.itemShape(index, SearchType.entries.size),
                        label = {
                            Text(searchType.name.lowercase().replaceFirstChar { it.uppercase() })
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = hasApiKey && query.isNotEmpty(),
                    onClick = {
                        onSearch(query, type)
                        focusManager.clearFocus()
                    },
                ) { Text("Search") }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = hasApiKey,
                    onClick = onRandom,
                ) { Text("Random") }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = hasApiKey,
                    onClick = onHistory,
                ) { Text("History") }
            }
        }
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
        if (state.module == null || (moduleId >= 0 && state.module!!.module.id != moduleId)) {
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

@Composable
fun DownloadHistoryScreen(
    modifier: Modifier = Modifier,
    history: List<Module>,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onModuleClick: (Int) -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("Clear your module search history?") },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    showClearDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.ClearAll, null)
                        }
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
            if (history.isEmpty()) {
                GuruBox(message = "No search history yet", onBack = onBack)
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(history.reversed()) { module ->
                        ModuleListItem(module = module, onClick = { onModuleClick(module.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleListItem(module: Module, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xff404040), RoundedCornerShape(2.dp))
                    .border(2.dp, Color(0xff808080), RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center,
                content = { Text(text = module.format, fontSize = 11.sp, color = Color.White) }
            )
        },
        headlineContent = {
            Text(
                text = module.songtitle.ifBlank { "(untitled)" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = module.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Text(text = "${module.sizeKb} KB", style = MaterialTheme.typography.labelSmall)
        },
    )
}

@Composable
private fun ArtistListItem(alias: String, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            headlineContent = { Text(text = alias, style = MaterialTheme.typography.bodyLarge) },
        )
    }
}

@Composable
private fun ModuleDetailLayout(
    modifier: Modifier = Modifier,
    moduleResult: ModuleResult
) {
    val module = moduleResult.module
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = module.songtitle.ifBlank { "(untitled)" }.fromHtml(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = module.filename.fromHtml(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${module.format} · ${module.artist.fromHtml()} · ${module.sizeKb} KB",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        HorizontalDivider()
        if (module.license.title.isNotBlank()) {
            SectionHeader(text = "License")
            Text(text = module.license.title, textAlign = TextAlign.Center)
            if (module.license.description.isNotBlank()) {
                Text(
                    text = module.license.description,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
        }
        if (module.comment.isNotBlank()) {
            SectionHeader(text = "Song Message")
            Text(
                text = module.formattedComment,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
            HorizontalDivider()
        }
        if (module.instruments.isNotBlank()) {
            SectionHeader(text = "Instruments")
            Text(
                text = module.formattedInstruments,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }
        if (moduleResult.hasSponsor) {
            HorizontalDivider()
            SectionHeader(text = "Sponsor")
            Text(text = moduleResult.sponsor.details.text, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text = text, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
}

@Composable
private fun GuruBox(message: String, onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Box(
                modifier = Modifier
                    .padding(32.dp)
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
                content = {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            )
            TextButton(onClick = onBack, content = { Text("Go Back") })
        }
    )
}
