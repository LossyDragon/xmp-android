package org.helllabs.android.xmp.ui.explorer

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.service.*
import org.helllabs.android.xmp.ui.explorer.util.CachingDocumentFile
import org.helllabs.android.xmp.ui.explorer.util.Event
import org.helllabs.android.xmp.ui.explorer.util.toCachingList
import org.helllabs.android.xmp.util.preferences.Manager.dataStoreManager
import org.helllabs.android.xmp.util.preferences.requestMediaPath
import timber.log.Timber

data class ExplorerScreenState(
    val items: List<CachingDocumentFile> = emptyList(),
    val backStack: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val currentModule: Module? = null,
    val playingState: PlayingState = PlayingState.NONE
) {
    val isPlayerBarVisible =
        currentModule != null &&
            (playingState == PlayingState.PLAYING || playingState == PlayingState.PAUSED)

    val isMusicPlaying = playingState == PlayingState.PLAYING
}

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    application: Application,
    private val moshiAdapter: JsonAdapter<Playlist>,
    private val useCase: PlayerUseCase,
    private val playlistDir: File?,
) : AndroidViewModel(application) {

    private val _uiState = mutableStateOf(ExplorerScreenState())
    val uiState: State<ExplorerScreenState> = _uiState

    private val _openDirectory = MutableSharedFlow<Event<CachingDocumentFile>>()
    val openDirectory = _openDirectory.asSharedFlow()

    private val _openDocument = MutableSharedFlow<Event<CachingDocumentFile>>()
    val openDocument = _openDocument.asSharedFlow()

    private val _noValidUri = MutableSharedFlow<Boolean>()
    val noValidUri = _noValidUri.asSharedFlow()

    init {
        viewModelScope.launch {
            val mediaPath: String = dataStoreManager.getPreference(requestMediaPath)

            // First time?
            if (mediaPath.isEmpty()) {
                _noValidUri.emit(true)
                return@launch
            }

            loadDirectory(mediaPath.toUri(), true)

            useCase.subscribeToService(EXPLORER_ROOT_ID)
        }
    }

    fun loadDirectory(directoryUri: Uri, addToStack: Boolean = false) {
        _uiState.value = uiState.value.copy(isLoading = true)
        val documentsTree = DocumentFile.fromTreeUri(getApplication(), directoryUri) ?: return
        val childDocuments = documentsTree.listFiles().toCachingList()

        viewModelScope.launch {
            val sortedDocuments = withContext(Dispatchers.IO) {
                childDocuments.toMutableList().apply {
                    sortBy { it.name?.lowercase() }
                    sortBy { !it.isDirectory }
                    forEachIndexed { index, cachingDocumentFile ->
                        cachingDocumentFile.setIndex(index)
                    }
                }
            }

            if (addToStack) {
                val backStack = uiState.value.backStack.toMutableList().apply { add(directoryUri) }
                _uiState.value = uiState.value.copy(backStack = backStack)
            }

            _uiState.value = uiState.value.copy(items = sortedDocuments, isLoading = false)
        }
    }

    fun documentClicked(clickedDocument: CachingDocumentFile) = viewModelScope.launch {
        Timber.d("documentClicked: ${clickedDocument.uri}")
        if (clickedDocument.isDirectory) {
            _openDirectory.emit(Event(clickedDocument))
        } else {
            _openDocument.emit(Event(clickedDocument))
        }
    }

    fun popBackStack() {
        val backStack = uiState.value.backStack.toMutableList().apply { removeLast() }
        _uiState.value = uiState.value.copy(backStack = backStack)

        loadDirectory(backStack.last())
    }

    /* Player Functions */
    fun onMusicItemPressed() = viewModelScope.launch {
        useCase.playFromMediaId("TODO TODO")
    }
}
