package com.lossydragon.media3.ui.screens.downloads.viewmodel

import android.content.Context
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lossydragon.media3.data.DownloadHistoryRepository
import com.lossydragon.media3.data.ModArchiveService
import com.lossydragon.media3.db.XmpPreferences
import com.lossydragon.media3.model.DownloadStatus
import com.lossydragon.media3.model.Module
import com.lossydragon.media3.model.ModuleResultState
import com.lossydragon.media3.util.getDownloadDir
import com.lossydragon.media3.util.getOrCreateOutputFile
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class ModuleResultViewModel(
    private val appContext: Context,
    private val service: ModArchiveService,
    private val httpClient: HttpClient,
    private val history: DownloadHistoryRepository,
    prefs: XmpPreferences
) : ViewModel() {

    val state: StateFlow<ModuleResultState>
        field = MutableStateFlow(ModuleResultState())

    private var downloadJob: Job? = null

    private val lastDirectory: Flow<String?> = prefs.getLastDirectoryFlow()

    fun getModuleById(id: Int) {
        if (id < 0) {
            getRandomModule()
            return
        }
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, isRandom = false) }
            service.getModuleById(id).fold(
                onSuccess = { result ->
                    state.update {
                        it.copy(
                            result = result,
                            moduleExists = checkExists(result.module),
                            isLoading = false,
                            softError = null,
                        )
                    }
                    history.add(result.module)
                },
                onFailure = ::handleFailure
            )
        }
    }

    fun getRandomModule() {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, isRandom = true) }
            service.getRandomModule().fold(
                onSuccess = { result ->
                    state.update {
                        it.copy(
                            result = result,
                            moduleExists = checkExists(result.module),
                            isLoading = false,
                            softError = null,
                        )
                    }

                    history.add(result.module)
                },
                onFailure = ::handleFailure
            )
        }
    }

    fun downloadModule(module: Module) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                state.update { it.copy(downloadStatus = DownloadStatus.Loading) }

                val rootUri = lastDirectory.firstOrNull()?.toUri() ?: run {
                    state.update {
                        it.copy(downloadStatus = DownloadStatus.Error("No folder selected"))
                    }
                    return@launch
                }

                val outputFile = appContext.getOrCreateOutputFile(
                    rootUri = rootUri,
                    module = module
                ) ?: run {
                    state.update {
                        it.copy(
                            downloadStatus = DownloadStatus.Error("Could not create output file")
                        )
                    }
                    return@launch
                }
                val response = httpClient.get(module.downloadUrl)
                val totalBytes = response.contentLength() ?: 0L
                val channel = response.bodyAsChannel()
                var bytesRead = 0L
                val buf = ByteArray(8192)

                outputFile.use { out ->
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buf, 0, buf.size)
                        if (read < 0) break
                        out.write(buf, 0, read)
                        bytesRead += read
                        if (totalBytes > 0) {
                            state.update {
                                it.copy(
                                    downloadStatus = DownloadStatus.Progress(
                                        bytesRead * 100f / totalBytes
                                    )
                                )
                            }
                        }
                    }
                }
                state.update {
                    it.copy(
                        downloadStatus = DownloadStatus.Success,
                        moduleExists = true
                    )
                }
            } catch (e: Exception) {
                Timber.e(e)
                state.update {
                    it.copy(
                        downloadStatus = DownloadStatus.Error(
                            e.message ?: "Download failed"
                        )
                    )
                }
            }
        }
    }

    fun deleteModule(module: Module) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileUri = findModuleUri(module) ?: return@launch
                DocumentsContract.deleteDocument(appContext.contentResolver, fileUri)
                state.update { it.copy(moduleExists = false) }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun refreshExists() {
        viewModelScope.launch {
            val module = state.value.result?.module ?: return@launch
            state.update { it.copy(moduleExists = checkExists(module)) }
        }
    }

    private fun handleFailure(t: Throwable) {
        Timber.e(t)
        state.update { it.copy(softError = t.message, isLoading = false) }
    }

    private suspend fun checkExists(module: Module) = findModuleUri(module) != null

    private suspend fun findModuleUri(module: Module): android.net.Uri? {
        val rootUri = (lastDirectory.firstOrNull() ?: return null).toUri()
        val dirUri = appContext.getDownloadDir(rootUri, module) ?: return null
        val dirDocId = DocumentsContract.getDocumentId(dirUri)
        val filename = module.url.substringAfterLast('#')
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, dirDocId)

        appContext.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == filename) {
                    return DocumentsContract.buildDocumentUriUsingTree(rootUri, cursor.getString(0))
                }
            }
        }
        return null
    }
}
