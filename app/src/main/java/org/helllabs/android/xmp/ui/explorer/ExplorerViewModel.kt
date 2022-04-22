package org.helllabs.android.xmp.ui.explorer

import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.model.BreadCrumb
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.model.PlaylistType
import org.helllabs.android.xmp.util.PlaylistUtils
import org.helllabs.android.xmp.util.PrefManager
import org.helllabs.android.xmp.util.PrefManager.dataStoreManager
import org.helllabs.android.xmp.util.PrefManager.installExamplesRequest
import org.helllabs.android.xmp.util.PrefManager.loopModeRequest
import org.helllabs.android.xmp.util.PrefManager.shuffleModeRequest
import org.helllabs.android.xmp.util.logE

sealed class ExplorerEvent {
    data class DirectoryList(val file: File) : ExplorerEvent()
}

sealed class ExplorerUiState {
    object FileNotFound : ExplorerUiState()
    data class Error(val error: String?) : ExplorerUiState()
    data class Loading(val isLoading: Boolean) : ExplorerUiState()
}

data class ExplorerState(
    val list: List<PlaylistItem> = listOf(),
    val currentFile: String = "",
)

data class ExplorerCrumbState(
    val crumbList: List<BreadCrumb> = listOf()
)

class ExplorerViewModel : ViewModel() {

    private val _uiState = MutableSharedFlow<ExplorerUiState>()
    val uiState: SharedFlow<ExplorerUiState> = _uiState.asSharedFlow()

    private val _state = mutableStateOf(ExplorerState())
    val state: State<ExplorerState> = _state

    private val _crumbState = mutableStateOf(ExplorerCrumbState())
    val crumbState: State<ExplorerCrumbState> = _crumbState

    var isLoopMode: Boolean = false
        private set
    var isShuffleMode: Boolean = false
        private set
    var installExample: Boolean = false
        private set


    init {
        viewModelScope.launch {
            dataStoreManager.getPreferenceFlow(loopModeRequest).collect { value ->
                isLoopMode = value
            }
            dataStoreManager.getPreferenceFlow(shuffleModeRequest).collect { value ->
                isShuffleMode = value
            }
            dataStoreManager.getPreferenceFlow(installExamplesRequest).collect { value ->
                installExample = value
            }
        }
    }

    fun onEvent(event: ExplorerEvent) {
        when (event) {
            is ExplorerEvent.DirectoryList -> getDirectoryList(event.file)
        }
    }

    private fun getDirectoryList(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.emit(ExplorerUiState.Loading(isLoading = true))

            getCrumbTrails(file)

            if (!file.exists()) {
                _uiState.emit(ExplorerUiState.FileNotFound)
                _uiState.emit(ExplorerUiState.Loading(isLoading = false))
                return@launch
            }

            try {
                val files = file.listFiles().orEmpty()

                val fileList = files.map { file ->
                    PlaylistItem(
                        type = getFileType(file),
                        name = file.name,
                        comment = getCommentData(file).orEmpty(),
                        file = file
                    ).also { item ->
                        item.isPlayable = !item.isDirectory() && item.comment.isNotEmpty() // :)
                    }
                }.toMutableList()

                fileList.sort()
                PlaylistUtils.renumberIds(fileList)

                _state.value = ExplorerState(list = fileList, currentFile = file.path)
            } catch (e: Exception) {
                logE(e.localizedMessage ?: "An error as occurred")
                _uiState.emit(ExplorerUiState.Error(e.localizedMessage))
            } finally {
                _uiState.emit(ExplorerUiState.Loading(isLoading = false))
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
        _crumbState.value = crumbState.value.copy(crumbList = crumbList.reversed())
    }

    private fun getFileType(file: File): PlaylistType {
        return if (file.isDirectory) PlaylistType.TYPE_DIRECTORY else PlaylistType.TYPE_FILE
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

    fun setLoop(value: Boolean) {
        viewModelScope.launch {
            dataStoreManager.editPreference(loopModeRequest.key, value)
        }
    }

    fun setShuffle(value: Boolean) {
        viewModelScope.launch {
            dataStoreManager.editPreference(shuffleModeRequest.key, value)
        }
    }
}
