package org.helllabs.android.xmp.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.presentation.theme.AppTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemList(
    item: PlaylistItem,
    isDraggable: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    ConstraintLayout(
        modifier = Modifier
            .height(72.dp)
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        val (icon, content, drag) = createRefs()
        val listIcon = when (item.type) {
            PlaylistItem.TYPE_DIRECTORY -> Icons.Outlined.FolderOpen
            PlaylistItem.TYPE_FILE -> Icons.Default.InsertDriveFile
            else -> throw IllegalArgumentException("Item should only use Type Directory or File!")
        }

        Icon(
            modifier = Modifier.constrainAs(icon) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start, 16.dp)
            },
            imageVector = listIcon,
            contentDescription = null
        )
        Column(
            modifier = Modifier.constrainAs(content) {
                width = Dimension.fillToConstraints
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(icon.end, 16.dp)
                end.linkTo(if (isDraggable) drag.start else parent.end, 16.dp)
            }
        ) {
            Text(
                text = item.name,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when {
                    item.isDirectory -> stringResource(id = R.string.directory)
                    !item.isPlayable -> stringResource(id = R.string.unplayable_item)
                    else -> item.comment
                }.orEmpty(),
                fontStyle = if (item.isDirectory) FontStyle.Italic else FontStyle.Normal,
                color = if (!item.isPlayable && !item.isDirectory) Color.Red else Color.Unspecified,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isDraggable) {
            Icon(
                modifier = Modifier.constrainAs(drag) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end, 16.dp)
                },
                imageVector = Icons.Default.DragHandle,
                contentDescription = null
            )
        }
    }
}

/************
 * Previews *
 ************/

@Preview
@Composable
private fun ItemListPreview() {
    AppTheme(false) {
        ItemList(
            item = PlaylistItem(
                PlaylistItem.TYPE_FILE,
                "Some Item Some Item Some Item Some Item Some Type",
                "Some Type Some Type Some Type Some Type Some Type"
            ),
            isDraggable = true,
            onClick = {},
            onLongClick = {},
        )
    }
}

@Preview
@Composable
private fun ItemListPreviewDark() {
    AppTheme(true) {
        ItemList(
            item = PlaylistItem(
                PlaylistItem.TYPE_DIRECTORY,
                "Some Item Some Item Some Item Some Item",
                "Some Type Some Type Some Type Some Type Some Type"
            ),
            isDraggable = false,
            onClick = {},
            onLongClick = {},
        )
    }
}
