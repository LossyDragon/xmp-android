package org.helllabs.android.xmp.compose.ui.playlist.components

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.compose.theme.XmpRoundedCorner
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.model.FileItem

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MenuCardItem(
    item: FileItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .clip(XmpRoundedCorner)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        leadingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        headlineContent = {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = item.comment.ifEmpty {
                    stringResource(id = R.string.error_no_comment)
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    )
}

@Preview
@Composable
private fun Preview() {
    XmpTheme(useDarkTheme = true) {
        MenuCardItem(
            item = FileItem(
                name = "Menu Card Item",
                comment = "Menu Card Comment\nMenu Card Comment\nMenu Card Comment",
                uri = Uri.EMPTY
            ),
            onClick = { },
            onLongClick = { }
        )
    }
}
