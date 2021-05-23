package org.helllabs.android.xmp.presentation.ui.fileBrowser

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.model.BreadCrumb
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.model.PlaylistItem.Companion.TYPE_DIRECTORY
import org.helllabs.android.xmp.model.PlaylistItem.Companion.TYPE_FILE
import org.helllabs.android.xmp.presentation.utils.playlist.PlaylistUtils
import org.helllabs.android.xmp.util.logE
import org.helllabs.android.xmp.util.logW

class FilelistViewModel : ViewModel() {

    private val _listState = MutableStateFlow<FileListState>(FileListState.None)
    val listState: StateFlow<FileListState> = _listState

    val currentFile = mutableStateOf("")
    val crumbState = mutableStateOf<List<BreadCrumb>>(listOf())

    fun getDirectoryList(file: File) {
        _listState.value = FileListState.Load

        currentFile.value = file.path
        getCrumbTrails(file)

        if (!file.exists()) {
            _listState.value = FileListState.NotFound
            logW("File ${file.name} was not found.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val fileList = mutableListOf<PlaylistItem>()
            _listState.value = try {
                file.listFiles()
                    ?.filter { !it.isHidden }
                    ?.forEach { file ->
                        fileList.add(
                            PlaylistItem(
                                type = if (file.isDirectory) TYPE_DIRECTORY else TYPE_FILE,
                                name = file.name,
                                comment = getCommentData(file)
                            ).also { item ->
                                item.file = file
                                item.isPlayable = !item.isDirectory && item.comment != null // :)
                            }
                        )
                    }

                fileList.sort()
                PlaylistUtils.renumberIds(fileList)
                FileListState.Loaded(fileList)
            } catch (e: Exception) {
                logE("Error: ${e.localizedMessage}")
                FileListState.Error(e.localizedMessage)
            }
        }
    }

    private fun getCrumbTrails(file: File) {
        val crumbList = mutableListOf<BreadCrumb>()
        var currentDir: File? = file
        do {
            currentDir?.let {
                crumbList.add(BreadCrumb(name = it.name, path = it.path))
            }
            currentDir = currentDir?.parentFile
        } while (currentDir?.parentFile != null)

        // We'll reverse it here instead of the composable, it animates better.
        crumbState.value = crumbList.reversed()
    }

    fun recursiveList(file: File?): List<String> {
        _listState.value = FileListState.Load

        if (file == null) {
            logW("file was null")
            _listState.value = FileListState.AllFiles
            return emptyList()
        }

        return walkDownPath(file)
    }

    private fun getCommentData(file: File): String? {
        var commentData: String? = null
        if (!file.isDirectory) {
            val modInfo = ModInfo()
            if (Xmp.testModule(file.path, modInfo))
                commentData = modInfo.type
        }
        return commentData
    }

    private fun walkDownPath(file: File): List<String> {
        val list = file
            .walkTopDown()
            .filter { it.isFile }
            .map { it.path }
            .sortedBy { it.toLowerCase(Locale.getDefault()) }
            .toList()

        _listState.value = FileListState.AllFiles
        return list
    }

    sealed class FileListState {
        object None : FileListState()
        object Load : FileListState()
        object AllFiles : FileListState()
        object NotFound : FileListState()
        class Error(val error: String?) : FileListState()
        class Loaded(val list: List<PlaylistItem>) : FileListState()
    }
}
