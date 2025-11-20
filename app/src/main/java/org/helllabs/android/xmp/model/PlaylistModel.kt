package org.helllabs.android.xmp.model

import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.helllabs.android.xmp.core.UriSerializer

@Serializable
data class Playlist(
    var comment: String = "",
    var isLoop: Boolean = false,
    var isShuffle: Boolean = false,
    var list: List<PlaylistItem> = listOf(),
    var name: String = "",
    @Serializable(with = UriSerializer::class)
    var uri: Uri = Uri.EMPTY,
    var useFileName: Boolean = false
)

@Serializable
data class PlaylistItem(
    val name: String,
    val type: String,
    @Serializable(with = UriSerializer::class)
    val uri: Uri
) {
    @Transient
    var id: Int = 0
}
