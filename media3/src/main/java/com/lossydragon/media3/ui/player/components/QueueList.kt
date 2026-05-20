package com.lossydragon.media3.ui.player.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.core.net.toUri
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.ui.theme.XmpTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun QueueList(
    queue: ImmutableList<ModuleFile>,
    currentIndex: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Scroll to current item
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && currentIndex < queue.size) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = {
            itemsIndexed(queue, key = { _, f -> f.uri.toString() }) { index, file ->
                val isCurrent = index == currentIndex
                val background = if (isCurrent) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
                ListItem(
                    onClick = { onItemClick(index) },
                    content = {
                        Text(
                            text = file.name,
                            style = if (isCurrent) {
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
                        if (isCurrent) {
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
        }
    )
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        QueueList(
            queue = List(10) {
                ModuleFile(
                    uri = "content://preview/$it".toUri(),
                    name = "Item $it",
                    sizeBytes = 669L,
                    extension = "669"
                )
            }.toPersistentList(),
            currentIndex = 4,
            onItemClick = {},
        )
    }
}
