package org.helllabs.android.xmp.ui.playlist_list

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.model.PlaylistType
import org.helllabs.android.xmp.ui.MainActivity
import org.helllabs.android.xmp.util.PlaylistUtils

sealed class PlaylistEvent {
    object Refresh : PlaylistEvent()
    data class Add(val name: String, val comment: String) : PlaylistEvent()
    data class Delete(val name: String) : PlaylistEvent()
    data class Edit(val name: String, val comment: String, val oldName: String) : PlaylistEvent()
    data class Setup(val name: String, val comment: String) : PlaylistEvent()
}

sealed class PlaylistUiEvent {
    object CreateError : PlaylistUiEvent()
    object EditCommentError : PlaylistUiEvent()
    object EditRenameError : PlaylistUiEvent()
    object SetupMkDirsError : PlaylistUiEvent()
    object SetupPlaylistError : PlaylistUiEvent()
    data class Loading(val isLoading: Boolean) : PlaylistUiEvent()
}

data class PlaylistState(
    val result: List<PlaylistItem> = listOf(),
    val softError: String? = null,
)

class PlaylistViewModel : ViewModel() {

    private val _uiState = MutableSharedFlow<PlaylistUiEvent>()
    val uiState: SharedFlow<PlaylistUiEvent> = _uiState.asSharedFlow()

    private val _state = mutableStateOf(PlaylistState())
    val state: State<PlaylistState> = _state

    init {
        onEvent(PlaylistEvent.Refresh)
    }

    fun onEvent(event: PlaylistEvent) {
        when (event) {
            PlaylistEvent.Refresh -> refreshList()
            is PlaylistEvent.Add -> addPlaylist(event.name, event.comment)
            is PlaylistEvent.Delete -> deletePlaylist(event.name)
            is PlaylistEvent.Edit -> editPlaylist(event.name, event.comment, event.oldName)
            is PlaylistEvent.Setup -> setupDataDir(event.name, event.comment)
        }
    }

    private fun refreshList() {
        viewModelScope.launch {
            _uiState.emit(PlaylistUiEvent.Loading(isLoading = true))

            val playlists = PlaylistUtils.listNoSuffix().map { name ->
                val comment = PlaylistUtils.readComment(name).orEmpty()
                PlaylistItem(PlaylistType.TYPE_PLAYLIST, name, comment)
            }.toMutableList()

            playlists.sort()
            PlaylistUtils.renumberIds(playlists)

            _state.value = PlaylistState(result = playlists)
            _uiState.emit(PlaylistUiEvent.Loading(isLoading = false))
        }
    }

    private fun addPlaylist(name: String, comment: String) {
        viewModelScope.launch {
            val result = PlaylistUtils.createEmptyPlaylist(name, comment)

            if (result) {
                onEvent(PlaylistEvent.Refresh)
            } else {
                _uiState.emit(PlaylistUiEvent.CreateError)
            }
        }
    }

    private fun deletePlaylist(name: String) {
        PlaylistUtils.delete(name)
        onEvent(PlaylistEvent.Refresh)
    }

    private fun editPlaylist(name: String, comment: String, oldName: String) {
        viewModelScope.launch {
            if (!PlaylistUtils.rename(oldName, name)) {
                _uiState.emit(PlaylistUiEvent.EditRenameError)
                return@launch
            }

            val file = File(MainActivity.DATA_DIR, name + PlaylistUtils.COMMENT_SUFFIX)
            if (!PlaylistUtils.editComment(file, comment)) {
                _uiState.emit(PlaylistUiEvent.EditCommentError)
                return@launch
            }

            onEvent(PlaylistEvent.Refresh)
        }
    }

    private fun setupDataDir(name: String, comment: String) {
        viewModelScope.launch {
            if (!MainActivity.DATA_DIR.isDirectory) {
                if (MainActivity.DATA_DIR.mkdirs()) {
                    if (!PlaylistUtils.createEmptyPlaylist(name, comment)) {
                        _uiState.emit(PlaylistUiEvent.SetupPlaylistError)
                        return@launch
                    }
                } else {
                    _uiState.emit(PlaylistUiEvent.SetupMkDirsError)
                    return@launch
                }
            }

            onEvent(PlaylistEvent.Refresh)
        }
    }
}
