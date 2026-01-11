package org.helllabs.android.xmp.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import com.lazygeniouz.dfc.file.DocumentFileCompat
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.core.Constants.DEFAULT_DOWNLOAD_DIR
import org.helllabs.android.xmp.model.FileItem
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.model.Module
import timber.log.Timber

/**
 * Handles SAF (Storage Access Framework) operations and document tree management
 */
class StorageManager(private val context: Context, private val prefManager: PrefManager) {

    fun testModule(uri: Uri, modInfo: ModInfo = ModInfo()): Boolean = Xmp.testFromFd(
        context = context,
        storageManager = this,
        uri = uri,
        modInfo = modInfo
    )

    fun loadModule(uri: Uri): Int = Xmp.loadFromFd(
        context = context,
        storageManager = this,
        uri = uri
    )

    /**
     * Gets the top level path for explorer
     */
    suspend fun getExplorerRootDirectory(): Result<DocumentFileCompat> = runCatching {
        val prefUri = prefManager.getExplorerRootPath()

        Timber.d("Loading root path: $prefUri")

        DocumentFileCompat.fromTreeUri(context, prefUri.toUri())
            ?: throw XmpException("Getting parent directory returned null")
    }

    /**
     * Gets the top level path for playlists
     */
    suspend fun getPlaylistsRootDirectory(): Result<DocumentFileCompat> = runCatching {
        val prefUri = prefManager.getPlaylistRootPath()

        Timber.d("Loading playlist path: $prefUri")

        DocumentFileCompat.fromTreeUri(context, prefUri.toUri())
            ?: throw XmpException("Getting playlist directory returned null")
    }

    /**
     * Get the download path a mod should be downloaded to.
     *
     * @see [PrefManager.getModArchiveFolder] if the pref was set to download
     * @see [PrefManager.getArtistFolder]
     */
    private suspend fun getDownloadPath(module: Module): Result<DocumentFileCompat> =
        getExplorerRootDirectory().mapCatching { modDir ->
            require(modDir.isDirectory()) {
                "Unable to access the mod directory."
            }

            var targetDir = modDir

            if (prefManager.getModArchiveFolder()) {
                targetDir = getOrCreateDirectory(targetDir, DEFAULT_DOWNLOAD_DIR, "TMA")
            }

            if (prefManager.getArtistFolder()) {
                val artistName = module.getArtist()
                targetDir = getOrCreateDirectory(targetDir, artistName, "artist")
            }

            targetDir
        }

    private fun getOrCreateDirectory(
        parent: DocumentFileCompat,
        directoryName: String,
        type: String
    ): DocumentFileCompat {
        val dir = parent.findFile(directoryName)
            ?: parent.createDirectory(directoryName)
            ?: throw XmpException("Failed to access or create the $type directory.")

        require(dir.isDirectory()) {
            "$type directory is not a directory."
        }

        return dir
    }

    /**
     * Delete a File or Directory
     *
     * @param uri the [Uri] to be deleted
     *
     * @return true if successful, otherwise false
     */
    fun deleteFileOrDirectory(uri: Uri?): Boolean {
        if (uri == null) return false

        val docFile = DocumentFileCompat.fromSingleUri(context, uri)
        return docFile?.delete() ?: false
    }

    /**
     * Check if a module exists in a location given the preferences
     * @see [PrefManager.getArtistFolder]
     * @see [PrefManager.getModArchiveFolder]
     *
     * @param module the [Module] in question
     */
    suspend fun doesModuleExist(module: Module?): Result<DocumentFileCompat> = runCatching {
        requireNotNull(module) { "Module is null" }
        require(module.url.isNotBlank()) { "Module URL is blank" }

        val dir = getDownloadPath(module).getOrThrow()
        val moduleFilename = module.url.substringAfterLast('#')
        val file = dir.findFile(moduleFilename)

        if (file != null && file.exists() && file.isFile()) {
            file
        } else {
            dir
        }
    }

    /**
     * Delete a recently downloaded module
     *
     * @param module the [Module] to be deleted
     *
     * @return if successful or not
     */
    suspend fun deleteModule(module: Module?): Result<Boolean> = runCatching {
        requireNotNull(module) { "Module is null" }
        require(module.url.isNotBlank()) { "Module URL is blank" }

        val dir = getDownloadPath(module).getOrThrow()
        val moduleFilename = module.url.substringAfterLast('#')
        val file = dir.findFile(moduleFilename)
            ?: throw XmpException("$moduleFilename not found in directory")

        file.delete()
    }.onFailure {
        Timber.e(it, "Failed to delete module")
    }

    /**
     * A Top-Down File Walker
     *
     * Will walk down a given uri and collect uris in alphabetical order, folders first
     *
     * @param uri the URI to begin walking
     * @param includeDirectories whether to add directories in the list to return
     *
     * @return a list of [Uri]'s in order.
     */
    fun walkDownDirectory(uri: Uri?, includeDirectories: Boolean = true): List<Uri> {
        if (uri == null) return emptyList()

        return try {
            val docId = DocumentsContract.getDocumentId(uri)
            val childDocUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, docId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            )

            val (directories, files) = collectDirectoriesAndFiles(childDocUri, uri, projection)
            buildSortedUriList(directories, files, includeDirectories)
        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied accessing: $uri")
            emptyList()
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Invalid URI: $uri")
            emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Error walking directory: $uri")
            emptyList()
        }
    }

    private fun collectDirectoriesAndFiles(
        childDocUri: Uri,
        parentUri: Uri,
        projection: Array<String>
    ): Pair<MutableList<Uri>, MutableList<Uri>> {
        val directories = mutableListOf<Uri>()
        val files = mutableListOf<Uri>()

        context.contentResolver.query(
            childDocUri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                val childDocumentId = cursor.getString(idCol)
                val mimeType = cursor.getString(mimeCol)
                val childUri = DocumentsContract.buildDocumentUriUsingTree(
                    parentUri,
                    childDocumentId
                )

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    directories.add(childUri)
                } else {
                    files.add(childUri)
                }
            }
        }

        return Pair(directories, files)
    }

    private fun buildSortedUriList(
        directories: List<Uri>,
        files: List<Uri>,
        includeDirectories: Boolean
    ): List<Uri> {
        val sortedUris = mutableListOf<Uri>()

        directories.sortedBy {
            it.toString().lowercase(Locale.getDefault())
        }.forEach { dirUri ->
            if (includeDirectories) {
                sortedUris.add(dirUri)
            }

            walkDownDirectory(dirUri, includeDirectories).also(sortedUris::addAll)
        }

        files.sortedBy { it.toString().lowercase(Locale.getDefault()) }.also(sortedUris::addAll)

        return sortedUris
    }

    /**
     * Gets the filename from a [Uri]
     *
     * @return the name of the uri file
     */
    fun getFileName(uri: Uri?): String? {
        if (uri == null) return null
        return DocumentFileCompat.fromSingleUri(context, uri)?.name
    }

    /**
     * Creates a DocumentFileCompat instance from a given URI.
     *
     * This method safely wraps the DocumentFileCompat creation process and handles
     * common exceptions that may occur when working with URIs from different sources.
     *
     * @param uri The URI to convert to a DocumentFileCompat instance. Can be from
     *            content providers, file system, or other sources.
     * @return A DocumentFileCompat instance if successful, null if the URI is invalid,
     *         permissions are denied, or any other error occurs during creation.
     */
    fun getDocumentFileFromUri(uri: Uri): DocumentFileCompat? = try {
        DocumentFileCompat.fromSingleUri(context, uri)
    } catch (e: IllegalStateException) {
        Timber.e(e, "DocumentFileCompat failed for URI: $uri")
        null
    } catch (e: SecurityException) {
        Timber.e(e, "Permission denied for URI: $uri")
        null
    }

    /**
     * Takes persistable URI permissions for the given URI.
     *
     * This must be called after receiving a URI from Storage Access Framework (SAF)
     * via OpenDocumentTree to ensure the permissions persist across app restarts.
     * Without this, the URI permissions are temporary and will be lost.
     *
     * @param uri The URI to take persistable permissions for, typically from SAF
     * @return true if permissions were successfully persisted, false if SecurityException occurred
     */
    fun takePersistablePerms(uri: Uri): Boolean {
        return try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            true
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to take persistable permission")
            false
        }
    }

    /**
     * List directory contents with metadata in a single optimized query
     * Much faster than calling getDocumentFileFromUri for each item
     */
    suspend fun listDirectoryWithMetadata(uri: Uri?): List<FileItem> = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext emptyList()

        try {
            val docId = DocumentsContract.getDocumentId(uri)
            val childDocUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, docId)

            // Get ALL metadata in one query
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_SIZE
            )

            val directories = mutableListOf<FileItem>()
            val files = mutableListOf<FileItem>()

            context.contentResolver.query(
                childDocUri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID
                )
                val mimeCol = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                )
                val nameCol = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                )
                val modCol = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                )
                val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val childDocumentId = cursor.getString(idCol)
                    val mimeType = cursor.getString(mimeCol)
                    val name = cursor.getString(nameCol)
                    val lastModified = cursor.getLong(modCol)
                    val size = cursor.getLong(sizeCol)
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(uri, childDocumentId)

                    val item = FileItem(
                        name = name,
                        uri = childUri,
                        isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR,
                        lastModified = lastModified,
                        size = size / 1024 // KB
                    )

                    if (item.isDirectory) {
                        directories.add(item)
                    } else {
                        files.add(item)
                    }
                }
            }

            // Sort directories first, then files. case-insensitive.
            val sortedDirectories = directories.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
            val sortedFiles = files.sortedWith(
                compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )

            sortedDirectories + sortedFiles
        } catch (e: Exception) {
            Timber.e(e, "Error listing directory: $uri")
            emptyList()
        }
    }
}
