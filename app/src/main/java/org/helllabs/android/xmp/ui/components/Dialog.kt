package org.helllabs.android.xmp.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.res.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.ui.theme.XmpTheme

private val maxDialogHeight = 256.dp

@Composable
fun SingleChoiceListDialog(
    isShowing: Boolean,
    icon: ImageVector,
    title: String,
    selectedIndex: Int,
    textList: ImmutableList<String>,
    subTextList: ImmutableList<String>? = null,
    confirmText: String = stringResource(id = android.R.string.ok),
    dismissText: String = stringResource(id = android.R.string.cancel),
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    onEmpty: () -> Unit
) {
    if (!isShowing) {
        return
    }

    if (textList.isEmpty()) {
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
                textList.forEachIndexed { index, item ->
                    RadioButtonItem(
                        index = index,
                        selection = selection,
                        text = item,
                        subText = subTextList?.let { it[index] },
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

@Composable
fun PermissionsRationaleDialog(
    description: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                content = { Text(text = stringResource(id = android.R.string.ok)) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        title = { Text(text = "Permissions Needed") },
        text = { Text(text = description) }
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
                textList = List(6) {
                    Playlist(name = "Playlist $it")
                }.map { it.name }.toImmutableList(),
                subTextList = List(6) {
                    Playlist(name = "Subtext $it")
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

@Preview
@Composable
fun Preview_PermissionsRationaleDialog() {
    XmpTheme {
        Box(modifier = Modifier.fillMaxWidth()) {
            PermissionsRationaleDialog(
                description = "Dialog Dialog Dialog",
                onDismiss = { },
                onConfirm = { },
            )
        }
    }
}
