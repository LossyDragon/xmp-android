package org.helllabs.android.xmp.ui.navkey

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.helllabs.android.xmp.ui.screens.search.screen.SearchType

@Serializable
sealed class NavKeySearch : NavKey {
    @Serializable
    data object Search : NavKeySearch()

    @Serializable
    data object SearchHistory : NavKeySearch()

    @Serializable
    data class SearchResult(val query: String, val type: SearchType) : NavKeySearch()

    @Serializable
    data class Result(val moduleID: Int) : NavKeySearch()
}
