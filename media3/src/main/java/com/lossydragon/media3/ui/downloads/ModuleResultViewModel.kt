package com.lossydragon.media3.ui.downloads

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lossydragon.media3.data.ModArchiveService
import com.lossydragon.media3.model.DownloadStatus
import com.lossydragon.media3.model.Module
import com.lossydragon.media3.model.ModuleResultState
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class ModuleResultViewModel(
    private val appContext: Context,
    private val service: ModArchiveService,
    private val httpClient: HttpClient
) : ViewModel() {

    val state: StateFlow<ModuleResultState>
        field = MutableStateFlow(ModuleResultState())

    private var downloadJob: Job? = null

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
                            module = result,
                            moduleExists = checkExists(result.module),
                            isLoading = false,
                            softError = null,
                        )
                    }
                },
                onFailure = {
                    Timber.e(it)
                    state.update { s ->
                        s.copy(
                            softError = it.message,
                            isLoading = false
                        )
                    }
                }
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
                            module = result,
                            moduleExists = checkExists(result.module),
                            isLoading = false,
                            softError = null,
                        )
                    }
                },
                onFailure = {
                    Timber.e(it)
                    state.update { s ->
                        s.copy(
                            softError = it.message,
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    fun downloadModule(module: Module) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                state.update { it.copy(downloadStatus = DownloadStatus.Loading) }
                val outputFile = getOrCreateOutputFile(module) ?: run {
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
                val prefs = appContext.getSharedPreferences("xmp_prefs", Context.MODE_PRIVATE)
                val rootUri = (prefs.getString("last_directory_uri", null) ?: return@launch).toUri()
                val dirUri = getDownloadDir(rootUri, module) ?: return@launch
                val dirDocId = DocumentsContract.getDocumentId(dirUri)
                val filename = module.url.substringAfterLast('#')

                val childrenUri =
                    DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, dirDocId)
                appContext.contentResolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        if (cursor.getString(1) == filename) {
                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(
                                rootUri,
                                cursor.getString(0)
                            )
                            DocumentsContract.deleteDocument(appContext.contentResolver, fileUri)
                            state.update { it.copy(moduleExists = false) }
                            return@launch
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun refreshExists() {
        viewModelScope.launch {
            val module = state.value.module?.module ?: return@launch
            state.update { it.copy(moduleExists = checkExists(module)) }
        }
    }

    private fun checkExists(module: Module): Boolean {
        return try {
            val prefs = appContext.getSharedPreferences("xmp_prefs", Context.MODE_PRIVATE)
            val rootUri = (prefs.getString("last_directory_uri", null) ?: return false).toUri()
            val dirUri = getDownloadDir(rootUri, module) ?: return false
            val dirDocId = DocumentsContract.getDocumentId(dirUri)
            val filename = module.url.substringAfterLast('#')

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, dirDocId)
            appContext.contentResolver.query(
                childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(0) == filename) return true
                }
            }
            false
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    private fun getDownloadDir(rootUri: Uri, module: Module): Uri? {
        return try {
            val rootDoc = DocumentsContract.getTreeDocumentId(rootUri)
            val cr = appContext.contentResolver
            val tmaDoc = findOrCreateChildDir(cr, rootUri, rootDoc, "TheModArchive") ?: return null
            val artistDoc = findOrCreateChildDir(cr, rootUri, tmaDoc, module.artist) ?: tmaDoc
            DocumentsContract.buildDocumentUriUsingTree(rootUri, artistDoc)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    private fun findOrCreateChildDir(
        cr: ContentResolver,
        treeUri: Uri,
        parentDocId: String,
        name: String
    ): String? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        cr.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == name &&
                    cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                ) {
                    return cursor.getString(0)
                }
            }
        }
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocId)
        val newUri = DocumentsContract.createDocument(
            cr,
            parentUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            name
        )
        return newUri?.let { DocumentsContract.getDocumentId(it) }
    }

    private fun getOrCreateOutputFile(module: Module): OutputStream? {
        return try {
            val prefs = appContext.getSharedPreferences("xmp_prefs", Context.MODE_PRIVATE)
            val rootUri = prefs.getString("last_directory_uri", null) ?: return null
            val dirUri = getDownloadDir(rootUri.toUri(), module) ?: return null
            val treeUri = rootUri.toUri()
            val dirDocId = DocumentsContract.getDocumentId(dirUri)
            val filename = module.url.substringAfterLast('#')

            // Check if file exists
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, dirDocId)
            var fileUri: Uri? = null
            appContext.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == filename) {
                        fileUri = DocumentsContract.buildDocumentUriUsingTree(
                            treeUri,
                            cursor.getString(0)
                        )
                        break
                    }
                }
            }

            // Create if not exists
            if (fileUri == null) {
                fileUri = DocumentsContract.createDocument(
                    appContext.contentResolver,
                    dirUri,
                    "application/octet-stream",
                    filename
                )
            }

            fileUri?.let { appContext.contentResolver.openOutputStream(it) }
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        downloadJob?.cancel()
    }
}
