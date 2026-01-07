package org.helllabs.android.xmp.compose.ui.playlist.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.FileItem
import timber.log.Timber

@Immutable
data class PlaylistsUiState(
    val isLoading: Boolean = true,
    val playlistLocation: String = "",
    val playlists: ImmutableList<FileItem> = persistentListOf()
)

class PlaylistsViewModel(
    private val playlistManager: PlaylistManager,
    private val storageManager: StorageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState = _uiState.asStateFlow()

    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    init {
        refreshPlaylists()
    }

    /** Display a snackbar message. */
    fun showError(message: String, throwable: Throwable? = null) {
        Timber.e(throwable, message)
        _snackMessage.value = message
    }

    /** Clear the snackbar message. */
    fun clearError() {
        _snackMessage.value = null
    }

    fun refreshPlaylists() {
        Timber.d("Refreshing playlists")
        viewModelScope.launch {
            clearError()
            _uiState.update { it.copy(isLoading = true) }

            val playlists = playlistManager.listAllPlaylists().fold(
                onSuccess = { playlists ->
                    playlists.map { (playlist, uri) ->
                        FileItem(name = playlist.name, comment = playlist.comment, uri = uri)
                    }.toPersistentList()
                },
                onFailure = { error ->
                    showError("Failed to list playlist files", error)
                    persistentListOf()
                }
            )

            val location = storageManager.getPlaylistsRootDirectory().fold(
                onSuccess = { file ->
                    file.path
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to find playlist root")
                    ""
                }
            )

            Timber.d("Fetched ${playlists.size} playlists")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    playlistLocation = location,
                    playlists = playlists,
                )
            }
        }
    }
}
