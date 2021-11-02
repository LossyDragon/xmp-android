package org.helllabs.android.xmp.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.model.PlaylistType
import org.helllabs.android.xmp.ui.preferences.Preferences
import org.helllabs.android.xmp.util.PlaylistUtils

class PlaylistMenuViewModel : ViewModel() {

    private val _playlistState = MutableStateFlow<PlaylistMenuState>(PlaylistMenuState.None)
    val playlistState: StateFlow<PlaylistMenuState> = _playlistState

    fun updateList() {
        _playlistState.value = PlaylistMenuState.Load

        val list = mutableListOf<PlaylistItem>()

        viewModelScope.launch {
            PlaylistUtils.listNoSuffix().forEach { name ->
                val comment = PlaylistUtils.readComment(name)
                val item = PlaylistItem(PlaylistType.TYPE_PLAYLIST, name, comment)
                list.add(item)
            }
            list.sort()
        }

        list.add(0, PlaylistItem(PlaylistType.TYPE_SPECIAL, null, null))
        PlaylistUtils.renumberIds(list)
        _playlistState.value = PlaylistMenuState.Loaded(list)
    }

    fun addPlaylist(name: String, comment: String): Boolean {
        val result = PlaylistUtils.createEmptyPlaylist(name, comment)
        updateList()
        return result
    }

    /**
     * Edit the specified playlist
     *
     * @return a value indicating it's success:
     *           0 success
     *          -1 failed playlist
     *          -2 failed comment
     */
    fun editPlaylist(id: Int, name: String, comment: String, oldName: String?): EEditPlaylist {
        when (id) {
            EditState.RESULT_DELETE_PLAYLIST.value -> PlaylistUtils.delete(name)
            EditState.RESULT_EDIT_PLAYLIST.value -> {
                if (!PlaylistUtils.rename(oldName!!, name)) {
                    return EEditPlaylist.FAILED_PLAYLIST
                }

                val file = File(Preferences.DATA_DIR, name + PlaylistUtils.COMMENT_SUFFIX)
                if (!PlaylistUtils.editComment(file, comment)) {
                    return EEditPlaylist.FAILED_COMMENT
                }
            }
            else -> throw IllegalArgumentException("Edit playlist id was not correct: $id")
        }

        updateList()
        return EEditPlaylist.SUCCESS
    }

    /**
     * Create application directory and populate with empty playlist
     *
     * @return a value indicating it's success:
     *           0 success
     *          -1 failed making playlist
     *          -2 failed making dirs
     */
    fun setupDataDir(name: String, comment: String): ESetupDataDir {
        if (!Preferences.DATA_DIR.isDirectory) {
            if (Preferences.DATA_DIR.mkdirs()) {
                if (!PlaylistUtils.createEmptyPlaylist(name, comment))
                    return ESetupDataDir.PLAYLIST_ERROR
            } else {
                return ESetupDataDir.MKDIRS_ERROR
            }
        }

        return ESetupDataDir.SUCCESS
    }

    sealed class PlaylistMenuState {
        object None : PlaylistMenuState()
        object Load : PlaylistMenuState()
        class Loaded(val list: List<PlaylistItem>) : PlaylistMenuState()
    }

    enum class ESetupDataDir {
        SUCCESS, PLAYLIST_ERROR, MKDIRS_ERROR
    }

    enum class EEditPlaylist {
        SUCCESS, FAILED_PLAYLIST, FAILED_COMMENT
    }
}
