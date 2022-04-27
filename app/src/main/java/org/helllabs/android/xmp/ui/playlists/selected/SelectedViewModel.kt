package org.helllabs.android.xmp.ui.playlists.selected

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.model.PlaylistData
import org.helllabs.android.xmp.service.PlayerUseCase
import timber.log.Timber

data class SelectedScreenState(
    val isLoading: Boolean = false,
    val data: List<PlaylistData> = emptyList(),
)

@HiltViewModel
class SelectedViewModel @Inject constructor(
    application: Application,
    private val useCase: PlayerUseCase,
) : AndroidViewModel(application) {

    private val _uiState = mutableStateOf(SelectedScreenState())
    val uiState: State<SelectedScreenState> = _uiState

    fun loadPlaylist(playlistName: String) = viewModelScope.launch {
        Timber.d("Loading playlist")
        _uiState.value = uiState.value.copy(isLoading = true)
        val playlist = "playlist::$playlistName"

        val resource = useCase.subscribeToService(playlist)

        Timber.d("Browser Result: $resource")

        val list = resource.data?.map {
            PlaylistData(
                name = it.description.title.toString(),
                type = it.description.description.toString(),
                uriPath = it.description.mediaUri.toString(),
            )
        }.orEmpty()

        _uiState.value = uiState.value.copy(data = list, isLoading = false)
        useCase.unsubscribeToService(playlist)
    }

    fun setDragDropState(data: MutableList<PlaylistData>) {
        Timber.d("Setting drop state")
        _uiState.value = uiState.value.copy(data = data)
    }
}
