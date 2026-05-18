package com.lossydragon.media3.ui

import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.*
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class NavKeyRoot : NavKey {
    @Serializable
    data object Main : NavKeyRoot()

    @Serializable
    data object Player : NavKeyRoot()
}

@Serializable
sealed class NavKeyMain(val title: String) : NavKey {
    abstract val selectedIcon: ImageVector
    abstract val unselectedIcon: ImageVector

    @Serializable
    data object Browser : NavKeyMain("Browser") {
        override val selectedIcon = Icons.Filled.Folder
        override val unselectedIcon = Icons.Outlined.Folder
    }

    @Serializable
    data object Playlists : NavKeyMain("Playlists") {
        override val selectedIcon = Icons.AutoMirrored.Filled.List
        override val unselectedIcon = Icons.AutoMirrored.Outlined.List
    }

    @Serializable
    data object Downloads : NavKeyMain("Downloads") {
        override val selectedIcon = Icons.Filled.Download
        override val unselectedIcon = Icons.Outlined.Download
    }

    @Serializable
    data object NowPlaying : NavKeyMain("Now Playing") {
        override val selectedIcon = Icons.Filled.MusicNote
        override val unselectedIcon = Icons.Outlined.MusicNote
    }
}
