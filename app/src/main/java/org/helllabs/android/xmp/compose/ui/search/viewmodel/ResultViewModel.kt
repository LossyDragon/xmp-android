package org.helllabs.android.xmp.compose.ui.search.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazygeniouz.dfc.file.DocumentFileCompat
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.buffer
import okio.sink
import org.helllabs.android.xmp.api.ModArchiveService
import org.helllabs.android.xmp.core.Constants.isSupported
import org.helllabs.android.xmp.core.FileManager
import org.helllabs.android.xmp.core.PrefManager
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

@Immutable
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

class ResultViewModel(
    private val httpClient: HttpClient,
    private val modArchive: ModArchiveService,
    private val storageManager: StorageManager,
    private val fileManager: FileManager,
    private val prefManager: PrefManager
) : ViewModel() {

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
        currentDownloadJob?.cancel()

        currentDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                updateDownloadState(DownloadStatus.Loading)

                val result = fileManager.createModuleFile(docFile, mod.filename)
                    .onFailure { error ->
                        updateDownloadState(
                            DownloadStatus.ErrorMsg(
                                error.message ?: "Failed to create file"
                            )
                        )
                        return@launch
                    }
                    .getOrThrow()

                downloadModuleToFile(mod, result)
                    .onSuccess {
                        updateDownloadState(DownloadStatus.Success)
                    }
                    .onFailure { error ->
                        Timber.e(error, "Download failed")
                        updateDownloadState(
                            DownloadStatus.Error(
                                error as? Exception ?: Exception(
                                    error
                                )
                            )
                        )
                    }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during download")
                updateDownloadState(DownloadStatus.Error(e))
            } finally {
                update()
            }
        }
    }

    private fun updateDownloadState(status: DownloadStatus) {
        _uiState.update {
            it.copy(
                isLoading = status is DownloadStatus.Loading,
                downloadStatus = status
            )
        }
    }

    private suspend fun downloadModuleToFile(
        mod: Module,
        outputFile: DocumentFileCompat
    ): Result<Unit> = runCatching {
        val response = httpClient.get(mod.downloadUrl)
        val contentLength = response.contentLength() ?: 0L
        val channel = response.bodyAsChannel()

        fileManager.writeToFile(outputFile) { outputStream ->
            val sink = outputStream.sink().buffer()
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
                    updateDownloadState(DownloadStatus.Progress(progress))
                }
            }

            sink.flush()
        }
    }

    fun getModuleById(id: Int) {
        if (id < 0) {
            getRandomModule()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRandom = false, isLoading = true) }
            handleModuleResource(modArchive.getModuleById(id))
        }
    }

    fun getRandomModule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRandom = true, isLoading = true) }
            handleModuleResource(modArchive.getRandomModule())
        }
    }

    private suspend fun handleModuleResource(resource: Result<ModuleResult>) {
        resource.fold(
            onSuccess = { result ->
                saveModuleToHistory(result.module)
                _uiState.update {
                    it.copy(
                        module = result,
                        moduleExists = doesModuleExist(result),
                        moduleSupported = isModuleSupported(result),
                        isLoading = false
                    )
                }
            },
            onFailure = { error ->
                Timber.e(error)
                _uiState.update {
                    it.copy(
                        softError = error.message,
                        isLoading = false
                    )
                }
            }
        )
    }

    fun deleteModule() {
        viewModelScope.launch {
            val result = storageManager.deleteModule(_uiState.value.module?.module)
            Timber.d("Module deleted: ${result.isSuccess}")
            update()
        }
    }

    fun update() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    moduleExists = doesModuleExist(_uiState.value.module),
                    moduleSupported = isModuleSupported(_uiState.value.module)
                )
            }
        }
    }

    private suspend fun doesModuleExist(result: ModuleResult?): Boolean {
        val moduleExist = storageManager.doesModuleExist(result?.module)
            .getOrNull() ?: return false

        Timber.d("Does module exist? -> ${moduleExist.isFile()}")
        return moduleExist.isFile()
    }

    private fun isModuleSupported(result: ModuleResult?): Boolean {
        val supported = result?.module?.isSupported() ?: true
        Timber.d("Is module supported? -> $supported")
        return supported
    }

    private suspend fun saveModuleToHistory(module: Module) {
        val history = prefManager.getSearchHistory()
            .map { it.toHistoryModule() }
            .toMutableList()

        if (history.any { it.id == module.id }) {
            Timber.i("Module ${module.id} already exists in history. Skipping")
            return
        }

        history.add(module.toHistoryModule())

        // Maintain max size of 50
        if (history.size >= 50) {
            history.removeAt(0)
        }

        prefManager.setSearchHistory(history.toPersistentList())
    }

    private fun Module.toHistoryModule() = Module(
        id = id,
        format = format,
        songtitle = songtitle,
        artistInfo = artistInfo,
        bytes = bytes
    )
}
