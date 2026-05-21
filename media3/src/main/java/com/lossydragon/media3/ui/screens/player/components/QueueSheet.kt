package com.lossydragon.media3.ui.screens.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.core.net.toUri
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.ui.theme.XmpTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QueueSheet(
    sheetState: SheetState,
    queue: ImmutableList<ModuleFile>,
    currentIndex: Int,
    onItemClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        content = {
            Text(
                text = "Queue",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            QueueList(
                queue = queue,
                currentIndex = currentIndex,
                onItemClick = onItemClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview() {
    val sheetState = SheetState(
        skipPartiallyExpanded = true,
        initialValue = SheetValue.Expanded,
        positionalThreshold = { 0.5f },
        velocityThreshold = { 125f },
    )
    XmpTheme {
        Scaffold { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize())

            QueueSheet(
                sheetState = sheetState,
                queue = List(10) {
                    ModuleFile(
                        uri = "content://preview/$it".toUri(),
                        name = "Item $it",
                        sizeBytes = 669L,
                        extension = "669"
                    )
                }.toPersistentList(),
                currentIndex = 3,
                onItemClick = {},
                onDismiss = {},
            )
        }
    }
}
