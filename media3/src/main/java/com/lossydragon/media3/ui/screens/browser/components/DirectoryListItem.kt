package com.lossydragon.media3.ui.screens.browser.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.*
import com.lossydragon.media3.model.FileItem
import com.lossydragon.media3.ui.theme.XmpTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DirectoryListItem(
    item: FileItem,
    onClick: () -> Unit
) {
    ListItem(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        content = { Text(item.name) },
        leadingContent = { Icon(imageVector = Icons.Default.Folder, contentDescription = null,) },
    )
}

@Preview
@Composable
private fun Preview() {
    XmpTheme {
        Surface {
            DirectoryListItem(
                item = FileItem(
                    name = "Directory Item",
                    uri = Uri.EMPTY,
                    isDirectory = true,
                    size = 0L,
                ),
                onClick = {},
            )
        }
    }
}
