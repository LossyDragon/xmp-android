package org.helllabs.android.xmp.compose.ui.filelist

import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazygeniouz.dfc.file.DocumentFileCompat
import java.text.DateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.FileItem
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber

// Bread crumbs are the back bone of the file explorer. :)
@Stable
data class BreadCrumb(val name: String, val path: DocumentFileCompat?, val enabled: Boolean = false)

// State class for UI related stuff
@Stable
data class FileListState(
    val crumbs: List<BreadCrumb> = listOf(),
    val isLoading: Boolean = false,
    val isLoop: Boolean = false,
    val isShuffle: Boolean = false,
    val lastScrollPosition: Int = 0,
    val list: List<FileItem> = listOf()
)

@Stable
class FileListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FileListState())
    val uiState = _uiState.asStateFlow()

    private val _softError = MutableSharedFlow<String>()
    val softError = _softError.asSharedFlow()

    private val currentPath: DocumentFileCompat?
        get() {
            val crumbs = uiState.value.crumbs
            return if (crumbs.isEmpty()) null else crumbs.last().path
        }

    val playlistList: MutableStateFlow<List<Playlist>> = MutableStateFlow(listOf())
    val playlistChoice: MutableStateFlow<DocumentFileCompat?> = MutableStateFlow(null)
    val deleteDirChoice: MutableStateFlow<DocumentFileCompat?> = MutableStateFlow(null)
    val deleteFileChoice: MutableStateFlow<DocumentFileCompat?> = MutableStateFlow(null)

    init {
        _uiState.update {
            it.copy(
                isShuffle = PrefManager.shuffleMode,
                isLoop = PrefManager.loopMode
            )
        }

        StorageManager.getModDirectory().onSuccess { dfc ->
            Timber.d("Initial Path: ${dfc.uri}")
            onNavigate(dfc)
        }.onFailure {
            Timber.e(it)
        }
    }

    fun setScrollPosition(value: Int) {
        _uiState.update { it.copy(lastScrollPosition = value) }
    }

    /**
     * Handle back presses
     * @return *true* if successful, otherwise false
     */
    fun onBackPressed(): Boolean {
        val currentCrumb = _uiState.value.crumbs.last().path

        currentCrumb?.parentFile?.let {
            onNavigate(it)
            return true
        }

        return false
    }

    /**
     * Play all valid files
     */
    suspend fun onAllFiles(): List<Uri> = withContext(Dispatchers.IO) {
        _uiState.update { it.copy(isLoading = true) }

        val list = StorageManager
            .walkDownDirectory(currentPath!!.uri, false)
        // .filter(Xmp::testFromFd)

        _uiState.update { it.copy(isLoading = false) }

        list
    }

    fun onLoop(value: Boolean) {
        PrefManager.loopMode = value
        _uiState.update { it.copy(isLoop = value) }
    }

    fun onShuffle(value: Boolean) {
        PrefManager.shuffleMode = value
        _uiState.update { it.copy(isShuffle = value) }
    }

    fun onRefresh() {
        onNavigate(currentPath)
    }

    fun onRestore() {
        onNavigate(uiState.value.crumbs.last().path!!.parentFile)
    }

    fun onNavigate(modDir: DocumentFileCompat?) {
        if (modDir == null) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            Timber.d("Path: ${modDir.uri}")

            // Rebuild our bread crumbs
            val crumbs = mutableListOf<BreadCrumb>()
            var docFile: DocumentFileCompat? = modDir
            while (docFile != null) {
                val crumb = BreadCrumb(
                    name = docFile.name,
                    path = docFile,
                    enabled = docFile.canRead()
                )

                crumbs.add(crumb)
                docFile = docFile.parentFile
            }
            _uiState.update { it.copy(crumbs = crumbs.reversed()) }

            val list = modDir.listFiles().map { file ->
                val item = if (file.isDirectory()) {
                    FileItem(
                        name = file.name,
                        comment = "",
                        docFile = file
                    )
                } else {
                    val date = DateFormat
                        .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
                        .format(file.lastModified)

                    FileItem(
                        name = file.name,
                        comment = "$date (${file.length / 1024} kB)",
                        docFile = file
                    )
                }

                item
            }.sorted()

            _uiState.update { it.copy(list = list, isLoading = false, lastScrollPosition = 0) }
        }
    }

    fun addToPlaylist(index: Int) {
        val choice = playlistList.value[index]
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            if (playlistChoice.value == null) {
                _softError.emit("Playlist choice is null")
                _uiState.update { it.copy(isLoading = false) }
                playlistChoice.value = null
                return@launch
            }

            val manager = PlaylistManager()
            if (!manager.load(choice.uri)) {
                _softError.emit("Playlist manager failed to load playlist")
                _uiState.update { it.copy(isLoading = false) }
                playlistChoice.value = null
                return@launch
            }

            val modInfo = ModInfo()
            if (playlistChoice.value!!.isFile()) {
                if (!Xmp.testFromFd(playlistChoice.value!!.uri, modInfo)) {
                    _softError.emit("Failed to validate file")
                    _uiState.update { it.copy(isLoading = false) }
                    playlistChoice.value = null
                    return@launch
                }

                val playlist = PlaylistItem(
                    name = modInfo.name,
                    type = modInfo.type,
                    uri = playlistChoice.value!!.uri
                )
                val list = listOf(playlist)
                val res = manager.add(list)

                if (!res) {
                    _softError.emit("Couldn't add module to playlist")
                }
            } else if (playlistChoice.value!!.isDirectory()) {
                val list = mutableListOf<PlaylistItem>()
                StorageManager.walkDownDirectory(playlistChoice.value!!.uri, false).forEach { uri ->
                    if (!Xmp.testFromFd(uri, modInfo)) {
                        Timber.w("Invalid playlist item $uri")
                        return@forEach
                    }

                    val playlist = PlaylistItem(
                        name = modInfo.name.ifEmpty {
                            StorageManager.getFileName(uri)
                        } ?: "",
                        type = modInfo.type,
                        uri = uri
                    )

                    list.add(playlist)
                }

                if (list.isEmpty()) {
                    _softError.emit("Empty directory")
                    _uiState.update { it.copy(isLoading = false) }
                    playlistChoice.value = null
                    return@launch
                }

                val res = manager.add(list)
                if (!res) {
                    _softError.emit("Couldn't add modules to playlist")
                }
            }

            _uiState.update { it.copy(isLoading = false) }
            playlistChoice.value = null
        }
    }

    fun getItems(): List<Uri> = _uiState.value.list.map { it.docFile!!.uri }

    // Get non directory items and count the number of directories
    fun getFileItems(): Pair<Int, List<Uri>> {
        val (files, dirs) = _uiState.value.list.partition { it.isFile }

        return Pair(dirs.size, files.map { it.docFile!!.uri })
    }

    fun dropDownAddToPlaylist(docFile: DocumentFileCompat? = null) {
        playlistList.value = PlaylistManager.listPlaylists()
        playlistChoice.value = docFile ?: currentPath
    }

    fun dropDownDelete(item: FileItem) {
        if (item.isFile) {
            deleteFileChoice.value = item.docFile
        } else {
            deleteDirChoice.value = item.docFile
        }
    }

    fun clearDeleteDir() {
        deleteDirChoice.value = null
    }

    fun clearFileDir() {
        deleteFileChoice.value = null
    }

    fun deleteFile(): Boolean = StorageManager.deleteFileOrDirectory(deleteFileChoice.value?.uri)

    fun deleteDir(): Boolean = StorageManager.deleteFileOrDirectory(deleteDirChoice.value?.uri)

    fun getFileName(): String = StorageManager.getFileName(deleteFileChoice.value?.uri).orEmpty()

    fun getDirName(): String = StorageManager.getFileName(deleteDirChoice.value?.uri).orEmpty()

    fun clearPlaylist() {
        playlistChoice.value = null
    }
}
