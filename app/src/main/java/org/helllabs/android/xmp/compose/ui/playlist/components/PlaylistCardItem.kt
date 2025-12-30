package org.helllabs.android.xmp.compose.ui.playlist.components

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import me.saket.cascade.CascadeDropdownMenu
import org.helllabs.android.xmp.compose.components.KoinPreview
import org.helllabs.android.xmp.compose.components.XmpDropdownMenuHeader
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.di.appModule
import org.helllabs.android.xmp.model.DropDownItem
import org.helllabs.android.xmp.model.DropDownSelection
import org.helllabs.android.xmp.model.PlaylistItem
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val playlistItemDropDownItems: List<DropDownItem> = listOf(
    DropDownItem("Add to play queue", DropDownSelection.ADD_TO_QUEUE),
    DropDownItem("Play all starting here", DropDownSelection.FILE_PLAY_HERE),
    DropDownItem("Play this module", DropDownSelection.FILE_PLAY_THIS_ONLY),
    DropDownItem("Remove from playlist", DropDownSelection.DELETE)
)

@Composable
fun PlaylistCardItem(
    scope: ReorderableCollectionItemScope,
    elevation: Dp,
    item: PlaylistItem,
    useFileName: Boolean,
    onItemClick: () -> Unit,
    onMenuClick: (DropDownSelection) -> Unit,
    onDragStopped: () -> Unit
) {
    var isContextMenuVisible by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        )
    ) {
        ListItem(
            modifier = Modifier
                .clickable { onItemClick() }
                .padding(6.dp),
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            ),
            leadingContent = {
                Icon(
                    modifier = with(scope) {
                        Modifier.draggableHandle(
                            onDragStarted = {
                                haptic.performHapticFeedback(HapticFeedbackType(25))
                            },
                            onDragStopped = {
                                haptic.performHapticFeedback(HapticFeedbackType(13))
                                onDragStopped()
                            }
                        )
                    },
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            headlineContent = {
                val storageManager = koinInject<StorageManager>()
                val text = if (useFileName) {
                    storageManager.getFileName(item.uri) ?: item.name
                } else {
                    item.name
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            supportingContent = {
                Text(
                    text = item.type,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isContextMenuVisible = true
                    },
                    content = {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                CascadeDropdownMenu(
                    expanded = isContextMenuVisible,
                    onDismissRequest = { isContextMenuVisible = false }
                ) {
                    XmpDropdownMenuHeader(text = "Edit Playlist")

                    playlistItemDropDownItems.forEach {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = it.text,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            onClick = {
                                haptic.performHapticFeedback(
                                    HapticFeedbackType.LongPress
                                )
                                onMenuClick(it.selection)
                                isContextMenuVisible = false
                            }
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
private fun Preview_PlaylistCardItem() {
    KoinPreview(modules = listOf(appModule)) {
        val isDragging by remember {
            mutableStateOf(false)
        }
        val elevation by animateDpAsState(
            targetValue = if (isDragging) 4.dp else 1.dp,
            label = "isDragging dp"
        )

        val lazyListState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(lazyListState) { _, _ ->
        }

        Surface {
            LazyColumn {
                item {
                    ReorderableItem(reorderableState, key = {}) {
                        PlaylistCardItem(
                            scope = this,
                            elevation = elevation,
                            item = PlaylistItem(
                                name = "Playlist title",
                                type = "Playlist comment",
                                uri = Uri.EMPTY
                            ),
                            useFileName = false,
                            onItemClick = { },
                            onMenuClick = { },
                            onDragStopped = { }
                        )
                    }
                }
            }
        }
    }
}
