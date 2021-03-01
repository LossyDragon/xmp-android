package org.helllabs.android.xmp.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.text.DateFormat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.helllabs.android.xmp.browser.playlist.PlaylistItem
import org.helllabs.android.xmp.browser.playlist.PlaylistItem.Companion.TYPE_DIRECTORY
import org.helllabs.android.xmp.browser.playlist.PlaylistItem.Companion.TYPE_FILE
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils
import org.helllabs.android.xmp.util.InfoCache
import org.helllabs.android.xmp.util.logW

class FilelistViewModel : ViewModel() {

    private val _listState = MutableStateFlow<FilelistState>(FilelistState.None)
    val listState: StateFlow<FilelistState> = _listState

    private val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)

    fun updateModList(dir: File?) {
        _listState.value = FilelistState.Load
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<PlaylistItem>()
            _listState.value = try {
                dir?.listFiles()?.forEach { file ->
                    val item: PlaylistItem = if (file.isDirectory) {
                        PlaylistItem(TYPE_DIRECTORY, file.name, "Directory") // TODO string
                    } else {
                        val comment = date.format(file.lastModified()) +
                            String.format(" (%d kB)", file.length() / 1024)
                        PlaylistItem(TYPE_FILE, file.name, comment)
                    }
                    item.file = file
                    list.add(item)
                }

                if (list.isEmpty()) {
                    FilelistState.Empty
                } else {
                    list.sort()
                    PlaylistUtils.renumberIds(list)
                    FilelistState.Loaded(list)
                }
            } catch (e: Exception) {
                FilelistState.Error(e.localizedMessage)
            }
        }
    }

    fun recursiveList(file: File?): List<String> {
        _listState.value = FilelistState.Load

        if (file == null) {
            logW("file was null")
            _listState.value = FilelistState.AllFiles
            return emptyList()
        }

        // TODO this blocks the UI
        return runBlocking {
            _listState.value = FilelistState.AllFiles
            walkDownPath(file)
        }
    }

    private fun walkDownPath(file: File): List<String> {

        val list = mutableListOf<String>()
        file.walkTopDown().forEach {
            if (it.isFile)
                list.add(it.path)
        }

        return list
    }

    fun clearCachedEntries(list: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            list.forEach {
                InfoCache.clearCache(it)
            }
        }
    }

    sealed class FilelistState {
        object None : FilelistState()
        object Load : FilelistState()
        object Empty : FilelistState()
        object AllFiles : FilelistState()
        class Error(val error: String?) : FilelistState()
        class Loaded(val list: List<PlaylistItem>) : FilelistState()
    }
}
