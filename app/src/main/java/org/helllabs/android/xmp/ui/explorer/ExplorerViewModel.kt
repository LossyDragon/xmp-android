package org.helllabs.android.xmp.ui.explorer

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.model.BreadCrumb
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.model.PlaylistType
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.util.PlaylistUtils
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.logE
import org.helllabs.android.xmp.util.logW

class ExplorerViewModel : ViewModel() {

    private val _listState = MutableStateFlow<FileListState>(FileListState.None)
    val listState: StateFlow<FileListState> = _listState

    val currentFile = mutableStateOf("")
    val crumbState = mutableStateOf<List<BreadCrumb>>(listOf())

    init {
        getDirectoryList(File(PrefManager.mediaPath!!))
    }

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
                                type =
                                if (file.isDirectory) PlaylistType.TYPE_DIRECTORY
                                else PlaylistType.TYPE_FILE,
                                name = file.name,
                                comment = getCommentData(file),
                                file = file
                            ).also { item ->
                                item.isPlayable = !item.isDirectory() && item.comment != null // :)
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
        if (file == null) {
            logW("file was null")
            return emptyList()
        }

        val list = file
            .walkTopDown()
            .filter { it.isFile && Xmp.testModule(it.path) } // slow???
            .map { it.path }
            .sortedBy { it.lowercase(Locale.getDefault()) }
            .toList()

        logD("Recursive list: $list")
        return list
    }

    private suspend fun getCommentData(file: File): String? {
        var commentData: String? = null
        if (!file.isDirectory) {
            withContext(Dispatchers.IO) {
                val modInfo = ModInfo()
                if (Xmp.testModule(file.path, modInfo))
                    commentData = modInfo.type
            }
        }
        return commentData
    }

    sealed class FileListState {
        object None : FileListState()
        object Load : FileListState()
        object NotFound : FileListState()
        class Error(val error: String?) : FileListState()
        class Loaded(val list: List<PlaylistItem>) : FileListState()
    }
}
