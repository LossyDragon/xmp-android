package org.helllabs.android.xmp.presentation.ui.playlists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.presentation.utils.playlist.Playlist
import org.helllabs.android.xmp.presentation.utils.playlist.PlaylistUtils

class PlaylistMenuViewModel : ViewModel() {

    private val _playlists = MutableLiveData<MutableList<PlaylistItem>>()
    val playlists: LiveData<MutableList<PlaylistItem>> = _playlists

    init {
        getPlaylists()
    }

    // Special playlist (file browser) is injected in the composable.
    fun getPlaylists() {
        val list = mutableListOf<PlaylistItem>()

        PlaylistUtils.listNoSuffix().forEach { name ->
            val item = PlaylistItem(
                PlaylistItem.TYPE_PLAYLIST,
                name,
                Playlist.readComment(name)
            )
            list.add(item)
        }

        list.sort()

        list.add(0, PlaylistItem(PlaylistItem.TYPE_SPECIAL, "", ""))

        _playlists.postValue(list)
    }
}
