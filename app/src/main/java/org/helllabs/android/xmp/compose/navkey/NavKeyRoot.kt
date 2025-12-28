package org.helllabs.android.xmp.compose.navkey

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class NavKeyRoot : NavKey {
    @Serializable
    data object Main : NavKeyRoot()

    // @Serializable
    // data object Player : NavKeyRoot()

    @Serializable
    data object Settings : NavKeyRoot()

    @Serializable
    data object SettingsAbout : NavKeyRoot()

    @Serializable
    data object SettingsFormats : NavKeyRoot()
}
