package org.helllabs.android.xmp.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.*
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

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun ItemList(
    item: PlaylistItem,
    isDraggable: Boolean = false,
    onDrag: ((value: Boolean) -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val listIcon = when (item.type) {
        PlaylistType.TYPE_DIRECTORY -> Icons.Outlined.FolderOpen
        PlaylistType.TYPE_FILE -> Icons.Default.InsertDriveFile
        else -> throw IllegalArgumentException("Item should only use Type Directory or File!")
    }

    ListItem(
        modifier = Modifier
            .height(72.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            ),
        icon = {
            Icon(
                modifier = Modifier.padding(top = 8.dp, start = 8.dp),
                imageVector = listIcon,
                contentDescription = null
            )
        },
        text = {
            Text(
                text = item.name!!,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryText = {
            Text(
                text = when {
                    item.isDirectory() -> stringResource(id = R.string.directory)
                    !item.isPlayable -> stringResource(id = R.string.unplayable_item)
                    else -> item.comment
                }.orEmpty(),
                fontStyle = if (item.isDirectory()) FontStyle.Italic else FontStyle.Normal,
                color =
                if (!item.isPlayable && !item.isDirectory()) Color.Red
                else Color.Unspecified,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailing = {
            // TODO: Research way to highlight on drag.
            if (isDraggable) {
                assert(onDrag != null) { "onDrag should not be null while draggable!" }
                Icon(
                    modifier = Modifier
                        .size(32.dp)
                        .pointerInteropFilter { event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> onDrag!!.invoke(true)
                                else -> onDrag!!.invoke(false)
                            }
                            true // Continue to consume the touch event.
                        },
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = null
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemBreadCrumb(
    crumb: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Card(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .padding(4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            ),
        elevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(12.dp),
                imageVector = Icons.Default.FolderOpen,
                contentDescription = "Playlist Icon"
            )
            Text(
                modifier = Modifier.padding(start = 2.dp),
                text = crumb,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ItemModule(
    item: Module,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        icon = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color(0xff404040),
                        RoundedCornerShape(2.dp)
                    )
                    .clip(RoundedCornerShape(2.dp))
                    .border(2.dp, Color(0xff808080)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    modifier = Modifier,
                    text = item.format!!,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        },
        text = {
            Text(
                text = item.getSongTitle().toString(),
                maxLines = 1,
                fontSize = 18.sp,
                overflow = TextOverflow.Ellipsis
            )
        },
        singleLineSecondaryText = true,
        secondaryText = {
            Text(
                text = item.getArtist(),
                maxLines = 1,
                fontSize = 14.sp,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailing = {
            Text(
                text = stringResource(id = R.string.size_kb, item.getBytesFormatted()),
                maxLines = 1,
                fontSize = 14.sp,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

/************
 * Previews *
 ************/

@Preview
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

@Preview
@Composable
private fun ItemListPreview() {
    XmpTheme {
        ItemList(
            item = PlaylistItem(
                PlaylistType.TYPE_FILE,
                "Some Item Some Item Some Item Some Item Some Type",
                "Some Type Some Type Some Type Some Type Some Type"
            ),
            isDraggable = true,
            onDrag = { /**/ },
            onClick = {},
            onLongClick = {},
        )
    }
}

@Preview
@Composable
private fun ItemModulePreview() {
    XmpTheme {
        ItemModule(
            item = Module(
                format = "XM",
                songtitle = "Some History Song Title",
                artistInfo = ArtistInfo(artist = Artist(alias = "Some History Artist Info")),
                bytes = 6690000
            )
        ) {}
    }
}

@Preview
@Composable
private fun ItemBreadCrumbPreview() {
    XmpTheme(false) {
        ItemBreadCrumb(
            crumb = "Some Bread Crumb",
            onClick = {},
            onLongClick = {}
        )
    }
}
