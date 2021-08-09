package org.helllabs.android.xmp.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.model.PlaylistType
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.theme.XmpTheme
import org.helllabs.android.xmp.util.ifNullOrEmpty

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
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

    if (playlist.type == PlaylistType.TYPE_PLAYLIST) {
        playlist.comment = playlist.comment.ifNullOrEmpty {
            stringResource(id = R.string.no_comment)
        }
    }

    Card(
        modifier = Modifier
            .padding(6.dp),
        shape = MaterialTheme.shapes.large,
        elevation = 4.dp,
    ) {
        ListItem(
            modifier = Modifier
                .combinedClickable(
                    onClick = { onClick() },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                ),
            text = {
                Text(
                    text = playlist.name!!,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            secondaryText = {
                Text(
                    modifier = Modifier.padding(bottom = 10.dp),
                    text = playlist.comment!!,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            icon = {
                Icon(
                    modifier = Modifier.padding(top = 8.dp, start = 8.dp),
                    imageVector = cardIcon,
                    contentDescription = null
                )
            }
        )
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme Card", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
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
