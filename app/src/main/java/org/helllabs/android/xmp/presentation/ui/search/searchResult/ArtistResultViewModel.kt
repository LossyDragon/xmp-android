package org.helllabs.android.xmp.presentation.ui.search.searchResult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.model.ArtistResult
import org.helllabs.android.xmp.repository.Repository

@HiltViewModel
class ArtistResultViewModel
@Inject
constructor(
    private val repository: Repository
) : ViewModel() {

    private val _artistState = MutableStateFlow<ArtistState>(ArtistState.None)
    val artistState: StateFlow<ArtistState> = _artistState

    fun fetchArtists(query: String) =
        viewModelScope.launch {
            _artistState.value = ArtistState.Load
            _artistState.value = try {
                val result = repository.getArtistSearch(query)

                if (!result.error.isNullOrBlank()) {
                    ArtistState.SoftError(result.error!!)
                    return@launch
                }

                ArtistState.SearchResult(result)
            } catch (e: Exception) {
                ArtistState.Error(e.localizedMessage)
            }
        }

    sealed class ArtistState {
        object None : ArtistState()
        object Load : ArtistState()
        class Error(val error: String?) : ArtistState()
        class SoftError(var softError: String) : ArtistState()
        class SearchResult(var result: ArtistResult) : ArtistState()
    }
}
