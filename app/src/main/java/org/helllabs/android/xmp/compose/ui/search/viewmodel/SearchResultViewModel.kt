package org.helllabs.android.xmp.compose.ui.search.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.api.ModArchiveService
import org.helllabs.android.xmp.model.ArtistResult
import org.helllabs.android.xmp.model.SearchListResult

sealed class SearchResult {
    data class Modules(val data: SearchListResult) : SearchResult()
    data class Artists(val data: ArtistResult) : SearchResult()
}

@Immutable
data class SearchResultState(
    val isLoading: Boolean = false,
    val softError: String? = null,
    val title: String = "",
    val result: SearchResult? = null
)

class SearchResultViewModel(private val modArchive: ModArchiveService) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchResultState())
    val uiState = _uiState.asStateFlow()

    fun getFileOrTitle(title: String, query: String) = viewModelScope.launch {
        _uiState.update { it.copy(title = title, isLoading = true) }

        modArchive.getSearchByFileNameOrTitle(query).fold(
            onSuccess = { result ->
                _uiState.update {
                    it.copy(
                        result = SearchResult.Modules(result),
                        softError = null,
                        isLoading = false
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(softError = error.message, isLoading = false)
                }
            }
        )
    }

    fun getArtistById(id: Int) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        modArchive.getArtistById(id).fold(
            onSuccess = { result ->
                _uiState.update {
                    it.copy(
                        result = SearchResult.Modules(result),
                        softError = null,
                        isLoading = false
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(softError = error.message, isLoading = false)
                }
            }
        )
    }

    fun getArtists(title: String, query: String) = viewModelScope.launch {
        _uiState.update { it.copy(title = title, isLoading = true) }

        modArchive.getArtistSearch(query).fold(
            onSuccess = { result ->
                _uiState.update {
                    it.copy(
                        result = SearchResult.Artists(result),
                        softError = null,
                        isLoading = false
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(softError = error.message, isLoading = false)
                }
            }
        )
    }
}
