package org.helllabs.android.xmp.ui.playlists

import TextFieldStyle
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.message
import com.vanpra.composematerialdialogs.title
import input
import org.helllabs.android.xmp.ui.components.AnimatingFabContent
import org.helllabs.android.xmp.ui.components.TwoLineItem
import org.helllabs.android.xmp.ui.theme.XmpAndroidTheme

@Composable
fun PlaylistsFab(
    extended: Boolean,
    onFabClicked: () -> Unit,
) {
    FloatingActionButton(
        modifier = Modifier
            .height(56.dp)
            .widthIn(min = 56.dp),
        onClick = onFabClicked,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = Color.White
    ) {
        AnimatingFabContent(
            extended = extended,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null
                )
            },
            text = { Text(text = "New Playlist") },
        )
    }
}

@Composable
fun PlaylistItemCard(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.List,
    primaryText: String,
    secondaryText: String,
    onClick: () -> Unit,
    onOverflowClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    TwoLineItem(
        modifier = modifier,
        primaryText = primaryText,
        secondaryText = secondaryText,
        cardIcon = icon,
        onItemClick = {
            onClick()
        },
        onOverflow = {
            /*TODO*/
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onOverflowClick()
        }
    )
}

@Composable
fun NewPlaylistDialog(
    isShowing: MaterialDialogState,
    onConfirm: (name: String, comment: String) -> Unit,
) {
    var nameInput by remember { mutableStateOf("") }
    var commentInput by remember { mutableStateOf("") }

    MaterialDialog(
        dialogState = isShowing,
        buttons = {
            positiveButton("Confirm") {
                onConfirm(nameInput, commentInput)
            }
            negativeButton("Cancel")
        }
    ) {
        title(text = "New Playlist")
        input(
            label = "Name",
            textFieldStyle = TextFieldStyle.Outlined,
            singleLine = true,
            isTextValid = { it.isNotEmpty() }
        ) { inputString ->
            nameInput = inputString
        }
        input(
            label = "Comment",
            textFieldStyle = TextFieldStyle.Outlined,
            singleLine = false,
            maxLines = 2
        ) { inputString ->
            commentInput = inputString
        }
    }
}

@Composable
fun EditPlaylistDialog(
    oldName: String,
    oldComment: String,
    isShowing: MaterialDialogState,
    onConfirm: (name: String, comment: String) -> Unit,
    onDelete: (name: String) -> Unit,
) {
    var nameInput by remember { mutableStateOf("") }
    var commentInput by remember { mutableStateOf("") }

    MaterialDialog(
        dialogState = isShowing,
        buttons = {
            positiveButton("Confirm") {
                onConfirm(nameInput, commentInput)
            }
            negativeButton("Cancel")
            accessibilityButton(
                icon = Icons.Default.Delete,
                onClick = {
                    isShowing.hide()
                    onDelete(oldName)
                }
            )
        }
    ) {
        title(text = "Edit Playlist")
        input(
            label = "Name",
            prefill = oldName,
            textFieldStyle = TextFieldStyle.Outlined,
            singleLine = true,
            isTextValid = { it.isNotEmpty() }
        ) { inputString ->
            nameInput = inputString
        }
        input(
            label = "Comment",
            prefill = oldComment,
            textFieldStyle = TextFieldStyle.Outlined,
            singleLine = false,
            maxLines = 2
        ) { inputString ->
            commentInput = inputString
        }
    }
}

@Composable
fun DeletePlaylistDialog(
    isShowing: MaterialDialogState,
    name: String,
    onDelete: () -> Unit
) {
    MaterialDialog(
        dialogState = isShowing,
        buttons = {
            positiveButton("Confirm") {
                onDelete()
            }
            negativeButton("Cancel")
        }
    ) {
        title("Delete Playlist")
        message("Are you sure you want to delete playlist: $name?\nThis cannot be undone!")
    }
}

@Preview(name = "Playlist Fab Button")
@Composable
private fun PlaylistFab_Preview() {
    XmpAndroidTheme {
        PlaylistsFab(extended = true, onFabClicked = {})
    }
}

@Preview(name = "Playlist Item Card")
@Composable
private fun PlaylistItemCard_Preview() {
    XmpAndroidTheme {
        PlaylistItemCard(
            primaryText = "Some Text",
            secondaryText = "Some more text!!",
            onClick = {},
            onOverflowClick = {}
        )
    }
}
