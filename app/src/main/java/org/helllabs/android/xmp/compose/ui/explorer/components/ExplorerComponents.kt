package org.helllabs.android.xmp.compose.ui.explorer.components

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import me.saket.cascade.CascadeDropdownMenu
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.components.KoinPreview
import org.helllabs.android.xmp.compose.components.XmpDropdownMenuHeader
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.explorer.BreadCrumb
import org.helllabs.android.xmp.model.DropDownItem
import org.helllabs.android.xmp.model.DropDownSelection
import org.helllabs.android.xmp.model.FileItem

private val crumbDropDownItems = listOf(
    DropDownItem("Add to play queue", DropDownSelection.ADD_TO_QUEUE),
    DropDownItem("Add to playlist", DropDownSelection.ADD_TO_PLAYLIST),
    DropDownItem("Play contents", DropDownSelection.DIR_PLAY_CONTENTS)
)

private val directoryDropDownItems = listOf(
    DropDownItem("Add to play queue", DropDownSelection.ADD_TO_QUEUE),
    DropDownItem("Add to playlist", DropDownSelection.ADD_TO_PLAYLIST),
    DropDownItem("Play contents", DropDownSelection.DIR_PLAY_CONTENTS),
    DropDownItem("Delete directory", DropDownSelection.DELETE)
)

private val fileDropDownItems: List<DropDownItem> = listOf(
    DropDownItem("Add to play queue", DropDownSelection.ADD_TO_QUEUE),
    DropDownItem("Add to playlist", DropDownSelection.ADD_TO_PLAYLIST),
    DropDownItem("Play all starting here", DropDownSelection.FILE_PLAY_HERE),
    DropDownItem("Play this file", DropDownSelection.FILE_PLAY_THIS_ONLY),
    DropDownItem("Delete file", DropDownSelection.DELETE),
)

@Composable
fun ExplorerListCard(
    item: FileItem,
    onItemClick: () -> Unit,
    onItemLongClick: (DropDownSelection) -> Unit
) {
    var isContextMenuVisible by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onItemClick
    ) {
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth(),
            leadingContent = {
                val icon = if (!item.isDirectory) {
                    Icons.AutoMirrored.Filled.InsertDriveFile
                } else {
                    Icons.Default.Folder
                }

                Icon(imageVector = icon, contentDescription = null)
            },
            headlineContent = {
                Text(text = item.name)
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

                val list = if (!item.isDirectory) fileDropDownItems else directoryDropDownItems
                CascadeDropdownMenu(
                    expanded = isContextMenuVisible,
                    onDismissRequest = { isContextMenuVisible = false }
                ) {
                    XmpDropdownMenuHeader(
                        text = if (!item.isDirectory) "This File" else "This Directory"
                    )

                    list.forEach {
                        DropdownMenuItem(
                            text = { Text(text = it.text) },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onItemLongClick(it.selection)
                                isContextMenuVisible = false
                            }
                        )
                    }
                }
            },
            supportingContent = {
                Text(
                    text = if (item.isDirectory) {
                        stringResource(id = R.string.directory)
                    } else {
                        item.comment
                    },
                    fontStyle = FontStyle.Italic
                )
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BreadCrumbs(
    modifier: Modifier = Modifier,
    crumbScrollState: LazyListState,
    crumbs: ImmutableList<BreadCrumb>,
    onCrumbMenu: (DropDownSelection) -> Unit,
    onCrumbClick: (BreadCrumb, Int) -> Unit
) {
    Row(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            state = crumbScrollState,
            contentPadding = PaddingValues(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stickyHeader {
                var isContextMenuVisible by rememberSaveable { mutableStateOf(false) }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(
                        topEnd = 16.dp,
                        bottomEnd = 16.dp
                    )
                ) {
                    val haptic = LocalHapticFeedback.current
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isContextMenuVisible = true
                        }
                    ) {
                        Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = null)

                        CascadeDropdownMenu(
                            expanded = isContextMenuVisible,
                            onDismissRequest = { isContextMenuVisible = false }
                        ) {
                            XmpDropdownMenuHeader(text = "All Files")
                            crumbDropDownItems.forEach {
                                DropdownMenuItem(
                                    text = { Text(text = it.text) },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onCrumbMenu(it.selection)
                                        isContextMenuVisible = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            itemsIndexed(crumbs) { index, item ->
                BreadCrumbChip(
                    enabled = item.enabled,
                    onClick = { onCrumbClick(item, index) },
                    label = item.name
                )
            }
        }
    }
}

@Composable
private fun BreadCrumbChip(
    enabled: Boolean,
    onClick: () -> Unit,
    label: String
) {
    AssistChip(
        modifier = Modifier.padding(horizontal = 3.dp),
        enabled = enabled,
        onClick = onClick,
        label = { Text(text = label) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    )
}

@Preview
@Composable
private fun Preview_ExplorerListCard() {
    KoinPreview {
        ExplorerListCard(
            item = FileItem(
                name = "File List Card",
                comment = "File List Comment",
                uri = Uri.EMPTY,
            ),
            onItemClick = { },
            onItemLongClick = { }
        )
    }
}

@Preview
@Composable
private fun Preview_BreadCrumbs() {
    XmpTheme(useDarkTheme = true) {
        val state = rememberLazyListState()
        BreadCrumbs(
            crumbScrollState = state,
            crumbs = listOf(
                BreadCrumb("Bread Crumb 1", null),
                BreadCrumb("Bread Crumb 2", null),
                BreadCrumb("Bread Crumb 3", null)
            ).toPersistentList(),
            onCrumbMenu = { },
            onCrumbClick = { _, _ -> }
        )
    }
}

@Preview
@Composable
private fun Preview_BreadCrumbChip() {
    XmpTheme(useDarkTheme = true) {
        BreadCrumbChip(enabled = true, onClick = { }, label = "Bread Crumb Chip")
    }
}
