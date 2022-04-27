package org.helllabs.android.xmp.util

import org.helllabs.android.xmp.model.PlaylistData

fun fake_PlaylistData(): List<PlaylistData> {
    val data = mutableListOf<PlaylistData>()

    repeat(30) {
        val item = PlaylistData(
            name = "Name $it",
            type = "Type $it",
            uriPath = ""
        )
        data.add(item)
    }

    return data
}
