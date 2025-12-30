package org.helllabs.android.xmp.compose.ui.home

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.core.XmpException
import org.helllabs.android.xmp.model.FileItem
import timber.log.Timber

@Stable
data class PlaylistMenuState(
    val errorText: String? = null,
    val isLoading: Boolean = true,
    val mediaPath: String = "",
    val playlistItems: ImmutableList<FileItem> = persistentListOf(),
    val askForStorage: Boolean = false
)

class PlaylistMenuViewModel(
    private val storageManager: StorageManager,
    private val prefManager: PrefManager,
    private val playlistManager: PlaylistManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistMenuState())
    val uiState = _uiState.asStateFlow()

    fun showError(message: String?) {
        _uiState.update { it.copy(errorText = message) }
    }

    /**
     * Create application directory and populate with empty playlist
     */
    suspend fun setupDataDir(name: String, comment: String): Result<Unit> {
        return runCatching {
            val dir = storageManager.getPlaylistDirectory().getOrThrow()

            require(!dir.isFile()) {
                "Playlist Directory returned null or is file!"
            }

            if (prefManager.getInstalledExamplePlaylist()) {
                return@runCatching
            }

            val isPlaylistEmpty = dir.listFiles().isEmpty()
            if (isPlaylistEmpty) {
                createExamplePlaylist(name, comment)
            }
        }.onFailure { error ->
            Timber.e(error, "Failed to setup data directory")
        }
    }

    private suspend fun createExamplePlaylist(name: String, comment: String) {
        playlistManager.createPlaylist(name, comment).onSuccess {
            prefManager.setInstalledExamplePlaylist(true)
        }.onFailure {
            Timber.e(it)
            throw XmpException("Unable to create Example playlist")
        }
    }

    suspend fun setDefaultPath() {
        storageManager.getDefaultPathName()
            .onSuccess { name ->
                _uiState.update { it.copy(mediaPath = name, askForStorage = false) }
                updateList()
            }
            .onFailure { err ->
                Timber.e(err, "Error setting default path")
                showError(err.message ?: "Error setting default path")
                _uiState.update { it.copy(mediaPath = "", askForStorage = false) }
            }
    }

    fun updateList() {
        viewModelScope.launch {
            refreshPlaylistItems()
        }
    }

    private suspend fun refreshPlaylistItems() {
        withContext(Dispatchers.IO) {
            if (uiState.value.mediaPath.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, playlistItems = persistentListOf()) }
                return@withContext
            }

            _uiState.update { it.copy(isLoading = true) }
            val items = playlistManager.listAllPlaylists().fold(
                onSuccess = { playlists ->
                    playlists.map {
                        FileItem(name = it.name, comment = it.comment, uri = it.uri)
                    }.toPersistentList()
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to list playlist files")
                    persistentListOf()
                }
            )

            _uiState.update { it.copy(isLoading = false, playlistItems = items) }
        }
    }

    // null `playlistItem` will be a new playlist
    fun editPlaylist(
        fileItem: FileItem?,
        name: String,
        comment: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = fileItem?.let { item ->
                playlistManager.loadPlaylist(item.uri).mapCatching { playlist ->
                    val withComment = playlistManager.setComment(playlist, comment)

                    if (playlist.name != name) {
                        playlistManager.renamePlaylist(withComment, name).getOrThrow()
                    } else {
                        playlistManager.savePlaylist(withComment).getOrThrow()
                    }
                }
            } ?: run {
                Timber.d("New Playlist: $name")
                playlistManager.createPlaylist(name, comment).map { }
            }

            result.fold(
                onSuccess = {
                    Timber.d("Playlist ${fileItem?.name.orEmpty()} edited")
                    refreshPlaylistItems()
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to edit playlist")
                    showError(error.message ?: "Failed to edit playlist")
                }
            )
        }
    }

    fun deletePlaylist(playlistItem: FileItem) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistManager.deletePlaylist(playlistItem.uri).fold(
                onSuccess = {
                    refreshPlaylistItems()
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to delete playlist")
                    showError(error.message ?: "Failed to delete playlist")
                }
            )
        }
    }

    fun askForStorage(value: Boolean) {
        _uiState.update { it.copy(askForStorage = value) }
    }
}
