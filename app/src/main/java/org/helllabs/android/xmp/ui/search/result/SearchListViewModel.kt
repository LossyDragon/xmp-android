package org.helllabs.android.xmp.ui.search.result

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.api.Repository
import org.helllabs.android.xmp.model.SearchListResult
import org.helllabs.android.xmp.util.logE

data class SearchListState(
    var result: SearchListResult? = null,
    val softError: String? = null,
)

sealed class SearchListEvent {
    data class FileOrTitle(val query: String) : SearchListEvent()
    data class ArtistById(val id: Int) : SearchListEvent()
}

sealed class SearchListUiState {
    data class Error(val error: String?) : SearchListUiState()
    data class Loading(val isLoading: Boolean) : SearchListUiState()
}

@HiltViewModel
class SearchListViewModel
@Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _uiState = MutableSharedFlow<SearchListUiState>()
    val uiState: SharedFlow<SearchListUiState> = _uiState.asSharedFlow()

    private val _state = mutableStateOf(SearchListState())
    val state: State<SearchListState> = _state

    fun onEvent(event: SearchListEvent) {
        when (event) {
            is SearchListEvent.ArtistById -> getArtistById(event.id)
            is SearchListEvent.FileOrTitle -> getFileOrTitle(event.query)
        }
    }

    private fun getFileOrTitle(query: String) =
        viewModelScope.launch {
            _uiState.emit(SearchListUiState.Loading(isLoading = true))

            try {
                val result = repository.getFileNameOrTitle(query)
                _state.value = if (!result.error.isNullOrBlank()) {
                    SearchListState(softError = result.error)
                } else {
                    SearchListState(result = result)
                }

                _uiState.emit(SearchListUiState.Loading(isLoading = false))
            } catch (e: Exception) {
                this@SearchListViewModel.logE(e.stackTraceToString())
                _uiState.emit(SearchListUiState.Error(e.localizedMessage))
            }
        }

    private fun getArtistById(id: Int) =
        viewModelScope.launch {
            _uiState.emit(SearchListUiState.Loading(isLoading = true))

            try {
                val result = repository.getArtistById(id)
                _state.value = if (!result.error.isNullOrBlank()) {
                    SearchListState(softError = result.error)
                } else {
                    SearchListState(result = result)
                }

                _uiState.emit(SearchListUiState.Loading(isLoading = false))
            } catch (e: Exception) {
                this@SearchListViewModel.logE(e.stackTraceToString())
                _uiState.emit(SearchListUiState.Error(e.localizedMessage))
            }
        }
}
