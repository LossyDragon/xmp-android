package com.lossydragon.media3.ui.screens.playlists.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.lossydragon.media3.ui.theme.XmpTheme

@Composable
internal fun NewPlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
    onError: (String) -> Unit,
    initialName: String = "",
    initialComment: String = ""
) {
    val isEditing = initialName.isNotBlank()

    var name by remember { mutableStateOf(initialName) }
    var comment by remember { mutableStateOf(initialComment) }

    val dismiss: () -> Unit = {
        name = ""
        comment = ""
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = dismiss,
        icon = {
            Icon(
                imageVector = if (isEditing) {
                    Icons.Default.Edit
                } else {
                    Icons.AutoMirrored.Filled.PlaylistAdd
                },
                contentDescription = null,
            )
        },
        title = { Text(text = if (isEditing) "Edit Playlist" else "New Playlist") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = "Name") },
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(text = "Comment") },
                    maxLines = 6,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        onError("No name given for playlist.")
                    } else {
                        onCreate(name, comment)
                    }
                    dismiss()
                },
                content = { Text(text = if (isEditing) "Save" else "Create") }
            )
        },
        dismissButton = {
            TextButton(onClick = dismiss, content = { Text(text = "Cancel") })
        }
    )
}

@Preview
@Composable
private fun Preview_Edit() {
    XmpTheme {
        NewPlaylistDialog(
            onDismiss = {},
            onCreate = { _, _ -> },
            onError = {},
            initialName = "Name Name",
            initialComment = "Comment Comment"
        )
    }
}

@Preview
@Composable
private fun Preview_New() {
    XmpTheme {
        NewPlaylistDialog(
            onDismiss = {},
            onCreate = { _, _ -> },
            onError = {},
        )
    }
}
