package org.helllabs.android.xmp.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.lazygeniouz.dfc.file.DocumentFileCompat
import java.util.Locale
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.core.Constants.DEFAULT_DOWNLOAD_DIR
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
     * Checks if we have a URI in preferences, then checks to see if we have R/W access
     */
    suspend fun checkPermissions(): Boolean {
        val safPath = prefManager.getSafStoragePath()
        if (safPath.isBlank()) {
            return false
        }

        val preference = safPath.toUri()
        val persistedUriPermissions = context.contentResolver.persistedUriPermissions

        return persistedUriPermissions.any {
            it.uri == preference && it.isReadPermission && it.isWritePermission
        }
    }

    /**
     * Get our parent/root directory
     */
    private suspend fun getParentDirectory(): Result<DocumentFileCompat> = runCatching {
        val prefUri = prefManager.getSafStoragePath().toUri()

        DocumentFileCompat.fromTreeUri(context, prefUri)
            ?: throw XmpException("Getting parent directory returned null")
    }

    /**
     * Get the playlist directory that was set
     */
    suspend fun getPlaylistDirectory(): Result<DocumentFileCompat> =
        getParentDirectory().mapCatching { parent ->
            parent.findFile("playlists")
                ?: throw XmpException("Playlist directory not found")
        }

    /**
     * Get the mod directory
     * This will be where modules are downloaded,
     * and where File Explorer should start
     */
    suspend fun getModDirectory(): Result<DocumentFileCompat> =
        getParentDirectory().mapCatching { parent ->
            parent.findFile("mods")
                ?: throw XmpException("Mods directory not found")
        }

    /**
     * Set the playlist directory to the specified [Uri]
     * Create `playlist` and `mod` folders respectively.
     */
    suspend fun setPlaylistDirectory(uri: Uri?): Result<Unit> = runCatching {
        requireNotNull(uri) { "Unable to set default Playlist directory" }

        prefManager.setSafStoragePath(uri.toString())

        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        context.contentResolver.takePersistableUriPermission(uri, flags)

        val parentDocument = getParentDirectory().getOrThrow()

        createRequiredDirectories(parentDocument)
    }

    private suspend fun createRequiredDirectories(parentDocument: DocumentFileCompat) {
        listOf("mods", "playlists").forEach { directoryName ->
            val exists = parentDocument.findFile(directoryName) != null
            if (!exists) {
                parentDocument.createDirectory(directoryName)

                if (directoryName == "mods") {
                    val modDir = parentDocument.findFile("mods")
                    installExampleMod(modDir)
                }
            }
        }
    }

    /**
     * Get the name of the default path we're allowed to work in.
     */
    suspend fun getDefaultPathName(): Result<String> =
        getParentDirectory().mapCatching { parent ->
            parent.name.ifEmpty {
                throw XmpException("Couldn't get default path name")
            }
        }

    /**
     * Attempt to install sample modules in our assets folder. Skip if it exists
     */
    private suspend fun installExampleMod(modPath: DocumentFileCompat?): Boolean {
        if (!prefManager.getExamples()) return true

        if (modPath == null) {
            Timber.w("modDir is null")
            return false
        }

        return runCatching {
            val assets = context.resources.assets
            assets.list("mod")?.forEach { asset ->
                if (shouldSkipAsset(modPath, asset)) {
                    Timber.i("Skipping $asset")
                    return@forEach
                }

                copyAssetToModPath(assets, asset, modPath)
            }
        }.onFailure { exception ->
            Timber.e(exception, "Failed to install example mod")
        }.isSuccess
    }

    private fun shouldSkipAsset(modPath: DocumentFileCompat, asset: String): Boolean {
        val mod = modPath.findFile(asset) ?: return false
        return mod.exists()
    }

    private fun copyAssetToModPath(
        assets: android.content.res.AssetManager,
        asset: String,
        modPath: DocumentFileCompat
    ) {
        assets.open("mod/$asset").use { inStream ->
            val file = modPath.createFile("application/octet-stream", asset)
                ?: return

            context.contentResolver.openOutputStream(file.uri)?.use { outStream ->
                inStream.copyTo(outStream)
            }
        }
    }

    /**
     * Get the download path a mod should be downloaded to.
     *
     * @see [PrefManager.getModArchiveFolder] if the pref was set to download
     * @see [PrefManager.getArtistFolder]
     */
    private suspend fun getDownloadPath(module: Module): Result<DocumentFileCompat> =
        getModDirectory().mapCatching { modDir ->
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
     * List only the immediate children of a directory (non-recursive)
     *
     * @param uri the directory URI
     * @return list of immediate child URIs
     */
    fun listDirectoryContents(uri: Uri?): List<Uri> {
        if (uri == null) return emptyList()

        return try {
            val docId = DocumentsContract.getDocumentId(uri)
            val childDocUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, docId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            )

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
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(uri, childDocumentId)

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        directories.add(childUri)
                    } else {
                        files.add(childUri)
                    }
                }
            }

            // Return directories first, then files, sorted alphabetically
            directories.sortedBy { getFileName(it)?.lowercase() } +
                files.sortedBy { getFileName(it)?.lowercase() }
        } catch (e: Exception) {
            Timber.e(e, "Error listing directory: $uri")
            emptyList()
        }
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
            sortedUris.addAll(walkDownDirectory(dirUri, includeDirectories))
        }

        sortedUris.addAll(
            files.sortedBy { it.toString().lowercase(Locale.getDefault()) }
        )

        return sortedUris
    }

    /**
     * Gets the filename from a [Uri]
     *
     * @return the name of the uri file
     */
    fun getFileName(uri: Uri?): String? {
        if (uri == null) return null
        return DocumentFileCompat.fromSingleUri(context, uri)!!.name
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
     *
     * @throws None - All exceptions are caught and logged, returning null instead.
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
}
