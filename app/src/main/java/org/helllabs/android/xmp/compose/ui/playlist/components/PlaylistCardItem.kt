package org.helllabs.android.xmp.compose.ui.playlist.components

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.saket.cascade.CascadeDropdownMenu
import org.helllabs.android.xmp.compose.components.XmpDropdownMenuHeader
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.core.StorageManager
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

    Surface(shadowElevation = elevation) {
        Card {
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick() },
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
                        contentDescription = null
                    )
                },
                headlineContent = {
                    val storageManager = koinInject<StorageManager>()
                    val text = if (useFileName) {
                        storageManager.getFileName(item.uri) ?: item.name
                    } else {
                        item.name
                    }
                    Text(text = text)
                },
                supportingContent = {
                    Text(text = item.type)
                },
                trailingContent = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isContextMenuVisible = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null
                        )
                    }

                    CascadeDropdownMenu(
                        expanded = isContextMenuVisible,
                        onDismissRequest = { isContextMenuVisible = false }
                    ) {
                        XmpDropdownMenuHeader(
                            text = "Edit Playlist"
                        )

                        playlistItemDropDownItems.forEach {
                            DropdownMenuItem(
                                text = { Text(text = it.text) },
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
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
private fun Preview_PlaylistCardItem() {
    XmpTheme(useDarkTheme = true) {
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
