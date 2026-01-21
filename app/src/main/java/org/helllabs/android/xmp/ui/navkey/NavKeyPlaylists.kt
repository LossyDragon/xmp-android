package org.helllabs.android.xmp.ui.navkey

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.helllabs.android.xmp.serializers.UriSerializer

@Serializable
sealed class NavKeyPlaylists : NavKey {
    @Serializable
    data object Playlists : NavKeyPlaylists()

    @Serializable
    data class Edit(@Serializable(with = UriSerializer::class) val uri: Uri?) : NavKeyPlaylists()

    @Serializable
    data class Selected(@Serializable(with = UriSerializer::class) val uri: Uri) : NavKeyPlaylists()
}
