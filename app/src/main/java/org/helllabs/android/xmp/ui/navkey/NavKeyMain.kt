package org.helllabs.android.xmp.ui.navkey

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Serializable
sealed class NavKeyMain(val title: String) : NavKey {
    // ImageVector not serializable
    abstract val selectedIcon: ImageVector?
    abstract val unSelectedIcon: ImageVector?

    @Serializable
    data object Playlists : NavKeyMain("Playlists") {
        override val selectedIcon: ImageVector = Icons.Filled.LibraryMusic
        override val unSelectedIcon: ImageVector = Icons.Outlined.LibraryMusic
    }

    @Serializable
    data object Explorer : NavKeyMain("Explorer") {
        override val selectedIcon: ImageVector = Icons.Filled.Folder
        override val unSelectedIcon: ImageVector = Icons.Outlined.Folder
    }

    @Serializable
    data object Downloads : NavKeyMain("Downloads") {
        override val selectedIcon: ImageVector = Icons.Filled.Download
        override val unSelectedIcon: ImageVector = Icons.Outlined.Download
    }

    companion object {
        fun findScreen(value: String): NavKeyMain = when (value) {
            "Playlists" -> Playlists
            "Explorer" -> Explorer
            "Downloads" -> Downloads
            else -> Playlists
        }

        fun getAllScreens(): ImmutableList<String> =
            persistentListOf(Playlists.title, Explorer.title, Downloads.title)
    }
}
