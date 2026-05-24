package com.lossydragon.media3.ui.screens.playlists

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lossydragon.media3.data.PlaylistRepository
import com.lossydragon.media3.data.exportPlaylistsToJson
import com.lossydragon.media3.data.readImportFromUri
import com.lossydragon.media3.data.toRoomEntities
import com.lossydragon.media3.data.writeExportToUri
import com.lossydragon.media3.db.entity.PlaylistEntity
import com.lossydragon.media3.db.entity.PlaylistEntryEntity
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.model.PlayerUiState
import com.lossydragon.media3.player.XmpPlayerViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlaylistsUiState(
    val playlists: ImmutableList<PlaylistEntity> = persistentListOf(),
    val selectedPlaylist: PlaylistEntity? = null,
    val entries: ImmutableList<ModuleFile> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val importResult: ImportResultUi? = null,
    val exportSuccess: Boolean = false
)

data class ImportResultUi(
    val playlistsImported: Int,
    val entriesImported: Int,
    val skipped: Int
)

class PlaylistsViewModel(
    private val appContext: Context,
    private val repo: PlaylistRepository,
    private val player: XmpPlayerViewModel
) : ViewModel() {

    val playerState: StateFlow<PlayerUiState> get() = player.state

    val state: StateFlow<PlaylistsUiState>
        field = MutableStateFlow(PlaylistsUiState())

    init {
        repo.playlists
            .onEach { playlists ->
                state.update { it.copy(playlists = playlists.toImmutableList()) }
            }
            .launchIn(viewModelScope)
    }

    /** Creates a new empty playlist with [name]. */
    fun createPlaylist(name: String, comment: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { repo.createPlaylist(name.trim(), comment.trim()) }
                .onFailure { state.update { s -> s.copy(error = it.message) } }
        }
    }

    /** Renames [playlist] to [newName]. */
    fun renamePlaylist(playlist: PlaylistEntity, newName: String, newComment: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            runCatching {
                repo.renamePlaylist(playlist.id, newName.trim(), newComment.trim())
            }.onFailure { state.update { s -> s.copy(error = it.message) } }
        }
    }

    /** Deletes [playlist] and all its entries. */
    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            runCatching { repo.deletePlaylist(playlist.id) }
                .onFailure { state.update { s -> s.copy(error = it.message) } }

            // If the deleted playlist was selected, deselect it
            if (state.value.selectedPlaylist?.id == playlist.id) {
                state.update { it.copy(selectedPlaylist = null, entries = persistentListOf()) }
            }
        }
    }

    /** Selects [playlist] and loads its entries. */
    fun selectPlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, selectedPlaylist = playlist) }
            runCatching { repo.getPlaylistFiles(playlist.id).toImmutableList() }
                .onSuccess { files ->
                    state.update { it.copy(entries = files, isLoading = false) }
                }
                .onFailure { e ->
                    state.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    /** Adds [file] to [playlist]. Refreshes entries if [playlist] is currently selected. */
    fun addToPlaylist(playlist: PlaylistEntity, file: ModuleFile) {
        viewModelScope.launch {
            runCatching { repo.addToPlaylist(playlist.id, file) }
                .onSuccess {
                    if (state.value.selectedPlaylist?.id == playlist.id) {
                        refreshEntries()
                    }
                }
                .onFailure { state.update { s -> s.copy(error = it.message) } }
        }
    }

    /** Removes [file] from the currently selected playlist. */
    fun removeFromPlaylist(file: ModuleFile) {
        val playlist = state.value.selectedPlaylist ?: return
        viewModelScope.launch {
            runCatching { repo.removeFromPlaylist(playlist.id, file.uri.toString()) }
                .onSuccess { refreshEntries() }
                .onFailure { state.update { s -> s.copy(error = it.message) } }
        }
    }

    /** Moves entry at [fromIndex] to [toIndex] in the selected playlist. */
    fun reorderEntry(fromIndex: Int, toIndex: Int) {
        val playlist = state.value.selectedPlaylist ?: return
        val entries = state.value.entries.toMutableList()
        if (fromIndex !in entries.indices || toIndex !in entries.indices) return

        entries.add(toIndex, entries.removeAt(fromIndex))

        // Optimistic update
        state.update { it.copy(entries = entries.toImmutableList()) }

        viewModelScope.launch {
            runCatching { repo.reorderEntries(playlist.id, entries) }
                .onFailure { e ->
                    state.update { s -> s.copy(error = e.message) }
                    refreshEntries() // revert on failure
                }
        }
    }

    /** Clears the selected playlist. */
    fun deselectPlaylist() {
        state.update { it.copy(selectedPlaylist = null, entries = persistentListOf()) }
    }

    /**
     * Exports all playlists to [outputUri].
     * Call after the user picks a file via SAF.
     */
    fun exportPlaylists(outputUri: Uri) {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true) }

            runCatching {
                withContext(Dispatchers.IO) {
                    val allPlaylists = repo.getAllPlaylistsOnce()
                    val entries = allPlaylists.associate { it.id to repo.getEntriesRaw(it.id) }
                    val jsonString = exportPlaylistsToJson(allPlaylists, entries)
                    writeExportToUri(appContext, outputUri, jsonString).getOrThrow()
                }
            }
                .onSuccess {
                    state.update { it.copy(isLoading = false, exportSuccess = true) }
                }
                .onFailure { e ->
                    state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    /**
     * Imports playlists from [inputUri].
     * Call after the user picks a file via SAF.
     * @param validateUris Whether to skip entries whose URIs are no longer accessible.
     */
    fun importPlaylists(inputUri: Uri, validateUris: Boolean = true) {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true) }

            runCatching {
                withContext(Dispatchers.IO) {
                    val export = readImportFromUri(appContext, inputUri).getOrThrow()
                    val (playlists, entryMap, result) = export.toRoomEntities(
                        context = appContext,
                        validateUris = validateUris,
                    )
                    playlists.forEach { playlist ->
                        val newId = repo.createPlaylist(playlist)
                        entryMap[playlist.name]?.forEach { entry ->
                            repo.addRawEntry(entry.copy(playlistId = newId))
                        }
                    }
                    result
                }
            }.onSuccess { result ->
                state.update {
                    it.copy(
                        isLoading = false,
                        importResult = ImportResultUi(
                            playlistsImported = result.playlistsImported,
                            entriesImported = result.entriesImported,
                            skipped = result.skipped,
                        )
                    )
                }
            }.onFailure { e ->
                state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun dismissError() = state.update { it.copy(error = null) }

    fun dismissImportResult() = state.update { it.copy(importResult = null) }

    fun dismissExportSuccess() = state.update { it.copy(exportSuccess = false) }

    // Playback — play selected playlist
    fun playPlaylist(startAt: Int = 0, isShuffle: Boolean = false) {
        val entries = state.value.entries
        if (entries.isEmpty()) return
        player.playAll(
            files = entries.toImmutableList(),
            startAt = startAt,
            isShuffle = isShuffle,
        )
    }

    fun playEntry(file: ModuleFile) {
        val entries = state.value.entries
        val index = entries.indexOf(file).coerceAtLeast(0)
        player.playAll(
            files = entries.toImmutableList(),
            startAt = index,
            isShuffle = false,
        )
    }

    // Mini player controls
    fun togglePlayPause() = player.togglePlayPause()

    fun next() = player.next()

    fun previous() = player.previous()

    private fun refreshEntries() {
        val playlist = state.value.selectedPlaylist ?: return
        viewModelScope.launch {
            runCatching { repo.getPlaylistFiles(playlist.id).toImmutableList() }
                .onSuccess { files -> state.update { it.copy(entries = files) } }
                .onFailure { e -> state.update { s -> s.copy(error = e.message) } }
        }
    }
}
