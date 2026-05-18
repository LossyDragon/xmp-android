package com.lossydragon.media3.ui.downloads

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lossydragon.media3.data.ModArchiveService
import com.lossydragon.media3.model.ArtistResult
import com.lossydragon.media3.model.SearchListResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

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

class DownloadViewModel(private val service: ModArchiveService) : ViewModel() {

    val state: StateFlow<DownloadSearchState>
        field = MutableStateFlow(DownloadSearchState())

    fun searchFileOrTitle(query: String) = viewModelScope.launch {
        state.update { it.copy(title = "Results: $query", isLoading = true, error = null) }
        service.searchByFileNameOrTitle(query).fold(
            onSuccess = {
                state.update { s -> s.copy(result = SearchResult.Modules(it), isLoading = false) }
            },
            onFailure = {
                Timber.e(it)
                state.update { s -> s.copy(error = it.message, isLoading = false) }
            }
        )
    }

    fun searchArtist(query: String) = viewModelScope.launch {
        state.update { it.copy(title = "Artists: $query", isLoading = true, error = null) }
        service.getArtistSearch(query).fold(
            onSuccess = {
                state.update { s -> s.copy(result = SearchResult.Artists(it), isLoading = false) }
            },
            onFailure = {
                Timber.e(it)
                state.update { s -> s.copy(error = it.message, isLoading = false) }
            }
        )
    }

    fun getArtistById(id: Int) = viewModelScope.launch {
        state.update { it.copy(isLoading = true, error = null) }
        service.getArtistById(id).fold(
            onSuccess = {
                state.update { s -> s.copy(result = SearchResult.Modules(it), isLoading = false) }
            },
            onFailure = {
                Timber.e(it)
                state.update { s -> s.copy(error = it.message, isLoading = false) }
            }
        )
    }
}
