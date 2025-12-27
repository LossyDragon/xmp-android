package org.helllabs.android.xmp.compose.ui.playlist

import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.model.Playlist
import timber.log.Timber

@Stable
class PlaylistViewModel : ViewModel() {

    private val manager: MutableStateFlow<PlaylistManager> = MutableStateFlow(PlaylistManager())

    private val _uiState = MutableStateFlow(Playlist())
    val uiState = _uiState.asStateFlow()

    fun save() {
        with(manager.value) {
            setLoop(_uiState.value.isLoop)
            setShuffle(_uiState.value.isShuffle)
            setList(_uiState.value.list)
            save()
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
        manager.value = PlaylistManager()
        with(manager.value) {
            load(Uri.parse(name))

            val listWithIds = playlist.list.mapIndexed { index, playlistItem ->
                playlistItem.copy(id = index)
            }.toPersistentList()

            _uiState.update {
                it.copy(
                    comment = playlist.comment,
                    isLoop = playlist.isLoop,
                    isShuffle = playlist.isShuffle,
                    list = listWithIds,
                    name = playlist.name,
                    uri = playlist.uri
                )
            }
        }
    }

    fun removeItem(index: Int) {
        val list = _uiState.value.list.toMutableList().apply {
            removeAt(index)
        }.toPersistentList()

        _uiState.update { it.copy(list = list) }
        save() // Save just in-case
    }

    fun useFileName(useFileName: Boolean) {
        _uiState.update {
            it.copy(useFileName = useFileName)
        }
    }
}
