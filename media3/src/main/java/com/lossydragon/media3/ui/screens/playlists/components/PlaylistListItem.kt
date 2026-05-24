package com.lossydragon.media3.ui.screens.playlists.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.lossydragon.media3.db.entity.PlaylistEntity
import com.lossydragon.media3.ui.theme.XmpTheme
import com.lossydragon.media3.util.toReadableDate

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlaylistListItem(
    item: PlaylistEntity,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        onClick = onSelect,
        shapes = ListItemDefaults.shapes(
            shape = MaterialTheme.shapes.small,
            focusedShape = MaterialTheme.shapes.small,
            pressedShape = MaterialTheme.shapes.small,
        ),
        content = { Text(text = item.name) },
        supportingContent = {
            Column {
                if (item.comment.isNotBlank()) Text(text = item.comment)
                Text(
                    text = "Created: ${item.createdAt.toReadableDate()}",
                    fontSize = 10.sp,
                )
            }
        },
        leadingContent = {
            Icon(imageVector = Icons.Default.AudioFile, contentDescription = null)
        },
        trailingContent = {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = "Delete") },
                        onClick = {
                            expanded = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = "Rename") },
                        onClick = {
                            expanded = false
                            onRename()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }
    )
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        PlaylistListItem(
            item = PlaylistEntity(
                name = "Name Name Name",
                comment = "Comment Comment"
            ),
            onSelect = {},
            onDelete = {},
            onRename = {},
        )
    }
}
