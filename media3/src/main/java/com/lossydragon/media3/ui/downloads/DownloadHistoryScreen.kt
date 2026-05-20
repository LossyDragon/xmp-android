package com.lossydragon.media3.ui.downloads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lossydragon.media3.model.Artist
import com.lossydragon.media3.model.ArtistInfo
import com.lossydragon.media3.model.Module
import com.lossydragon.media3.ui.downloads.components.GuruBox
import com.lossydragon.media3.ui.downloads.components.ModuleListItem
import com.lossydragon.media3.ui.theme.XmpTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet

@Composable
internal fun DownloadHistoryScreen(
    modifier: Modifier = Modifier,
    history: ImmutableList<Module>,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onModuleClick: (Int) -> Unit
) {
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
                        onClear()
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
                            onClick = { showClearDialog = true },
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
                    GuruBox(message = "No search history yet", onBack = onBack)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        content = {
                            items(history.reversed()) { module ->
                                ModuleListItem(
                                    module = module,
                                    onClick = { onModuleClick(module.id) }
                                )
                            }
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
        DownloadHistoryScreen(
            history = Array(10) {
                Module(
                    format = "MOD",
                    songtitle = "Song Title $it",
                    artistInfo = ArtistInfo(artist = listOf(Artist(alias = "Song Artist $it"))),
                    bytes = (it + 1) * 1234,
                )
            }.toPersistentList(),
            onBack = {},
            onClear = {},
            onModuleClick = {},
        )
    }
}
