package org.helllabs.android.xmp.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.helllabs.android.xmp.serializers.ImmutableListSerializer
import org.helllabs.android.xmp.serializers.UriSerializer

@Immutable
@Serializable
data class Playlist(
    val comment: String = "",
    val isLoop: Boolean = false,
    val isShuffle: Boolean = false,
    @Serializable(with = ImmutableListSerializer::class)
    val list: ImmutableList<PlaylistItem> = persistentListOf(),
    val name: String = "",
    @Serializable(with = UriSerializer::class)
    val uri: Uri = Uri.EMPTY,
    val useFileName: Boolean = false
) {
    fun withComment(newComment: String) = copy(comment = newComment)
    fun withName(newName: String) = copy(name = newName)
    fun withLoop(value: Boolean) = copy(isLoop = value)
    fun withShuffle(value: Boolean) = copy(isShuffle = value)
    fun withList(newList: ImmutableList<PlaylistItem>) = copy(list = newList)
    fun withUri(newUri: Uri) = copy(uri = newUri)
}

@Immutable
@Serializable
data class PlaylistItem(
    val name: String,
    val type: String,
    @Serializable(with = UriSerializer::class)
    val uri: Uri,
    @Transient
    val id: Int = 0 // Can now be copied/modified
)
