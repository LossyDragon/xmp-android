package org.helllabs.android.xmp.compose.ui.home

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazygeniouz.dfc.file.DocumentFileCompat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.core.XmpException
import org.helllabs.android.xmp.model.FileItem
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber

@Stable
data class PlaylistMenuState(
    val errorText: String? = null,
    val isLoading: Boolean = true,
    val mediaPath: String = "",
    val playlistItems: ImmutableList<FileItem> = persistentListOf(),
    val editPlaylist: FileItem? = null,
    val newPlaylist: Boolean = false,
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
        playlistManager.new(name, comment)
            .onSuccess {
                prefManager.setInstalledExamplePlaylist(true)
            }
            .onFailure {
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
        viewModelScope.launch(Dispatchers.IO) {
            if (uiState.value.mediaPath.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, playlistItems = persistentListOf()) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            val items = loadPlaylistItems()
            _uiState.update { it.copy(isLoading = false, playlistItems = items) }
        }
    }

    private suspend fun loadPlaylistItems(): ImmutableList<FileItem> {
        return playlistManager.listPlaylistFiles()
            .mapCatching { files ->
                files.mapNotNull { docFile ->
                    playlistManager.load(docFile.uri)
                        .map {
                            FileItem(
                                name = playlistManager.playlist.name,
                                comment = playlistManager.playlist.comment,
                                docFile = docFile
                            )
                        }
                        .onFailure { error ->
                            Timber.e(error, "Failed to load playlist: ${docFile.name}")
                        }
                        .getOrNull()
                }.toPersistentList()
            }
            .onFailure { error ->
                Timber.e(error, "Failed to list playlist files")
            }
            .getOrDefault(persistentListOf())
    }

    fun editPlaylist(item: FileItem?) {
        _uiState.update { it.copy(editPlaylist = item) }

        if (item == null) {
            updateList()
        }
    }

    fun newPlaylist(show: Boolean) {
        _uiState.update { it.copy(newPlaylist = show) }
    }

    fun askForStorage(value: Boolean) {
        _uiState.update { it.copy(askForStorage = value) }
    }
}
