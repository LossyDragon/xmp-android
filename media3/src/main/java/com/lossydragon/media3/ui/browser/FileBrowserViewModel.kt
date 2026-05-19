package com.lossydragon.media3.ui.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lossydragon.media3.data.XmpPreferences
import com.lossydragon.media3.model.BrowserUiState
import com.lossydragon.media3.model.FileItem
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.util.SKIP_EXTENSIONS
import com.lossydragon.media3.util.UNSUPPORTED_EXTENSIONS
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.helllabs.libxmp.model.ModInfo

@Suppress("ktlint:standard:class-signature")
class FileBrowserViewModel(
    private val appContext: Context,
    private val prefs: XmpPreferences
) : ViewModel() {

    val state: StateFlow<BrowserUiState>
        field = MutableStateFlow(BrowserUiState())

    private val dirStack = ArrayDeque<Uri>()

    private val metadataCache = mutableMapOf<String, ModInfo>()

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

    fun getMetadata(uri: String): ModInfo = metadataCache[uri] ?: ModInfo()

    fun setMetadata(uri: String, modInfo: ModInfo) {
        if (modInfo.name.trim().isNotBlank()) {
            metadataCache[uri] = ModInfo(name = modInfo.name.trim(), type = modInfo.type)
        }
    }

    fun onRootFolderPicked(uri: Uri) {
        appContext.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        viewModelScope.launch {
            prefs.setLastDirectoryUri(uri.toString())
        }
        rootTreeUri = uri

        dirStack.clear()
        dirStack.addLast(uri)

        loadDirectory(uri)
    }

    fun navigateInto(item: FileItem) {
        dirStack.addLast(item.uri)
        loadDirectory(item.uri)
    }

    fun navigateUp(): Boolean {
        if (dirStack.size <= 1) {
            return false
        }
        dirStack.removeLast()
        loadDirectory(dirStack.last())
        return true
    }

    fun canNavigateUp() = dirStack.size > 1

    fun navigateToBreadcrumb(index: Int) {
        while (dirStack.size > index + 1) dirStack.removeLast()
        loadDirectory(dirStack.last())
    }

    fun setShuffle(value: Boolean) {
        state.value = state.value.copy(isShuffle = value)
    }

    fun setLoop(value: Boolean) {
        state.value = state.value.copy(isLoop = value)
    }

    private fun loadDirectory(uri: Uri) {
        state.value = state.value.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val treeRoot = rootTreeUri ?: uri

                val docId = when {
                    DocumentsContract.isTreeUri(uri) &&
                        DocumentsContract.isDocumentUri(appContext, uri) ->
                        DocumentsContract.getDocumentId(uri)

                    DocumentsContract.isTreeUri(uri) ->
                        DocumentsContract.getTreeDocumentId(uri)

                    else ->
                        DocumentsContract.getDocumentId(uri)
                }

                val childrenUri = DocumentsContract
                    .buildChildDocumentsUriUsingTree(treeRoot, docId)
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                )

                val directories = mutableListOf<FileItem>()
                val modules = mutableListOf<ModuleFile>()

                appContext.contentResolver.query(
                    /* uri = */ childrenUri,
                    /* projection = */ projection,
                    /* selection = */ null,
                    /* selectionArgs = */ null,
                    /* sortOrder = */ null
                )?.use { cursor ->
                    val idCol = cursor
                        .getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor
                        .getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor
                        .getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = cursor
                        .getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                    while (cursor.moveToNext()) {
                        val childId = cursor.getString(idCol)
                        val name = cursor.getString(nameCol) ?: continue
                        val mime = cursor.getString(mimeCol) ?: continue
                        val size = cursor.getLong(sizeCol)
                        val childUri = DocumentsContract
                            .buildDocumentUriUsingTree(treeRoot, childId)

                        val dotIndex = name.indexOf('.')
                        val ext = if (dotIndex >= 0) {
                            name.substringAfterLast('.').lowercase()
                        } else {
                            ""
                        }
                        val prefix = if (dotIndex >= 0) {
                            name.substringBefore('.').lowercase()
                        } else {
                            ""
                        }

                        when {
                            mime == DocumentsContract.Document.MIME_TYPE_DIR -> {
                                FileItem(
                                    name = name,
                                    uri = childUri,
                                    isDirectory = true,
                                    size = 0L,
                                ).also(directories::add)
                            }

                            // Skip silently
                            ext in UNSUPPORTED_EXTENSIONS ||
                                prefix in UNSUPPORTED_EXTENSIONS -> Unit

                            ext !in SKIP_EXTENSIONS && prefix !in SKIP_EXTENSIONS -> {
                                ModuleFile(
                                    uri = childUri,
                                    name = name,
                                    sizeBytes = size,
                                    extension = when {
                                        ext.isNotEmpty() && ext !in SKIP_EXTENSIONS -> ext
                                        else -> prefix
                                    },
                                ).also(modules::add)
                            }
                        }
                    }
                }

                val sortedDirs = directories.sortedBy { it.name.lowercase() }
                val sortedMods = modules.sortedBy { it.name.lowercase() }
                state.value = state.value.copy(
                    currentPath = uri.lastPathSegment ?: "",
                    files = sortedMods.toImmutableList(),
                    directories = sortedDirs.toImmutableList(),
                    breadcrumbs = dirStack.map { stack ->
                        stack.lastPathSegment
                            ?.substringAfterLast('/')
                            ?.substringAfterLast(':')
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
}
