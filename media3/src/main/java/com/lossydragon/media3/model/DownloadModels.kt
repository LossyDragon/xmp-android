package com.lossydragon.media3.model

import androidx.compose.runtime.Immutable

enum class SearchType { TITLE, ARTIST }

/** UI state and domain models for the ModArchive download feature. */

sealed class SearchResult {
    data class Modules(val data: SearchListResult) : SearchResult()
    data class Artists(val data: ArtistResult) : SearchResult()
}

@Immutable
data class DownloadSearchState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val title: String = "",
    val result: SearchResult? = null
)

@Immutable
sealed class DownloadStatus {
    data object None : DownloadStatus()
    data object Loading : DownloadStatus()
    data class Progress(val percent: Float) : DownloadStatus()
    data object Success : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}

@Immutable
data class ModuleResultState(
    val isLoading: Boolean = false,
    val isRandom: Boolean = false,
    val module: ModuleResult? = null,
    val moduleExists: Boolean = false,
    val softError: String? = null,
    val hardError: String? = null,
    val downloadStatus: DownloadStatus = DownloadStatus.None
)
