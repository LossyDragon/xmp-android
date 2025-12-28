package org.helllabs.android.xmp.core

import android.content.Context
import android.net.Uri
import com.lazygeniouz.dfc.file.DocumentFileCompat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.json.Json
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber

class PlaylistManager(
    private val context: Context,
    private val json: Json,
    private val storageManager: StorageManager
) {

    var playlist: Playlist = Playlist()
        private set

    private var oldName: String? = null

    suspend fun new(name: String, comment: String): Result<Boolean> {
        playlist = Playlist(
            name = name.trim(),
            comment = comment.trim()
        )
        return save()
    }

    fun load(uri: Uri): Result<Boolean> = runCatching {
        require(uri.pathSegments.last().endsWith(".json")) {
            "Uri: $uri is not a .json file"
        }

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            playlist = json.decodeFromString<Playlist>(jsonString)
        } ?: throw XmpException("Failed to open input stream for $uri")

        true
    }.onFailure {
        Timber.e(it, "Failed to load playlist from $uri")
    }

    suspend fun save(): Result<Boolean> = runCatching {
        val dir = storageManager.getPlaylistDirectory().getOrThrow()
        val fileName = "${playlist.name}${Constants.SUFFIX}"
        val tempFileName = ".tmp_${System.currentTimeMillis()}_${playlist.name}.json"

        val tempFile = dir.createFile("application/json", tempFileName)
            ?: throw XmpException("Failed to create temp file")

        try {
            writePlaylistToFile(tempFile.uri)
            deleteOldFileIfRenamed(dir, fileName)
            renameTempFile(tempFile, fileName)

            val finalFile = dir.findFile(fileName)
                ?: throw XmpException("File not found after rename: $fileName")

            playlist = playlist.copy(uri = finalFile.uri)
            oldName = null
            true
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    private fun writePlaylistToFile(uri: Uri) {
        val jsonString = json.encodeToString(Playlist.serializer(), playlist)
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.writer().use { writer ->
                writer.write(jsonString)
                writer.flush()
            }
        } ?: throw XmpException("Failed to open output stream")
    }

    private fun deleteOldFileIfRenamed(dir: DocumentFileCompat, fileName: String) {
        when {
            !oldName.isNullOrEmpty() && oldName != playlist.name -> {
                dir.findFile("$oldName${Constants.SUFFIX}")?.delete()
            }

            else -> dir.findFile(fileName)?.delete()
        }
    }

    private fun renameTempFile(tempFile: DocumentFileCompat, fileName: String) {
        if (!tempFile.renameTo(fileName)) {
            throw XmpException("Failed to rename temp file to $fileName")
        }
    }

    suspend fun rename(newName: String, newComment: String): Result<Boolean> {
        if (newName.trim() != playlist.name) {
            oldName = playlist.name
        }

        playlist = playlist
            .withName(newName.trim())
            .withComment(newComment.trim())

        return save()
    }

    suspend fun add(items: List<PlaylistItem>): Result<Boolean> {
        if (items.isEmpty()) return Result.success(true)

        playlist = playlist.withList(
            (playlist.list + items).toPersistentList()
        )

        return save()
    }

    fun setLoop(value: Boolean) {
        playlist = playlist.withLoop(value)
    }

    fun setShuffle(value: Boolean) {
        playlist = playlist.withShuffle(value)
    }

    fun setList(list: ImmutableList<PlaylistItem>) {
        playlist = playlist.withList(list)
    }

    suspend fun listPlaylists(): Result<List<Playlist>> = runCatching {
        val dir = storageManager.getPlaylistDirectory().getOrThrow()

        require(!dir.isFile()) { "Playlist directory is a file" }

        dir.listFiles()
            .filter { it.extension == "json" }
            .mapNotNull { dfc ->
                loadPlaylistFromFile(dfc)
            }
    }.onFailure {
        Timber.e(it, "Unable to query playlist directory")
    }

    private fun loadPlaylistFromFile(dfc: DocumentFileCompat): Playlist? = runCatching {
        context.contentResolver.openInputStream(dfc.uri)?.use { inputStream ->
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<Playlist>(jsonString)
        }
    }.onFailure {
        Timber.e(it, "Failed to load playlist: ${dfc.name}")
    }.getOrNull()

    suspend fun delete(name: String): Result<Boolean> = runCatching {
        val dir = storageManager.getPlaylistDirectory().getOrThrow()
        val playlistFile = dir.findFile("$name${Constants.SUFFIX}")
            ?: throw XmpException("Playlist not found: $name")

        playlistFile.delete()
    }.onFailure {
        Timber.e(it, "Failed to delete playlist: $name")
    }

    suspend fun listPlaylistFiles(): Result<List<DocumentFileCompat>> = runCatching {
        val dir = storageManager.getPlaylistDirectory().getOrThrow()

        require(!dir.isFile()) { "Playlist directory is a file" }

        dir.listFiles().filter { it.extension == "json" }
    }.onFailure {
        Timber.e(it, "Unable to query playlist directory")
    }
}
