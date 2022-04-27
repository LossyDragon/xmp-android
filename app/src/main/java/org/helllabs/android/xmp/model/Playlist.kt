package org.helllabs.android.xmp.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class Playlist(
    val name: String,
    val comment: String,
    val data: List<PlaylistData>
) : Parcelable, List<PlaylistData> by data {
    @IgnoredOnParcel
    var orderID: Int = 0
        private set

    fun setOrderID(value: Int) {
        orderID = value
    }
}

@Parcelize
data class PlaylistData(
    val id: Int = 0,
    val name: String,
    val type: String,
    val uriPath: String,
) : Parcelable
