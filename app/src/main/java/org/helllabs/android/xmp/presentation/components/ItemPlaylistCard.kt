package org.helllabs.android.xmp.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.model.PlaylistItem.Companion.TYPE_PLAYLIST
import org.helllabs.android.xmp.model.PlaylistItem.Companion.TYPE_SPECIAL
import org.helllabs.android.xmp.presentation.theme.AppTheme

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun ItemPlaylistCard(
    playlist: PlaylistItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {

    val cardIcon = when (playlist.type) {
        TYPE_SPECIAL -> Icons.Outlined.FolderOpen
        TYPE_PLAYLIST -> Icons.Default.List
        else -> throw IllegalArgumentException("Card should only use Type Special or Playlist!")
    }

    CardClickable(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth(),
        elevation = 4.dp,
        onClick = { onClick() },
        onLongClick = { onLongClick() }
    ) {
        Row(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterVertically),
                imageVector = cardIcon,
                contentDescription = null
            )
            Column {
                Text(
                    text = playlist.name,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playlist.comment.orEmpty(),
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/************
 * Previews *
 ************/

@Preview
@Composable
private fun ItemPlaylistCardPreview() {
    AppTheme(false) {
        ItemPlaylistCard(
            playlist = PlaylistItem(
                type = TYPE_SPECIAL,
                name = "Some very long playlist name that should ellipsize at the end",
                comment = stringResource(id = R.string.app_description)
            ),
            onClick = {},
            onLongClick = {}
        )
    }
}

@Preview
@Composable
private fun ItemPlaylistCardPreviewDark() {
    AppTheme(true) {
        ItemPlaylistCard(
            playlist = PlaylistItem(
                type = TYPE_PLAYLIST,
                name = "Some very long playlist name that should ellipsize at the end",
                comment = stringResource(id = R.string.app_name)
            ),
            onClick = {},
            onLongClick = {}
        )
    }
}
