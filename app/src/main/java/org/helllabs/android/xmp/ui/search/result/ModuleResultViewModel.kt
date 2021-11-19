package org.helllabs.android.xmp.ui.search.result

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.JsonAdapter
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.FetchObserver
import com.tonyodev.fetch2core.Reason
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.api.Repository
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.ModuleResult
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.search.ModArchiveConstants
import org.helllabs.android.xmp.ui.search.ModArchiveConstants.isSupported
import org.helllabs.android.xmp.util.FileUtils
import org.helllabs.android.xmp.util.logE

@HiltViewModel
class ModuleResultViewModel
@Inject constructor(
    private val repository: Repository,
    private val fetchDownloader: Fetch,
    private val moshiAdapter: JsonAdapter<List<Module>>
) : ViewModel() {

    private val _uiState = MutableSharedFlow<ModuleUiState>()
    val uiState: SharedFlow<ModuleUiState> = _uiState.asSharedFlow()

    private val _state = mutableStateOf(ModuleState())
    val state: State<ModuleState> = _state

    private var request: Request? = null

    private val fetchObserver = object : FetchObserver<Download> {
        override fun onChanged(data: Download, reason: Reason) {
            viewModelScope.launch {
                if (request!!.id == data.id) {
                    when (data.status) {
                        Status.COMPLETED -> {
                            _uiState.emit(ModuleUiState.Loading(false))
                            _state.value = state.value.copy(
                                moduleExists = doesModuleExist(state.value.module),
                                moduleSupported = isModuleSupported(state.value.module)
                            ) // 👍
                        }
                        Status.CANCELLED,
                        Status.FAILED -> _uiState.emit(ModuleUiState.Loading(false))
                        Status.QUEUED,
                        Status.DOWNLOADING -> _uiState.emit(ModuleUiState.Loading(true))
                        else -> Unit // Don't care about the rest
                    }
                    if (data.error != Error.NONE) {
                        val error = data.error.toString() + "\n" + reason
                        _uiState.emit(ModuleUiState.Error(error))
                    }
                }
            }
        }
    }

    fun attachObserver() {
        if (request != null)
            fetchDownloader.attachFetchObserversForDownload(request!!.id, fetchObserver)
    }

    fun removeObserver() {
        if (request != null)
            fetchDownloader.removeFetchObserversForDownload(request!!.id, fetchObserver)
    }

    fun removeFetch() {
        fetchDownloader.close()
    }

    fun onEvent(event: ModuleEvent) {
        when (event) {
            is ModuleEvent.Module -> getModuleById(event.id)
            is ModuleEvent.RandomModule -> getRandomModule()
            is ModuleEvent.DownloadModule -> downloadModule(event.mod, event.url, event.file)
        }
    }

    private fun getModuleById(id: Int) {
        viewModelScope.launch {
            _uiState.emit(ModuleUiState.Random(enabled = false))
            _uiState.emit(ModuleUiState.Loading(isLoading = true))

            try {
                val result = repository.getModuleById(id)
                _state.value = if (result.error != null) {
                    _uiState.emit(ModuleUiState.Loading(isLoading = false))
                    ModuleState(softError = result.error)
                } else {
                    _uiState.emit(ModuleUiState.Loading(isLoading = false))
                    saveModuleToHistory(result.module)
                    ModuleState(
                        module = result,
                        moduleExists = doesModuleExist(result),
                        moduleSupported = isModuleSupported(result)
                    )
                }
            } catch (e: Exception) {
                _uiState.emit(ModuleUiState.Error(e.localizedMessage))
            }
        }
    }

    private fun getRandomModule() {
        viewModelScope.launch {
            _uiState.emit(ModuleUiState.Random(enabled = true))
            _uiState.emit(ModuleUiState.Loading(isLoading = true))

            try {
                val result = repository.getRandomModule()
                _state.value = if (!result.error.isNullOrBlank()) {
                    _uiState.emit(ModuleUiState.Loading(isLoading = false))
                    ModuleState(softError = result.error)
                } else {
                    _uiState.emit(ModuleUiState.Loading(isLoading = false))
                    saveModuleToHistory(result.module)
                    ModuleState(
                        module = result,
                        moduleExists = doesModuleExist(result),
                        moduleSupported = isModuleSupported(result)
                    )
                }
            } catch (e: Exception) {
                _uiState.emit(ModuleUiState.Error(e.localizedMessage))
            }
        }
    }

    private fun downloadModule(mod: String, url: String, file: String) {
        val pathFile = File(file)
        pathFile.mkdirs()

        request = Request(url, "$pathFile/$mod")

        fetchDownloader.attachFetchObserversForDownload(request!!.id, fetchObserver)
            .enqueue(
                request!!,
                { updatedRequests -> request = updatedRequests },
                { error ->
                    logE("enqueue: $error")
                    viewModelScope.launch {
                        _uiState.emit(ModuleUiState.Error(error.toString()))
                    }
                }
            )
    }

    private fun doesModuleExist(result: ModuleResult?): Boolean {
        val file = FileUtils.localFile(result?.module)
        return file?.exists() ?: false
    }

    private fun isModuleSupported(result: ModuleResult?): Boolean {
        return result?.module?.isSupported() ?: true
    }

    private fun saveModuleToHistory(module: Module?) {
        if (module == null)
            return

        // Load history list first
        val searchHistory = PrefManager.searchHistory?.let {
            moshiAdapter.fromJson(it)
        }.orEmpty().toMutableList()

        // Check to see if the module has been searched before. Skip if true
        searchHistory.forEach {
            if (it.id == module.id)
                return
        }

        // Remove the oldest item if history length is reached
        if (searchHistory.size >= ModArchiveConstants.HISTORY_LENGTH)
            searchHistory.removeFirst()

        // Add the current module into the history
        searchHistory.add(module)

        // Convert into JSON and save it
        PrefManager.searchHistory = moshiAdapter.toJson(searchHistory)
    }

    sealed class ModuleUiState {
        data class Error(val error: String?) : ModuleUiState()
        data class Loading(val isLoading: Boolean) : ModuleUiState()
        data class Random(val enabled: Boolean) : ModuleUiState()
    }
}
