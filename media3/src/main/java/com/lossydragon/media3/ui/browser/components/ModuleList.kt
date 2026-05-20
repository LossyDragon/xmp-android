package com.lossydragon.media3.ui.browser.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.lossydragon.media3.model.BrowserUiState
import com.lossydragon.media3.model.FileItem
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.ui.theme.XmpTheme
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun ModuleList(
    state: BrowserUiState,
    padding: PaddingValues,
    listState: LazyListState,
    onDir: (FileItem) -> Unit,
    onSelect: (ModuleFile) -> Unit
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + 88.dp, // FAB
            start = padding.calculateStartPadding(LocalLayoutDirection.current),
            end = padding.calculateEndPadding(LocalLayoutDirection.current),
        ),
        modifier = Modifier.fillMaxSize(),
        content = {
            items(
                items = state.directories,
                key = { it.uri.toString() },
                itemContent = { dir ->
                    DirectoryListItem(item = dir, onClick = { onDir(dir) })
                }
            )
            items(
                items = state.files,
                key = { it.uri.toString() },
                itemContent = { file ->
                    ModuleListItem(
                        file = file,
                        onClick = { onSelect(file) },
                    )
                }
            )
        }
    )
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        ModuleList(
            state = BrowserUiState(
                files = List(10) {
                    ModuleFile(
                        uri = "$it".toUri(),
                        name = "Item $it",
                        sizeBytes = it + 1L * it,
                        extension = "669",
                    )
                }.toImmutableList(),
            ),
            padding = PaddingValues(),
            listState = LazyListState(),
            onDir = {},
            onSelect = {},
        )
    }
}
