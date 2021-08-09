package org.helllabs.android.xmp.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.model.PlaylistType
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.theme.XmpTheme
import org.helllabs.android.xmp.util.ifNullOrEmpty

@Composable
fun ItemPlaylistCard(
    playlist: PlaylistItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    val cardIcon = when (playlist.type) {
        PlaylistType.TYPE_SPECIAL -> Icons.Outlined.FolderOpen
        PlaylistType.TYPE_PLAYLIST -> Icons.Default.List
        else -> throw IllegalArgumentException("Card should only use Type Special or Playlist!")
    }

    // Lazy sanity check
    if (playlist.type == PlaylistType.TYPE_SPECIAL && playlist.name == null) {
        val mediaPath = PrefManager.mediaPath ?: "..."
        playlist.name = stringResource(id = R.string.playlist_special_title)
        playlist.comment = stringResource(id = R.string.playlist_special_comment, mediaPath)
    }

    CardClickable(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth(),
        elevation = 4.dp,
        onClick = { onClick() },
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick()
        }
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
                    text = playlist.name!!,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playlist.comment.ifNullOrEmpty {
                        stringResource(id = R.string.no_comment)
                    },
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

@Preview(name = "Dark Theme Card", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ItemPlaylistCardPreview() {
    XmpTheme(false) {
        ItemPlaylistCard(
            playlist = PlaylistItem(
                type = PlaylistType.TYPE_PLAYLIST,
                name = "Some very long playlist name that should ellipsize at the end",
                comment = stringResource(id = R.string.app_description)
            ),
            onClick = {},
            onLongClick = {}
        )
    }
}
