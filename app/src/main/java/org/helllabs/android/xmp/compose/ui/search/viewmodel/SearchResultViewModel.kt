package org.helllabs.android.xmp.compose.ui.search.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.api.Repository
import org.helllabs.android.xmp.core.Resource

@Stable
data class SearchResultState(
    val isLoading: Boolean = false,
    val softError: String? = null,
    val title: String = "",
    val result: Any? = null
)

@Stable
class SearchResultViewModel(private val repository: Repository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchResultState())
    val uiState = _uiState.asStateFlow()

    fun getFileOrTitle(string: String, query: String) = viewModelScope.launch {
        _uiState.update { it.copy(title = string, isLoading = true) }

        repository.getFileNameOrTitle(query).collectLatest { resource ->
            when (resource) {
                is Resource.Success -> {
                    val result = resource.data
                    if (result != null) {
                        _uiState.update {
                            it.copy(result = result, softError = "", isLoading = false)
                        }
                    } else {
                        _uiState.update {
                            it.copy(softError = "No data returned", isLoading = false)
                        }
                    }
                }

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(softError = resource.message, isLoading = false)
                    }
                }

                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun getArtistById(id: Int) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        repository.getArtistById(id).collectLatest { resource ->
            when (resource) {
                is Resource.Success -> {
                    val result = resource.data
                    if (result != null) {
                        _uiState.update {
                            it.copy(result = result, softError = "", isLoading = false)
                        }
                    } else {
                        _uiState.update {
                            it.copy(softError = "No data returned", isLoading = false)
                        }
                    }
                }

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(softError = resource.message, isLoading = false)
                    }
                }

                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun getArtists(string: String, query: String) = viewModelScope.launch {
        _uiState.update { it.copy(title = string, isLoading = true) }

        repository.getArtistSearch(query).collectLatest { resource ->
            when (resource) {
                is Resource.Success -> {
                    val result = resource.data
                    if (result != null) {
                        _uiState.update {
                            it.copy(
                                result = result,
                                softError = "",
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(softError = "No data returned", isLoading = false)
                        }
                    }
                }

                is Resource.Error -> {
                    _uiState.update {
                        it.copy(softError = resource.message, isLoading = false)
                    }
                }

                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
}
