package org.helllabs.android.xmp.compose.ui.playlist.viewmodel

import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
data class PlaylistsUiState(
    val isLoading: Boolean = true,
    val mediaPath: String = "",
    val playlistItems: ImmutableList<FileItem> = persistentListOf(),
    val askForStorage: Boolean = false,
    val hasStorageAccess: Boolean = false
)

class PlaylistsViewModel(
    private val storageManager: StorageManager,
    private val prefManager: PrefManager,
    private val playlistManager: PlaylistManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState = _uiState.asStateFlow()

    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    init {
        initializeStorage()
    }

    /** Initialize storage access and load playlists if available. */
    fun initializeStorage() {
        viewModelScope.launch {
            val hasAccess = checkStoragePermissions()
            _uiState.update { it.copy(hasStorageAccess = hasAccess) }

            if (hasAccess) {
                loadDefaultPath()
            } else {
                showStorageRequest()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Refresh storage access and reload playlist list. */
    fun refreshAll() {
        viewModelScope.launch {
            val hasAccess = checkStoragePermissions()
            _uiState.update { it.copy(hasStorageAccess = hasAccess) }

            if (hasAccess) {
                loadPlaylistItems()
            } else {
                showStorageRequest()
            }
        }
    }

    /** Load or refresh the list of playlists. */
    fun loadPlaylistItems() {
        viewModelScope.launch {
            if (uiState.value.mediaPath.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, playlistItems = persistentListOf()) }
                return@launch
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

    /** Handle user selecting a storage directory via SAF. */
    fun setExplorerRootDirectory(uri: Uri?) {
        if (uri == null) {
            showError("No directory selected")
            return
        }

        viewModelScope.launch {
            if (!storageManager.takePersistablePerms(uri)) {
                showError("Failed to persist directory access")
                return@launch
            }
            prefManager.setExplorerRootPath(uri.toString())

            val hasAccess = checkStoragePermissions()
            _uiState.update { it.copy(hasStorageAccess = hasAccess) }

            loadDefaultPath()
        }
    }

    /** Initialize data directory with example playlist if needed. */
    fun initializeDataDirectory(name: String, comment: String) {
        viewModelScope.launch {
            if (uiState.value.mediaPath.isEmpty()) return@launch

            setupDataDirectory(name, comment)
                .onSuccess { loadPlaylistItems() }
                .onFailure { error ->
                    Timber.e(error, "Failed to setup data directory")
                    showError(error.message ?: "Failed to initialize directory")
                }
        }
    }

    /** Show or hide the storage request dialog. */
    fun showStorageRequest(show: Boolean = true) {
        _uiState.update { it.copy(askForStorage = show) }
    }

    /** Display an error message to the user. */
    fun showError(message: String?) {
        _snackMessage.value = message
    }

    /** Clear the current error message. */
    fun clearError() {
        _snackMessage.value = null
    }

    private suspend fun checkStoragePermissions(): Boolean {
        val result = withContext(Dispatchers.IO) {
            storageManager.checkPermissions()
        }
        Timber.d("Storage permissions: $result")
        return result
    }

    private suspend fun loadDefaultPath() {
        storageManager.getPlaylistsRootDirectory()
            .onSuccess { name ->
                _uiState.update { it.copy(mediaPath = name.path.toString(), askForStorage = false) }
                loadPlaylistItems()
            }
            .onFailure { error ->
                Timber.e(error, "Error setting default path")
                showError(error.message ?: "Error setting default path")
                _uiState.update { it.copy(mediaPath = "", askForStorage = false) }
            }
    }

    private suspend fun setupDataDirectory(name: String, comment: String): Result<Unit> {
        return runCatching {
            val dir = storageManager.getPlaylistsRootDirectory().getOrThrow()

            require(dir.isDirectory()) { "Playlist directory is not a valid directory" }

            if (prefManager.getInstalledExamplePlaylist()) {
                return@runCatching
            }

            if (dir.listFiles().isEmpty()) {
                playlistManager.createPlaylist(name, comment)
                    .onSuccess {
                        prefManager.setInstalledExamplePlaylist(true)
                    }
                    .onFailure {
                        throw XmpException("Unable to create example playlist")
                    }
            }
        }
    }
}
