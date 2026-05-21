package com.lossydragon.media3.ui.screens.downloads.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lossydragon.media3.model.Artist
import com.lossydragon.media3.model.ArtistInfo
import com.lossydragon.media3.model.Module
import com.lossydragon.media3.ui.components.GuruBox
import com.lossydragon.media3.ui.screens.downloads.components.ModuleListItem
import com.lossydragon.media3.ui.screens.downloads.viewmodel.DownloadHistoryViewModel
import com.lossydragon.media3.ui.theme.XmpTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun DownloadHistoryScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onModuleClick: (Int) -> Unit
) {
    val viewModel = koinViewModel<DownloadHistoryViewModel>()
    val history by viewModel.history.collectAsStateWithLifecycle()

    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null) },
            title = { Text(text = "Clear History") },
            text = { Text(text = "Clear your module search history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clear()
                        showClearDialog = false
                    },
                    content = { Text(text = "Clear") }
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false },
                    content = { Text(text = "Cancel") }
                )
            }
        )
    }

    DownloadHistoryContent(
        modifier = modifier,
        history = history,
        onBack = onBack,
        onModuleClick = onModuleClick,
        onShowDialog = { showClearDialog = it },
    )
}

@Composable
private fun DownloadHistoryContent(
    modifier: Modifier = Modifier,
    history: ImmutableList<Module>,
    onBack: () -> Unit,
    onModuleClick: (Int) -> Unit,
    onShowDialog: (Boolean) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "History") },
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
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(
                            onClick = { onShowDialog(true) },
                            content = {
                                Icon(
                                    imageVector = Icons.Default.ClearAll,
                                    contentDescription = null
                                )
                            }
                        )
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
            content = {
                if (history.isEmpty()) {
                    GuruBox(message = "No items stored in history ", onBack = onBack)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        content = {
                            items(
                                items = history,
                                itemContent = { module ->
                                    ModuleListItem(
                                        module = module,
                                        onClick = { onModuleClick(module.id) }
                                    )
                                }
                            )
                        }
                    )
                }
            }
        )
    }
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        DownloadHistoryContent(
            history = Array(10) {
                Module(
                    format = "MOD",
                    songtitle = "Song Title $it",
                    artistInfo = ArtistInfo(artist = listOf(Artist(alias = "Song Artist $it"))),
                    bytes = (it + 1) * 1234,
                )
            }.toPersistentList(),
            onBack = {},
            onModuleClick = {},
            onShowDialog = {},
        )
    }
}
