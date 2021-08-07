package org.helllabs.android.xmp.ui.playlist_detail

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.model.PlaylistItem

@Suppress("UNCHECKED_CAST")
inline fun <VM : ViewModel> viewModelFactory(crossinline f: () -> VM) =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(aClass: Class<T>): T = f() as T
    }

class PlaylistActivityViewModel(name: String) : ViewModel() {

    var playlist: Playlist = Playlist(name)

    private val _playlistState = MutableStateFlow<PlaylistState>(PlaylistState.None)
    val playlistState: StateFlow<PlaylistState> = _playlistState

    fun getPlaylist() {
        _playlistState.value = PlaylistState.Load

        viewModelScope.launch(Dispatchers.IO) {
            _playlistState.value = PlaylistState.Loaded(playlist.list)
        }
    }

    sealed class PlaylistState {
        object None : PlaylistState()
        object Load : PlaylistState()
        class Loaded(val list: List<PlaylistItem>) : PlaylistState()
    }
}
