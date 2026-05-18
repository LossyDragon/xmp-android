package com.lossydragon.media3.ui.downloads

import androidx.navigation3.runtime.NavKey
import com.lossydragon.media3.model.SearchType
import kotlinx.serialization.Serializable

sealed interface NavKeyDownload : NavKey {
    @Serializable data object Search : NavKeyDownload

    @Serializable data object History : NavKeyDownload

    @Serializable data class SearchResult(val query: String, val type: SearchType) : NavKeyDownload

    @Serializable data class Module(val moduleId: Int) : NavKeyDownload
}
