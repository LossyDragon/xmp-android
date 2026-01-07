package org.helllabs.android.xmp.compose.ui.explorer

import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazygeniouz.dfc.file.DocumentFileCompat
import java.text.DateFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.helllabs.android.xmp.core.PlaylistManager
import org.helllabs.android.xmp.core.PlaylistManager.Companion.addItem
import org.helllabs.android.xmp.core.PlaylistManager.Companion.addItems
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.FileItem
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber

@Immutable
data class BreadCrumb(
    val name: String,
    val path: Uri?,
    val enabled: Boolean = true,
    val scrollPosition: Int = 0
)

@Immutable
data class ExplorerState(
    val crumbs: ImmutableList<BreadCrumb> = persistentListOf(),
    val isLoading: Boolean = true,
    val isLoop: Boolean = false,
    val isShuffle: Boolean = false,
    val list: List<FileItem> = emptyList()
)

class ExplorerViewModel(
    private val playlistManager: PlaylistManager,
    private val prefManager: PrefManager,
    private val storageManager: StorageManager
) : ViewModel() {

    val listState: LazyListState by mutableStateOf(LazyListState(0, 0))

    private val _uiState = MutableStateFlow(ExplorerState())
    val uiState = _uiState.asStateFlow()

    private val _softError = MutableSharedFlow<String>()
    val softError = _softError.asSharedFlow()

    private val currentPath: Uri?
        get() = uiState.value.crumbs.lastOrNull()?.path

    val playlistList = MutableStateFlow<List<Pair<Playlist, Uri>>>(emptyList())
    val playlistChoice = MutableStateFlow<Uri?>(null)
    val deleteDirChoice = MutableStateFlow<Uri?>(null)
    val deleteFileChoice = MutableStateFlow<Uri?>(null)

    private var navigationJob: Job? = null

    // Cache directory contents to improve performance on back navigation
    private val directoryCache = object : LinkedHashMap<String, List<FileItem>>(
        /* initialCapacity = */ 16,
        /* loadFactor = */ 0.75f,
        /* accessOrder = */ true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, List<FileItem>>?
        ): Boolean {
            return size > 50
        }
    }

    private val scrollPositionCache = mutableMapOf<String, Int>()

    init {
        viewModelScope.launch {
            storageManager.getExplorerRootDirectory().onSuccess { dfc ->
                Timber.d("Initial Path: ${dfc.uri}")
                onNavigate(dfc.uri)
                _uiState.update {
                    it.copy(
                        isShuffle = prefManager.getShuffleMode(),
                        isLoop = prefManager.getLoopMode(),
                    )
                }
            }.onFailure { error ->
                Timber.e(error)
                _softError.emit("Failed to access initial directory:\n${error.message}")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared")
        directoryCache.clear()
        scrollPositionCache.clear()
    }

    private fun saveScrollPosition() {
        val currentIndex = listState.firstVisibleItemIndex
        currentPath?.toString()?.let { path ->
            scrollPositionCache[path] = currentIndex
        }
    }

    /**
     * Check if back navigation is possible
     */
    fun canNavigateBack(): Boolean = _uiState.value.crumbs.size > 1

    /**
     * Navigate back to parent directory
     * @return true if navigation was performed, false if already at root
     */
    fun navigateBack(): Boolean {
        val crumbs = _uiState.value.crumbs
        if (crumbs.size <= 1) return false

        val parentCrumb = crumbs[crumbs.size - 2]
        onNavigate(parentCrumb.path)
        return true
    }

    /**
     * Get all files recursively from current directory
     */
    suspend fun onAllFiles(): List<Uri> = withContext(Dispatchers.IO) {
        _uiState.update { it.copy(isLoading = true) }
        val list = storageManager.walkDownDirectory(currentPath, includeDirectories = false)
        _uiState.update { it.copy(isLoading = false) }
        list
    }

    fun onLoop(value: Boolean) {
        viewModelScope.launch {
            prefManager.setLoopMode(value)
            _uiState.update { it.copy(isLoop = value) }
        }
    }

    fun onShuffle(value: Boolean) {
        viewModelScope.launch {
            prefManager.setShuffleMode(value)
            _uiState.update { it.copy(isShuffle = value) }
        }
    }

    fun onRefresh() {
        currentPath?.toString()?.let { directoryCache.remove(it) }
        onNavigate(currentPath)
    }

    fun onRestore() {
        onNavigate(currentPath)
    }

    fun onNavigate(uri: Uri?) {
        if (uri == null) return

        saveScrollPosition()

        navigationJob?.cancel()
        navigationJob = viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isLoading = true) }
            }

            try {
                val targetDir = storageManager.getDocumentFileFromUri(uri)

                if (targetDir == null || !targetDir.exists()) {
                    _softError.emit("Directory does not exist")
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    return@launch
                }

                if (!targetDir.isDirectory()) {
                    _softError.emit("Not a directory")
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    return@launch
                }

                // Get saved scroll position for this directory (0 if not visited before)
                val cacheKey = uri.toString()
                val savedScrollPosition = scrollPositionCache[cacheKey] ?: 0

                Timber.d("Navigating to $cacheKey, scroll position: $savedScrollPosition")

                val cachedList = directoryCache[cacheKey]

                if (cachedList != null) {
                    val crumbs = buildBreadcrumbsFromUri(targetDir)
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                crumbs = crumbs,
                                list = cachedList,
                                isLoading = false,
                            )
                        }
                    }
                    return@launch
                }

                val crumbs = buildBreadcrumbsFromUri(targetDir)

                val childUris = storageManager.listDirectoryContents(uri)

                val list = childUris.mapNotNull { childUri ->
                    val docFile = storageManager.getDocumentFileFromUri(childUri)
                        ?: return@mapNotNull null

                    if (docFile.isDirectory()) {
                        FileItem(
                            name = docFile.name,
                            comment = "",
                            uri = childUri,
                            isDirectory = true,
                        )
                    } else {
                        val date = DateFormat
                            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
                            .format(docFile.lastModified)

                        FileItem(
                            name = docFile.name,
                            comment = "$date (${docFile.length / 1024} kB)",
                            uri = childUri,
                            isDirectory = false
                        )
                    }
                }

                directoryCache[cacheKey] = list

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            crumbs = crumbs,
                            list = list,
                            isLoading = false,
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error navigating to $uri")
                _softError.emit("Error accessing directory: ${e.message}")
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun buildBreadcrumbsFromUri(currentDoc: DocumentFileCompat): ImmutableList<BreadCrumb> {
        val crumbs = mutableListOf<BreadCrumb>()
        val currentUri = currentDoc.uri

        try {
            val docId = DocumentsContract.getDocumentId(currentUri)
            val colonIndex = docId.indexOf(':')

            if (colonIndex == -1) {
                crumbs.add(
                    BreadCrumb(
                        name = currentDoc.name,
                        path = currentUri
                    )
                )
                return crumbs.toPersistentList()
            }

            val storage = docId.take(colonIndex)
            val pathPart = docId.substring(colonIndex + 1)
            val segments = pathPart.split("/").filter { it.isNotEmpty() }

            for (i in segments.indices) {
                val pathUpToHere = segments.subList(0, i + 1).joinToString("/")
                val fullDocId = "$storage:$pathUpToHere"
                val uri = DocumentsContract.buildDocumentUriUsingTree(currentUri, fullDocId)

                crumbs.add(
                    BreadCrumb(
                        name = segments[i],
                        path = uri
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error building breadcrumbs")
            crumbs.add(
                BreadCrumb(
                    name = currentDoc.name,
                    path = currentUri
                )
            )
        }

        return crumbs.toPersistentList()
    }

    fun addToPlaylist(index: Int) {
        val choice = playlistList.value[index]
        val choiceUri = playlistChoice.value

        if (choiceUri == null) {
            viewModelScope.launch {
                _softError.emit("No file or directory selected")
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isLoading = true) }
            }

            try {
                var playlist = playlistManager.loadPlaylist(choice.second).getOrNull()

                if (playlist == null) {
                    _softError.emit("Failed to load playlist")
                    return@launch
                }

                val docFile = storageManager.getDocumentFileFromUri(choiceUri)

                if (docFile == null) {
                    _softError.emit("Invalid file or directory")
                    return@launch
                }

                if (docFile.isFile()) {
                    // Add single file
                    val modInfo = ModInfo()
                    if (!storageManager.testModule(choiceUri, modInfo)) {
                        _softError.emit("Invalid module file")
                        return@launch
                    }

                    val playlistItem = PlaylistItem(
                        name = modInfo.name.ifEmpty { docFile.name },
                        type = modInfo.type,
                        uri = choiceUri
                    )

                    playlist = playlist.addItem(playlistItem)
                } else if (docFile.isDirectory()) {
                    // Add all files in directory
                    val items = mutableListOf<PlaylistItem>()
                    val modInfo = ModInfo()

                    storageManager.walkDownDirectory(choiceUri, includeDirectories = false)
                        .forEach { uri ->
                            if (storageManager.testModule(uri, modInfo)) {
                                items.add(
                                    PlaylistItem(
                                        name = modInfo.name.ifEmpty {
                                            storageManager.getFileName(uri) ?: "Unknown"
                                        },
                                        type = modInfo.type,
                                        uri = uri
                                    )
                                )
                            } else {
                                Timber.w("Invalid module: $uri")
                            }
                        }

                    if (items.isEmpty()) {
                        _softError.emit("No valid modules found in directory")
                        return@launch
                    }

                    playlist = playlist.addItems(items)
                }

                playlistManager.savePlaylist(choice.second, playlist)
            } catch (e: Exception) {
                Timber.e(e, "Error adding to playlist")
                _softError.emit("Error: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoading = false) }
                }
                playlistChoice.value = null
            }
        }
    }

    fun getItems(): List<Uri> = _uiState.value.list.map { it.uri }

    fun getFileItems(): Pair<Int, List<Uri>> {
        val (files, dirs) = _uiState.value.list.partition { !it.isDirectory }
        return Pair(dirs.size, files.map { it.uri })
    }

    fun dropDownAddToPlaylist(uri: Uri? = null) {
        viewModelScope.launch {
            playlistList.value = playlistManager.listAllPlaylists().getOrDefault(emptyList())
            playlistChoice.value = uri ?: currentPath
        }
    }

    fun dropDownDelete(item: FileItem) {
        if (item.isDirectory) {
            deleteDirChoice.value = item.uri
        } else {
            deleteFileChoice.value = item.uri
        }
    }

    fun clearDeleteDir() {
        deleteDirChoice.value = null
    }

    fun clearFileDir() {
        deleteFileChoice.value = null
    }

    fun deleteFile(): Boolean {
        val result = storageManager.deleteFileOrDirectory(deleteFileChoice.value)
        if (result) {
            currentPath?.toString()?.let { directoryCache.remove(it) }
        }
        return result
    }

    fun deleteDir(): Boolean {
        val result = storageManager.deleteFileOrDirectory(deleteDirChoice.value)
        if (result) {
            currentPath?.toString()?.let { directoryCache.remove(it) }
        }
        return result
    }

    fun getFileName(): String = storageManager.getFileName(deleteFileChoice.value) ?: "Unknown"

    fun getDirName(): String = storageManager.getFileName(deleteDirChoice.value) ?: "Unknown"

    fun clearPlaylist() {
        playlistChoice.value = null
    }

    fun clearCache() {
        directoryCache.clear()
        scrollPositionCache.clear()
    }

    fun getScrollPosition(path: String): Int {
        return scrollPositionCache[path] ?: 0
    }
}
