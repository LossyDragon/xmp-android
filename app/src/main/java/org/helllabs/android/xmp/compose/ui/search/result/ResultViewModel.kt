package org.helllabs.android.xmp.compose.ui.search.result

import android.os.Build
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lazygeniouz.dfc.file.DocumentFileCompat
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.buffer
import okio.sink
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.api.Repository
import org.helllabs.android.xmp.core.Constants.isSupported
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.Resource
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.model.ModuleResult
import timber.log.Timber

@Stable
sealed class DownloadStatus {
    data class Error(val error: Exception) : DownloadStatus()
    data class ErrorMsg(val error: String) : DownloadStatus()
    data class Progress(val percent: Float) : DownloadStatus()
    data object Loading : DownloadStatus()
    data object None : DownloadStatus()
    data object Success : DownloadStatus()
}

@Stable
data class ModuleResultState(
    val isRandom: Boolean = false,
    val isLoading: Boolean = false,
    val softError: String? = null,
    val hardError: String? = null,
    val module: ModuleResult? = null,
    val moduleExists: Boolean = false,
    val moduleSupported: Boolean = true,
    val downloadStatus: DownloadStatus = DownloadStatus.None
)

@Stable
class ResultViewModelFactory : ViewModelProvider.Factory {
    private val repository = Repository(XmpApplication.modArchiveModule.apiHelper)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ResultViewModel(XmpApplication.modArchiveModule.httpClient, repository) as T
}

class ResultViewModel(private val httpClient: HttpClient, private val repository: Repository) :
    ViewModel() {

    private val _uiState = MutableStateFlow(ModuleResultState())
    val uiState = _uiState.asStateFlow()

    private var currentDownloadJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        currentDownloadJob?.cancel()
    }

    fun showSoftError(message: String) {
        _uiState.update { it.copy(softError = message) }
    }

    fun downloadModule(mod: Module, docFile: DocumentFileCompat) {
        if (currentDownloadJob != null && currentDownloadJob!!.isActive) {
            currentDownloadJob!!.cancel()
        }

        currentDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        downloadStatus = DownloadStatus.Loading
                    )
                }

                val modDoc = docFile.createFile("application/octet-stream", mod.filename)

                if (modDoc == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            downloadStatus = DownloadStatus.ErrorMsg(
                                "Failed to create file to download"
                            )
                        )
                    }
                    return@launch
                }

                val outputStream = modDoc.uri.let { uri ->
                    val context = XmpApplication.instance!!.applicationContext
                    context.contentResolver.openOutputStream(uri)
                }

                if (outputStream == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            downloadStatus = DownloadStatus.ErrorMsg(
                                "Failed to open output stream"
                            )
                        )
                    }
                    return@launch
                }

                val response = httpClient.get(mod.url)
                val contentLength = response.contentLength() ?: 0L

                val channel = response.bodyAsChannel()
                outputStream.use { outStream ->
                    val sink = outStream.sink().buffer()
                    var totalBytesRead = 0L
                    val buffer = ByteArray(8192)

                    while (!channel.isClosedForRead) {
                        val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                        if (bytesRead < 0) break

                        sink.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Update progress
                        if (contentLength > 0) {
                            val progress = (totalBytesRead * 100 / contentLength).toFloat()
                            _uiState.update {
                                it.copy(downloadStatus = DownloadStatus.Progress(progress))
                            }
                        }
                    }

                    sink.flush()
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        downloadStatus = DownloadStatus.Success
                    )
                }
            } catch (e: Exception) {
                Timber.e(e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        downloadStatus = DownloadStatus.Error(e)
                    )
                }
            } finally {
                update()
            }
        }
    }

    fun getModuleById(id: Int) {
        if (id < 0) {
            getRandomModule()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRandom = false, isLoading = true) }
            repository.getModuleById(id).collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val result = resource.data
                        if (result != null) {
                            saveModuleToHistory(result.module)
                            _uiState.update {
                                it.copy(
                                    module = result,
                                    moduleExists = doesModuleExist(result),
                                    moduleSupported = isModuleSupported(result),
                                    isLoading = false
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    softError = "No data returned",
                                    isLoading = false
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                softError = resource.message,
                                isLoading = false
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun getRandomModule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRandom = true, isLoading = true) }

            repository.getRandomModule().collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val result = resource.data
                        if (result != null) {
                            saveModuleToHistory(result.module)
                            _uiState.update {
                                it.copy(
                                    module = result,
                                    moduleExists = doesModuleExist(result),
                                    moduleSupported = isModuleSupported(result),
                                    isLoading = false
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    softError = "No data returned",
                                    isLoading = false
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                softError = resource.message,
                                isLoading = false
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun deleteModule() {
        val result = StorageManager.deleteModule(_uiState.value.module?.module)
        Timber.d("Module deleted was: ${result.isSuccess}")
        update()
    }

    fun update() {
        _uiState.update {
            it.copy(
                moduleExists = doesModuleExist(_uiState.value.module),
                moduleSupported = isModuleSupported(_uiState.value.module)
            )
        }
    }

    private fun doesModuleExist(result: ModuleResult?): Boolean {
        val moduleExist = StorageManager.doesModuleExist(result?.module).getOrNull() ?: return false

        Timber.d("Does module exist? -> ${moduleExist.isFile()}")
        return moduleExist.isFile()
    }

    private fun isModuleSupported(result: ModuleResult?): Boolean {
        val supported = result?.module?.isSupported() ?: true
        Timber.d("Is module supported? -> $supported")
        return supported
    }

    private fun saveModuleToHistory(module: Module) {
        val history = mutableListOf<Module>()

        // We only care about a few things to store.
        PrefManager.searchHistory.map {
            Module(
                id = it.id,
                format = it.format,
                songtitle = it.songtitle,
                artistInfo = it.artistInfo,
                bytes = it.bytes
            )
        }.also { history.addAll(it) }

        if (history.any { it.id == module.id }) {
            Timber.i("Module ${module.id} already exists in history. Skipping")
            return
        }

        val moduleToAdd = Module(
            id = module.id,
            format = module.format,
            songtitle = module.songtitle,
            artistInfo = module.artistInfo,
            bytes = module.bytes
        )
        history.add(moduleToAdd)

        if (history.size >= 50) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                history.removeFirst()
            } else {
                history.removeAt(0)
            }
        }

        PrefManager.searchHistory = history
    }
}
