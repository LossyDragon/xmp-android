package org.helllabs.android.xmp.core

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.lazygeniouz.dfc.file.DocumentFileCompat
import java.util.Locale
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.core.Constants.DEFAULT_DOWNLOAD_DIR
import org.helllabs.android.xmp.model.Module
import timber.log.Timber

class XmpException(string: String) : Exception(string)

/**
 * This object class is kinda of a mash up of anything related to SAF and the Document Tree
 */
object StorageManager {

    /**
     * Checks if we have a URI in preferences, then checks to see if we have R/W access
     */
    fun checkPermissions(): Boolean {
        val context = XmpApplication.instance!!.applicationContext

        val isPreferenceEmpty = PrefManager.safStoragePath.isBlank()
        if (isPreferenceEmpty) {
            return false
        }

        val preference = PrefManager.safStoragePath.toUri()
        val persistedUriPermissions = context.contentResolver.persistedUriPermissions
        return persistedUriPermissions.any {
            it.uri == preference && it.isReadPermission && it.isWritePermission
        }
    }

    /**
     * Get our parent/root directory
     */
    private fun getParentDirectory(): Result<DocumentFileCompat> {
        val context = XmpApplication.instance?.applicationContext
            ?: return Result.failure(XmpException("App context is null"))

        val prefUri = PrefManager.safStoragePath.toUri()

        val parent = DocumentFileCompat.fromTreeUri(context, prefUri)
            ?: return Result.failure(XmpException("Getting parent directory returned null"))

        return Result.success(parent)
    }

    /**
     * Get the playlist directory that was set
     */
    fun getPlaylistDirectory(): Result<DocumentFileCompat> =
        getParentDirectory().mapCatching { parent ->
            parent.findFile("playlists") ?: throw XmpException("Playlist directory not found")
        }

    /**
     * Get the mod directory
     * This will be where modules are downloaded,
     * and where File Explorer should start
     */
    fun getModDirectory(): Result<DocumentFileCompat> = getParentDirectory().mapCatching { parent ->
        parent.findFile("mods") ?: throw XmpException("Mods directory not found")
    }

    /**
     * Set the playlist directory to the specified [Uri]
     * Create `playlist` and `mod` folders respectively.
     */
    fun setPlaylistDirectory(uri: Uri?): Result<Unit> {
        return runCatching {
            if (uri == null) {
                throw XmpException("Unable to set default Playlist directory")
            }

            val context = XmpApplication.instance?.applicationContext
                ?: return Result.failure(XmpException("App context is null"))

            PrefManager.safStoragePath = uri.toString()

            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.contentResolver.takePersistableUriPermission(uri, flags)

            val parentDocument = getParentDirectory().getOrElse { throw it }

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
    }

    /**
     * Get the name of the default path we're allowed to work in.
     */
    fun getDefaultPathName(): Result<String> = getParentDirectory().mapCatching { parent ->
        parent.name.ifEmpty { throw XmpException("Couldn't get default path name") }
    }

    /**
     * Attempt to install sample modules in our assets folder. Skip if it exists
     */
    private fun installExampleMod(modPath: DocumentFileCompat?): Boolean {
        if (!PrefManager.examples) return true

        if (modPath == null) {
            Timber.w("modDir is null")
            return false
        }

        val context = XmpApplication.instance!!.applicationContext
        return runCatching {
            val assets = context.resources.assets
            assets.list("mod")?.forEach { asset ->
                val mod = modPath.findFile(asset) ?: return@forEach
                if (mod.exists()) {
                    Timber.i("Skipping $asset")
                    return@forEach
                }

                val inStream = assets.open("mod/$asset")
                val file = modPath.createFile("application/octet-stream", asset)
                val outStream = context.contentResolver.openOutputStream(file!!.uri)
                    ?: return@forEach

                inStream.copyTo(outStream)
            }
        }.onFailure { exception ->
            Timber.e(exception)
            return false
        }.isSuccess
    }

    /**
     * Get the download path a mod should be downloaded to.
     *
     * @see [PrefManager.modArchiveFolder] if the pref was set to download
     * @see [PrefManager.artistFolder]
     */
    private fun getDownloadPath(module: Module): Result<DocumentFileCompat> =
        getModDirectory().mapCatching { modDir ->
            if (!modDir.isDirectory()) {
                throw XmpException("Unable to access the mod directory.")
            }

            var targetDir = modDir

            if (PrefManager.modArchiveFolder) {
                targetDir = targetDir.findFile(DEFAULT_DOWNLOAD_DIR) ?: targetDir.createDirectory(
                    DEFAULT_DOWNLOAD_DIR
                )
                    ?: throw XmpException("Failed to access or create TMA directory.")
                if (!targetDir.isDirectory()) {
                    throw IllegalArgumentException("TMA directory is not a directory.")
                }
            }

            if (PrefManager.artistFolder) {
                val artistName = module.getArtist()
                targetDir = targetDir.findFile(artistName) ?: targetDir.createDirectory(artistName)
                    ?: throw XmpException("Failed to access or create the artist directory.")
                if (!targetDir.isDirectory()) {
                    throw IllegalArgumentException("Artist directory is not a directory.")
                }
            }

            targetDir
        }

    /**
     * Delete a File or Directory
     *
     * @param uri the [Uri] to be deleted
     *
     * @return true if successful, otherwise false
     */
    fun deleteFileOrDirectory(uri: Uri?): Boolean {
        if (uri == null) {
            return false
        }

        val context = XmpApplication.instance!!.applicationContext
        val docFile = DocumentFileCompat.fromSingleUri(context, uri)

        return docFile?.delete() ?: false
    }

    /**
     * Check if a module exists in a location given the preferences
     * @see [PrefManager.artistFolder]
     * @see [PrefManager.modArchiveFolder]
     *
     *  @param module the [Module] in question
     */
    fun doesModuleExist(module: Module?): Result<DocumentFileCompat> = runCatching {
        if (module == null || module.url.isBlank()) {
            throw XmpException("Module or module URL is null or blank.")
        }

        getDownloadPath(module).mapCatching { dir ->
            val moduleFilename = module.url.substringAfterLast('#')
            val file = dir.findFile(moduleFilename)
            if (file != null && file.exists() && file.isFile()) {
                file
            } else {
                dir
            }
        }.getOrElse { throw it }
    }

    /**
     * Delete a recently downloaded module
     *
     * @param module the [Module] to be deleted
     *
     * @return if successful or not
     */
    fun deleteModule(module: Module?): Result<Boolean> {
        if (module == null || module.url.isBlank()) {
            Timber.w("Module was null")
            return Result.failure(XmpException("Module was null or url is blank"))
        }

        return getDownloadPath(module).mapCatching { dir ->
            val moduleFilename = module.url.substringAfterLast('#')
            val file = dir.findFile(moduleFilename)
                ?: throw XmpException("$moduleFilename not found in directory")

            file.delete()
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
        if (uri == null) {
            return emptyList()
        }

        val context = XmpApplication.instance!!.applicationContext
        val docId = DocumentsContract.getDocumentId(uri)
        val childDocUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )

        val directories = mutableListOf<Uri>()
        val files = mutableListOf<Uri>()

        context.contentResolver.query(childDocUri, projection, null, null, null)?.use { cursor ->
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

        val sortedUris = mutableListOf<Uri>()

        directories.sortedBy { it.toString().lowercase(Locale.getDefault()) }.forEach { dirUri ->
            if (includeDirectories) {
                sortedUris.add(dirUri)
            }
            sortedUris.addAll(walkDownDirectory(dirUri, includeDirectories))
        }

        sortedUris.addAll(files.sortedBy { it.toString().lowercase(Locale.getDefault()) })

        return sortedUris
    }

    /**
     * Gets the filename from a [Uri]
     *
     * @return the name of the uri file
     */
    fun getFileName(uri: Uri?): String? {
        if (uri == null) {
            return null
        }

        val context = XmpApplication.instance!!.applicationContext
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.moveToFirst()
        val idx = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx == null || idx < 0) {
            return null
        }
        val fileName = cursor.getString(idx)
        cursor.close()

        return fileName // DocumentFileCompat.fromSingleUri(context, uri)?.name
    }
}
