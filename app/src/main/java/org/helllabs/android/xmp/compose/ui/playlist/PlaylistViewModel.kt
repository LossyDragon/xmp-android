package org.helllabs.android.xmp.compose.ui.playlist

import android.net.Uri
import androidx.compose.runtime.*
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.model.Playlist
import timber.log.Timber

@Stable
class PlaylistViewModel(
    private val playlistUri: Uri,
    private val playlistManager: PlaylistManager,
    private val prefManager: PrefManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(Playlist())
    val uiState = _uiState.asStateFlow()

    val useFileName = prefManager.useFileNameFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        viewModelScope.launch {
            val playlist = playlistManager.loadPlaylist(playlistUri).getOrThrow()
            _uiState.update { playlist }
        }
    }

    fun save() {
        viewModelScope.launch {
            playlistManager.savePlaylist(_uiState.value)
        }
    }

    fun setShuffle(value: Boolean) {
        _uiState.update { it.copy(isShuffle = value) }
    }

    fun setLoop(value: Boolean) {
        _uiState.update { it.copy(isLoop = value) }
    }

    fun onMove(from: Int, to: Int) {
        val list = _uiState.value.list.toMutableList().apply {
            add(to, removeAt(from))
        }.toPersistentList()

        _uiState.update { it.copy(list = list) }
    }

    fun onDragStopped() {
        Timber.d("Drag stopped")
        save()
    }

    fun getUriItems(): List<Uri> = _uiState.value.list.map { it.uri }

    fun onRefresh(name: String) {
        viewModelScope.launch {
            val listWithIds = _uiState.value.list.mapIndexed { index, playlistItem ->
                playlistItem.copy(id = index)
            }.toPersistentList()

            _uiState.update { it.copy(list = listWithIds) }
        }
    }

    fun removeItem(index: Int) {
        val list = _uiState.value.list.toMutableList().apply {
            removeAt(index)
        }.toPersistentList()

        _uiState.update { it.copy(list = list) }
        save() // Save just in-case
    }

    fun useFileName() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(useFileName = useFileName.value)
            }
        }
    }
}
