package org.helllabs.android.xmp.ui.components

import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.model.PlaylistType

fun fakeDataPlaylistMenu(): List<PlaylistItem> {
    val list = mutableListOf<PlaylistItem>()
    repeat(50) {
        list.add(
            PlaylistItem(
                type = PlaylistType.TYPE_PLAYLIST,
                name = "Playlist $it",
                comment = "Comment $it",
                id = it,
                file = null,
            )
        )
    }

    return list
}
