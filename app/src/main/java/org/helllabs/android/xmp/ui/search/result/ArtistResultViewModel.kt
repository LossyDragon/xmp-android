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
import org.helllabs.android.xmp.model.ArtistResult

sealed class ArtistEvent {
    data class FetchArtist(val query: String) : ArtistEvent()
}

sealed class ArtistUiState {
    data class Error(val error: String?) : ArtistUiState()
    data class Loading(val isLoading: Boolean) : ArtistUiState()
}

data class ArtistState(
    var result: ArtistResult? = null,
    val softError: String? = null,
)

@HiltViewModel
class ArtistResultViewModel
@Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _uiState = MutableSharedFlow<ArtistUiState>()
    val uiState: SharedFlow<ArtistUiState> = _uiState.asSharedFlow()

    private val _state = mutableStateOf(ArtistState())
    val state: State<ArtistState> = _state

    fun onEvent(event: ArtistEvent) {
        when (event) {
            is ArtistEvent.FetchArtist -> fetchArtists(event.query)
        }
    }

    private fun fetchArtists(query: String) =
        viewModelScope.launch {
            _uiState.emit(ArtistUiState.Loading(isLoading = true))

            try {
                val result = repository.getArtistSearch(query)
                _state.value = if (!result.error.isNullOrBlank()) {
                    ArtistState(softError = result.error)
                } else {
                    ArtistState(result = result)
                }

                _uiState.emit(ArtistUiState.Loading(isLoading = false))
            } catch (e: Exception) {
                _uiState.emit(ArtistUiState.Error(e.localizedMessage))
            }
        }
}
