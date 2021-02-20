package org.helllabs.android.xmp.modarchive.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.model.SearchListResult
import org.helllabs.android.xmp.repository.Repository

@HiltViewModel
class SearchListViewModel
@Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _searchResultState = MutableStateFlow<SearchResultState>(SearchResultState.None)
    val searchResultState: StateFlow<SearchResultState> = _searchResultState

    fun getFileOrTitle(query: String) =
        viewModelScope.launch {
            _searchResultState.value = SearchResultState.Load
            _searchResultState.value = try {
                val result = repository.getFileNameOrTitle(query)
                if (!result.error.isNullOrBlank()) {
                    SearchResultState.SoftError(result.error!!)
                } else {
                    SearchResultState.SearchResult(result)
                }
            } catch (e: Exception) {
                SearchResultState.Error(e.localizedMessage)
            }
        }

    fun getArtistById(id: Int) =
        viewModelScope.launch {
            _searchResultState.value = SearchResultState.Load
            _searchResultState.value = try {
                val result = repository.getArtistById(id)
                if (!result.error.isNullOrBlank()) {
                    SearchResultState.SoftError(result.error!!)
                } else {
                    SearchResultState.SearchResult(result)
                }
            } catch (e: Exception) {
                SearchResultState.Error(e.localizedMessage)
            }
        }

    sealed class SearchResultState {
        object None : SearchResultState()
        object Load : SearchResultState()
        class Error(val error: String?) : SearchResultState()
        class SoftError(var softError: String) : SearchResultState()
        class SearchResult(var result: SearchListResult) : SearchResultState()
    }
}
