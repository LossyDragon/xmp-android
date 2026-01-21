package org.helllabs.android.xmp.ui.screens.playlist.components

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
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
import kotlinx.collections.immutable.persistentListOf
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.DropDownItem
import org.helllabs.android.xmp.model.DropDownSelection
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.ui.components.KoinPreview
import org.helllabs.android.xmp.ui.theme.XmpRoundedCorner
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableColumn

private val playlistItemDropDownItems: List<DropDownItem> = listOf(
    DropDownItem("Add to play queue", DropDownSelection.ADD_TO_QUEUE),
    DropDownItem("Play all starting here", DropDownSelection.FILE_PLAY_HERE),
    DropDownItem("Play this module", DropDownSelection.FILE_PLAY_THIS_ONLY),
    DropDownItem("Remove from playlist", DropDownSelection.DELETE)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaylistCardItem(
    @SuppressLint("ModifierParameter") iconModifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource,
    item: PlaylistItem,
    isDragging: Boolean,
    useFileName: Boolean,
    onItemClick: () -> Unit,
    onMenuClick: (DropDownSelection) -> Unit
) {
    var isContextMenuVisible by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)

    Surface(shadowElevation = elevation, color = Color.Transparent) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            interactionSource = interactionSource,
            shape = XmpRoundedCorner,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            onClick = onItemClick,
        ) {
            ListItem(
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                ),
                leadingContent = {
                    IconButton(
                        modifier = iconModifier,
                        interactionSource = interactionSource,
                        onClick = {},
                        content = {
                            Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder")
                        }
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

                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                    ) {
                        DropdownMenu(
                            expanded = isContextMenuVisible,
                            onDismissRequest = { isContextMenuVisible = false },
                            shape = RoundedCornerShape(16.dp),
                            content = {
                                DropdownMenuGroup(
                                    shapes = MenuDefaults.groupShape(0, 1),
                                    content = {
                                        DropdownMenuItem(
                                            enabled = false,
                                            text = { Text(text = "Edit Playlist") },
                                            onClick = { }
                                        )
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
                                )
                            }
                        )
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
    KoinPreview {
        Surface {
            ReorderableColumn(
                list = persistentListOf(
                    PlaylistItem(
                        name = "Playlist title",
                        type = "Playlist comment",
                        uri = Uri.EMPTY
                    )
                ),
                onSettle = { _, _ -> }
            ) { _, item, isDragging ->
                key(item.id) {
                    ReorderableItem {
                        val interactionSource = remember { MutableInteractionSource() }
                        PlaylistCardItem(
                            iconModifier = Modifier,
                            interactionSource = interactionSource,
                            item = item,
                            isDragging = isDragging,
                            useFileName = false,
                            onItemClick = { },
                            onMenuClick = { },
                        )
                    }
                }
            }
        }
    }
}
