package com.lossydragon.media3.ui.screens.player.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SheetValue.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.*
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
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (currentIndex in queue.indices) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        dragHandle = null,
        content = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
                content = {
                    Text(
                        text = "Queue (${queue.size})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .align(Alignment.CenterEnd),
                        onClick = onDismiss,
                        content = {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        }
                    )
                }
            )
            HorizontalDivider()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = {
                    itemsIndexed(
                        items = queue,
                        key = { _, f -> f.uri.toString() },
                        itemContent = { index, file ->
                            QueueListItem(
                                isCurrentItem = index == currentIndex,
                                index = index,
                                file = file,
                                onItemClick = onItemClick,
                            )
                        }
                    )
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QueueListItem(
    isCurrentItem: Boolean,
    index: Int,
    file: ModuleFile,
    onItemClick: (Int) -> Unit
) {
    val background = if (isCurrentItem) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    ListItem(
        onClick = { onItemClick(index) },
        content = {
            Text(
                text = file.name,
                style = if (isCurrentItem) {
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            if (isCurrentItem) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        supportingContent = {
            Text(
                text = file.extension.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .background(background),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview() {
    val sheetState = rememberBottomSheetState(
        initialValue = Expanded,
        enabledValues = setOf(Hidden, Expanded),
    )
    XmpTheme {
        Scaffold { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            )
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
