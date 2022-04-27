package org.helllabs.android.xmp.ui.playlists

import android.app.Application
import android.support.v4.media.MediaBrowserCompat
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.launch
import okio.buffer
import okio.source
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.service.PLAYLIST_ROOT_ID
import org.helllabs.android.xmp.service.PlayerUseCase
import timber.log.Timber

data class PlaylistScreenState(
    val playlists: List<MediaBrowserCompat.MediaItem> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    application: Application,
    private val moshiAdapter: JsonAdapter<Playlist>,
    private val useCase: PlayerUseCase,
    private val playlistDir: File?,
) : AndroidViewModel(application) {

    private val _uiState = mutableStateOf(PlaylistScreenState())
    val uiState: State<PlaylistScreenState> = _uiState

    init {
        getMediaBrowserPlaylist()
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared")
        useCase.unsubscribeToService(PLAYLIST_ROOT_ID)
    }

    private fun getMediaBrowserPlaylist() = viewModelScope.launch {
        _uiState.value = uiState.value.copy(isLoading = true)
        val resource = useCase.subscribeToService(PLAYLIST_ROOT_ID)

        Timber.d("Browser Result: $resource")

        val list = resource.data ?: emptyList()
        _uiState.value = uiState.value.copy(isLoading = false, playlists = list)
        useCase.unsubscribeToService(PLAYLIST_ROOT_ID)
    }

    fun addPlaylist(name: String, comment: String) {
        Timber.d("Add Playlist: $name, $comment")

        val emptyPlaylist = Playlist(
            name = name,
            comment = comment,
            data = emptyList()
        )

        val json = moshiAdapter.toJson(emptyPlaylist)
        File(playlistDir, "$name.json")
            .bufferedWriter()
            .use { out -> out.write(json) }
            .also { getMediaBrowserPlaylist() }
    }

    fun editPlaylist(oldname: String, name: String, comment: String): Boolean {
        Timber.d("Edit Playlist: $name, $comment")

        val src = File(playlistDir, "$oldname.json")

        // Step 1: Edit the contents
        val buffer = src.source().buffer()
        var contents = moshiAdapter.fromJson(buffer)!!
        buffer.close()

        contents = contents.copy(name = name, comment = comment)

        src.bufferedWriter()
            .use { out -> out.write(moshiAdapter.toJson(contents)) }

        // Step 2: Rename the file
        val dest = File(playlistDir, "$name.json")

        val success = src.renameTo(dest)
        Timber.d("Playlist renamed: $success")

        getMediaBrowserPlaylist()
        return success
    }

    fun deletePlaylist(name: String): Boolean {
        Timber.d("Delete Playlist: $name")

        val src = File(playlistDir, "$name.json")
        val deleted = src.delete()

        Timber.d("Playlist deleted: $deleted")

        getMediaBrowserPlaylist()
        return deleted
    }
}
