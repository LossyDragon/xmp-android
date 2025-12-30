package org.helllabs.android.xmp.compose.components

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.model.FileItem
import org.helllabs.android.xmp.model.Playlist

private val maxDialogHeight = 256.dp

@Composable
fun SingleChoiceListDialog(
    isShowing: Boolean,
    icon: ImageVector,
    title: String,
    selectedIndex: Int,
    list: ImmutableList<String>,
    confirmText: String = stringResource(id = android.R.string.ok),
    dismissText: String = stringResource(id = android.R.string.cancel),
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    onEmpty: () -> Unit
) {
    if (!isShowing) {
        return
    }

    if (list.isEmpty()) {
        onEmpty()
        return
    }

    var selection by remember { mutableIntStateOf(selectedIndex) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = icon, contentDescription = null) },
        title = { Text(text = title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = maxDialogHeight)
                    .selectableGroup()
                    .verticalScroll(scrollState)
            ) {
                list.forEachIndexed { index, item ->
                    RadioButtonItem(
                        index = index,
                        selection = selection,
                        text = item,
                        onClick = { selection = index }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selection) }) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        }
    )
}

@Composable
fun MessageDialog(
    isShowing: Boolean,
    icon: ImageVector = Icons.Default.Error,
    title: String,
    text: String,
    confirmText: String,
    dismissText: String = stringResource(id = android.R.string.cancel),
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    if (!isShowing) {
        return
    }

    AlertDialog(
        modifier = Modifier.heightIn(max = maxDialogHeight.times(3)),
        onDismissRequest = onDismiss ?: onConfirm,
        icon = { Icon(imageVector = icon, contentDescription = null) },
        title = { Text(text = title) },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.verticalScroll(scrollState)
            ) {
                Text(text = text)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            onDismiss?.let {
                TextButton(onClick = it) {
                    Text(text = dismissText)
                }
            }
        }
    )
}

@Composable
fun NewPlaylistDialog(
    isShowing: Boolean,
    onConfirm: (newName: String, newComment: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isShowing) {
        return
    }

    var newName by remember { mutableStateOf("") }
    var newComment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(imageVector = Icons.Default.Edit, contentDescription = null)
        },
        title = {
            Text(text = stringResource(id = R.string.dialog_title_new_playlist))
        },
        text = {
            Column {
                Text(text = stringResource(id = R.string.dialog_message_new_playlist))
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    label = {
                        Text(text = "Name")
                    },
                    value = newName,
                    onValueChange = { newName = it }
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    label = {
                        Text(text = "Comment")
                    },
                    value = newComment,
                    onValueChange = { newComment = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = newName.isNotEmpty(),
                onClick = { onConfirm(newName, newComment) },
                content = {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )
}

@Composable
fun EditPlaylistDialog(
    isShowing: Boolean,
    fileItem: FileItem?,
    onConfirm: (FileItem, String, String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (FileItem) -> Unit
) {
    if (!isShowing || fileItem == null) {
        return
    }

    var newName by remember { mutableStateOf(fileItem.name) }
    var newComment by remember { mutableStateOf(fileItem.comment) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier
            .padding(28.dp)
            .wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        icon = {
            Icon(imageVector = Icons.Default.Edit, contentDescription = null)
        },
        title = {
            Text(text = "Edit Playlist")
        },
        text = {
            Column {
                Text(text = "Enter a new name or comment for ${fileItem.name}")
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = newName,
                    onValueChange = { newName = it }
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = newComment,
                    onValueChange = { newComment = it }
                )

                if (confirmDelete) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "Press delete again to delete playlist")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = newName.isNotEmpty(),
                onClick = {
                    onConfirm(fileItem, newName, newComment)
                },
                content = {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            )
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (!confirmDelete) {
                        confirmDelete = true
                        return@TextButton
                    }

                    onDelete(fileItem)
                },
                content = {
                    Text(text = stringResource(id = R.string.delete))
                }
            )
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )
}

@Composable
fun TextInputDialog(
    isShowing: Boolean,
    icon: ImageVector? = null,
    title: String,
    text: String? = null,
    defaultText: String,
    onConfirm: (value: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isShowing) {
        return
    }

    var value by remember { mutableStateOf(defaultText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            icon?.let {
                Icon(imageVector = it, contentDescription = null)
            }
        },
        title = {
            Text(text = title)
        },
        text = {
            Column {
                text?.let {
                    Text(text = it)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(value = value, onValueChange = { value = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotEmpty(),
                onClick = { onConfirm(value) },
                content = {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )
}

/**
 * Previews
 */

@Preview
@Composable
fun Preview_SingleChoiceListDialog() {
    XmpTheme(useDarkTheme = true) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SingleChoiceListDialog(
                isShowing = true,
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                title = stringResource(id = R.string.dialog_title_select_playlist),
                selectedIndex = 2,
                list = List(6) {
                    Playlist(name = "Playlist $it")
                }.map { it.name }.toImmutableList(),
                onConfirm = { },
                onDismiss = { },
                onEmpty = { }
            )
        }
    }
}

@Preview
@Composable
fun Preview_NewPlaylistDialog() {
    XmpTheme(useDarkTheme = true) {
        Box(modifier = Modifier.fillMaxWidth()) {
            NewPlaylistDialog(
                isShowing = true,
                onConfirm = { _, _ -> },
                onDismiss = { }
            )
        }
    }
}

@Preview
@Composable
fun Preview_MessageDialog() {
    XmpTheme(useDarkTheme = true) {
        Box(modifier = Modifier.fillMaxWidth()) {
            MessageDialog(
                isShowing = true,
                title = stringResource(id = R.string.error),
                text = stringResource(id = R.string.dialog_message_error_create_playlist),
                confirmText = stringResource(id = android.R.string.ok),
                onConfirm = { },
                onDismiss = { }
            )
        }
    }
}

@Preview
@Composable
fun Preview_LongMessageDialog() {
    val message = Array(20) { "Message Line $it\n" }
    XmpTheme(useDarkTheme = true) {
        Box(modifier = Modifier.fillMaxWidth()) {
            MessageDialog(
                isShowing = true,
                title = stringResource(id = R.string.error),
                text = message.joinToString(","),
                confirmText = stringResource(id = android.R.string.ok),
                onConfirm = { },
                onDismiss = { }
            )
        }
    }
}

@Preview
@Composable
fun Preview_EditPlaylistDialog() {
    XmpTheme(useDarkTheme = true) {
        Box(modifier = Modifier.fillMaxWidth()) {
            EditPlaylistDialog(
                isShowing = true,
                fileItem = FileItem(
                    name = "Playlist Name",
                    comment = "Playlist Comment",
                    uri = Uri.EMPTY
                ),
                onConfirm = { _, _, _ -> },
                onDismiss = {},
                onDelete = {}
            )
        }
    }
}

@Preview
@Composable
fun Preview_TextInputDialog() {
    XmpTheme(useDarkTheme = true) {
        Box(modifier = Modifier.fillMaxWidth()) {
            TextInputDialog(
                isShowing = true,
                icon = Icons.Default.Info,
                title = "Some Title",
                text = "Some Text",
                defaultText = "Default Text",
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}
