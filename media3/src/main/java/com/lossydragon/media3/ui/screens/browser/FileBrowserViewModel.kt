package com.lossydragon.media3.ui.screens.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lossydragon.media3.core.Constants
import com.lossydragon.media3.data.ModuleMetadataRepository
import com.lossydragon.media3.db.XmpPreferences
import com.lossydragon.media3.model.BrowserUiState
import com.lossydragon.media3.model.FileItem
import com.lossydragon.media3.model.ModuleFile
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FileBrowserViewModel(
    private val appContext: Context,
    private val prefs: XmpPreferences,
    private val db: ModuleMetadataRepository
) : ViewModel() {

    val state: StateFlow<BrowserUiState>
        field = MutableStateFlow(BrowserUiState())

    private val dirStack = ArrayDeque<Uri>()
    private var rootTreeUri: Uri? = null

    init {
        viewModelScope.launch {
            prefs.getLastDirectoryUri()?.let { savedUri ->
                state.value = state.value.copy(isLoading = true)
                onRootFolderPicked(savedUri.toUri())
            } ?: run {
                state.value = state.value.copy(isLoading = false)
            }
        }
    }

    fun onRootFolderPicked(uri: Uri) {
        appContext.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        viewModelScope.launch { prefs.setLastDirectoryUri(uri.toString()) }
        rootTreeUri = uri
        dirStack.clear()
        dirStack.addLast(uri)

        // Wait until we're fully done.
        // loadDirectory(uri)

        // Background index entire tree
        viewModelScope.launch(Dispatchers.IO) {
            indexDirectory(uri)
            loadDirectory(uri)
        }
    }

    fun navigateInto(item: FileItem) {
        dirStack.addLast(item.uri)
        viewModelScope.launch(Dispatchers.IO) {
            indexDirectory(item.uri)
            loadDirectory(item.uri)
        }
    }

    fun navigateUp(): Boolean {
        if (dirStack.size <= 1) return false
        dirStack.removeLast()
        loadDirectory(dirStack.last())
        return true
    }

    fun canNavigateUp() = dirStack.size > 1

    fun setShuffle(value: Boolean) {
        state.value = state.value.copy(isShuffle = value)
    }

    fun setLoop(value: Boolean) {
        state.value = state.value.copy(isLoop = value)
    }

    fun navigateToBreadcrumb(index: Int) {
        while (dirStack.size > index + 1) dirStack.removeLast()
        loadDirectory(dirStack.last())
    }

    private fun loadDirectory(uri: Uri) {
        state.value = state.value.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val treeRoot = rootTreeUri ?: uri
                val docId = resolveDocId(uri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeRoot, docId)

                data class RawFile(val uri: Uri, val name: String, val size: Long, val ext: String)

                val directories = mutableListOf<FileItem>()
                val rawFiles = mutableListOf<RawFile>()

                appContext.contentResolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_SIZE,
                    ),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    val idCol =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                    while (cursor.moveToNext()) {
                        val childId = cursor.getString(idCol)
                        val name = cursor.getString(nameCol) ?: continue
                        val mime = cursor.getString(mimeCol) ?: continue
                        val size = cursor.getLong(sizeCol)
                        val childUri =
                            DocumentsContract.buildDocumentUriUsingTree(treeRoot, childId)
                        val ext = name.substringAfterLast('.', "").lowercase()
                        val prefix = name.substringBefore('.').lowercase()

                        when {
                            mime == DocumentsContract.Document.MIME_TYPE_DIR ->
                                directories.add(
                                    FileItem(
                                        name = name,
                                        uri = childUri,
                                        isDirectory = true,
                                        size = 0L
                                    )
                                )

                            ext in Constants.UNSUPPORTED_EXTENSIONS ||
                                prefix in Constants.UNSUPPORTED_EXTENSIONS -> Unit

                            ext !in Constants.SKIP_EXTENSIONS &&
                                prefix !in Constants.SKIP_EXTENSIONS ->
                                rawFiles.add(RawFile(childUri, name, size, ext.ifEmpty { prefix }))
                        }
                    }
                }

                // Single batch DB query instead of N individual queries
                val cachedMap = db.getByFileNames(rawFiles.map { it.name })
                    .associateBy { it.fileName }

                val modules = rawFiles.map { raw ->
                    val cached = cachedMap[raw.name]
                    ModuleFile(
                        uri = raw.uri,
                        name = raw.name,
                        sizeBytes = raw.size,
                        extension = raw.ext,
                        resolvedName = cached?.name ?: "",
                        resolvedType = cached?.type ?: "",
                    )
                }

                state.value = state.value.copy(
                    currentPath = uri.lastPathSegment ?: "",
                    files = modules.sortedBy { it.name.lowercase() }.toImmutableList(),
                    directories = directories.sortedBy { it.name.lowercase() }.toImmutableList(),
                    breadcrumbs = dirStack.map {
                        it.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
                            ?: "Root"
                    }.toImmutableList(),
                    isLoading = false,
                    hasStorageAccess = true,
                )
            } catch (e: Exception) {
                state.value = state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private suspend fun indexDirectory(uri: Uri) {
        val treeRoot = rootTreeUri ?: uri
        val docId = resolveDocId(uri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeRoot, docId)

        appContext.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            )
            val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

            while (cursor.moveToNext()) {
                val childId = cursor.getString(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val mime = cursor.getString(mimeCol) ?: continue
                val size = cursor.getLong(sizeCol)
                val childUri = DocumentsContract.buildDocumentUriUsingTree(treeRoot, childId)
                val ext = name.substringAfterLast('.', "").lowercase()
                val prefix = name.substringBefore('.').lowercase()

                when {
                    mime == DocumentsContract.Document.MIME_TYPE_DIR -> Unit

                    ext in Constants.UNSUPPORTED_EXTENSIONS ||
                        prefix in Constants.UNSUPPORTED_EXTENSIONS -> Unit

                    ext !in Constants.SKIP_EXTENSIONS &&
                        prefix !in Constants.SKIP_EXTENSIONS -> {
                        if (!db.exists(name, size)) {
                            db.fetchAndCache(childUri, name, size, ext.ifEmpty { prefix })
                        }
                    }
                }
            }
        }
    }

    private fun resolveDocId(uri: Uri): String = when {
        DocumentsContract.isTreeUri(uri) &&
            DocumentsContract.isDocumentUri(appContext, uri) ->
            DocumentsContract.getDocumentId(uri)

        DocumentsContract.isTreeUri(uri) ->
            DocumentsContract.getTreeDocumentId(uri)

        else ->
            DocumentsContract.getDocumentId(uri)
    }
}
