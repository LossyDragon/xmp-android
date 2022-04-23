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
import org.helllabs.android.xmp.ui.search.ModArchiveConstants
import org.helllabs.android.xmp.ui.search.ModArchiveConstants.isSupported
import org.helllabs.android.xmp.util.Files
import org.helllabs.android.xmp.util.PrefManager
import org.helllabs.android.xmp.util.logE
import org.helllabs.android.xmp.util.logI

data class ModuleState(
    val module: ModuleResult? = null,
    val moduleExists: Boolean = false,
    val moduleSupported: Boolean = true,
    val softError: String? = null,
)

sealed class ModuleEvent {
    object RandomModule : ModuleEvent()
    object DeleteModule : ModuleEvent()
    object ExistingModule : ModuleEvent()
    data class Module(val id: Int) : ModuleEvent()
    data class Download(val mod: String, val url: String, val file: String) : ModuleEvent()
}

sealed class ModuleUiState {
    data class Error(val error: String?) : ModuleUiState()
    data class Loading(val isLoading: Boolean) : ModuleUiState()
    data class Random(val enabled: Boolean) : ModuleUiState()
}

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
                        this@ModuleResultViewModel.logE(error)
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
            is ModuleEvent.DeleteModule -> deleteModule()
            is ModuleEvent.Download -> downloadModule(event.mod, event.url, event.file)
            is ModuleEvent.ExistingModule -> existingModule()
            is ModuleEvent.Module -> getModuleById(event.id)
            is ModuleEvent.RandomModule -> getRandomModule()
        }
    }

    private fun existingModule() {
        val module = state.value.module!!.module
        val file = Files.getDownloadPath(module)
        val url = module.url
        val mod = module.filename

        downloadModule(mod, url, file)
    }

    private fun deleteModule() {
        val result = Files.deleteModuleFile(state.value.module?.module!!)
        logI("Module deleted was: $result")
        _state.value = state.value.copy(
            moduleExists = doesModuleExist(state.value.module),
            moduleSupported = isModuleSupported(state.value.module)
        )
    }

    private fun getModuleById(id: Int) {
        viewModelScope.launch {
            _uiState.emit(ModuleUiState.Random(enabled = false))
            _uiState.emit(ModuleUiState.Loading(isLoading = true))

            try {
                val result = repository.getModuleById(id)
                _state.value = if (result.error != null) {
                    ModuleState(softError = result.error)
                } else {
                    saveModuleToHistory(result.module)
                    ModuleState(
                        module = result,
                        moduleExists = doesModuleExist(result),
                        moduleSupported = isModuleSupported(result)
                    )
                }

                _uiState.emit(ModuleUiState.Loading(isLoading = false))
            } catch (e: Exception) {
                this@ModuleResultViewModel.logE(e.stackTraceToString())
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
                    ModuleState(softError = result.error)
                } else {
                    saveModuleToHistory(result.module)
                    ModuleState(
                        module = result,
                        moduleExists = doesModuleExist(result),
                        moduleSupported = isModuleSupported(result)
                    )
                }

                _uiState.emit(ModuleUiState.Loading(isLoading = false))
            } catch (e: Exception) {
                this@ModuleResultViewModel.logE(e.stackTraceToString())
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
                        this@ModuleResultViewModel.logE(error.toString())
                        _uiState.emit(ModuleUiState.Error(error.toString()))
                    }
                }
            )
    }

    private fun doesModuleExist(result: ModuleResult?): Boolean {
        val file = Files.localFile(result?.module)
        return file?.exists() ?: false
    }

    private fun isModuleSupported(result: ModuleResult?): Boolean {
        return result?.module?.isSupported() ?: true
    }

    private suspend fun saveModuleToHistory(module: Module?) {
        if (module == null)
            return

        // Load history list first
        val searchHistory = PrefManager.getPreference(PrefManager.searchHistoryRequest).let {
            kotlin.runCatching {
                moshiAdapter.fromJson(it)
            }.getOrNull()
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
        PrefManager.dataStoreManager.editPreference(
            key = PrefManager.searchHistoryRequest.key,
            newValue = moshiAdapter.toJson(searchHistory)
        )
    }
}
