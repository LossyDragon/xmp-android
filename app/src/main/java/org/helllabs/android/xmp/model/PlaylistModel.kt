package org.helllabs.android.xmp.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.helllabs.android.xmp.serializers.UriSerializer

/**
 * @param name the name of the playlist.
 * @param comment the description of the playlist
 * @param useFileName whether to use the module name or filename.
 * @param isLoop whether to loop the playlist.
 * @param isShuffle whether to shuffle the playlist.
 * @param uri the playlist uri.
 * @param list the list of [PlaylistItem]
 */
@Immutable
@Serializable
data class Playlist(
    val name: String = "",
    val comment: String = "",
    val useFileName: Boolean = false,
    val isLoop: Boolean = false,
    val isShuffle: Boolean = false,
    @Serializable(with = UriSerializer::class)
    val uri: Uri = Uri.EMPTY,
    @Contextual
    val list: ImmutableList<PlaylistItem> = persistentListOf()
)

/**
 * @param name the module tracker name or filename.
 * @param type the module type (ie: XM, S3M)
 * @param uri the module uri.
 * @param id the module id in a list.
 */
@Immutable
@Serializable
data class PlaylistItem(
    val name: String,
    val type: String,
    @Serializable(with = UriSerializer::class)
    val uri: Uri,
    @Transient
    val id: Int = 0
)
