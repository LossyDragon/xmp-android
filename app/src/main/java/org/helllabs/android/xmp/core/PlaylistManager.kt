package org.helllabs.android.xmp.core

import android.net.Uri
import com.lazygeniouz.dfc.file.DocumentFileCompat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.json.Json
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber

class PlaylistManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    var playlist: Playlist = Playlist()
        private set

    private var oldName: String? = null

    fun new(name: String, comment: String): Result<Boolean> {
        playlist = Playlist(
            name = name.trim(),
            comment = comment.trim()
        )

        return save()
    }

    fun load(uri: Uri): Boolean {
        if (!uri.pathSegments.last().contains(".json")) {
            Timber.w("Uri: $uri is not a .json file")
            return false
        }

        val context = XmpApplication.instance?.applicationContext ?: return false

        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                playlist = json.decodeFromString<Playlist>(jsonString)
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to load playlist from $uri")
            false
        }
    }

    fun save(): Result<Boolean> = runCatching {
        val dir = StorageManager.getPlaylistDirectory().getOrThrow()
        val context = XmpApplication.instance?.applicationContext
            ?: throw IllegalStateException("Application context is null")

        val fileName = "${playlist.name}${Constants.SUFFIX}"
        val mimeType = "application/json"

        // Create temp file first
        val tempFileName = ".tmp_${System.currentTimeMillis()}_${playlist.name}.json"
        val tempFile = dir.createFile(mimeType, tempFileName)
            ?: throw XmpException("Failed to create temp file")

        try {
            // Write to temp file
            val jsonString = json.encodeToString(Playlist.serializer(), playlist)
            context.contentResolver.openOutputStream(tempFile.uri)?.use { outputStream ->
                outputStream.writer().use { writer ->
                    writer.write(jsonString)
                    writer.flush()
                }
            } ?: throw XmpException("Failed to open output stream")

            // Delete old file if renamed
            if (!oldName.isNullOrEmpty() && oldName != playlist.name) {
                dir.findFile("$oldName${Constants.SUFFIX}")?.delete()
            } else {
                // Delete existing file with same name
                dir.findFile(fileName)?.delete()
            }

            // Rename temp to final name
            if (!tempFile.renameTo(fileName)) {
                throw XmpException("Failed to rename temp file to $fileName")
            }

            // Refresh to get updated uri after rename
            val finalFile = dir.findFile(fileName)
                ?: throw XmpException("File not found after rename: $fileName")

            // Update playlist uri only after successful write and rename
            playlist = playlist.copy(uri = finalFile.uri)
            oldName = null

            true
        } catch (e: Exception) {
            // Clean up temp file on failure
            tempFile.delete()
            throw e
        }
    }

    fun rename(newName: String, newComment: String): Result<Boolean> {
        if (newName.trim() != playlist.name) {
            oldName = playlist.name
        }

        playlist = playlist
            .withName(newName.trim())
            .withComment(newComment.trim())

        return save()
    }

    fun add(items: List<PlaylistItem>): Result<Boolean> {
        if (items.isEmpty()) return Result.success(true)

        val newList = (playlist.list + items).toPersistentList()
        playlist = playlist.withList(newList)

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

    companion object {
        fun listPlaylistsDF(): List<DocumentFileCompat> =
            StorageManager.getPlaylistDirectory().mapCatching { dir ->
                if (dir.isFile()) {
                    throw XmpException("Playlist directory is a file")
                }
                dir.listFiles()
            }.getOrElse {
                Timber.e(it, "Unable to query playlist directory")
                emptyList()
            }

        fun listPlaylists(): List<Playlist> =
            StorageManager.getPlaylistDirectory().mapCatching { dir ->
                if (dir.isFile()) {
                    throw XmpException("Playlist directory is a file")
                }

                dir.listFiles()
                    .filter { it.extension == "json" }
                    .mapNotNull { dfc ->
                        try {
                            PlaylistManager().apply { load(dfc.uri) }.playlist
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to load playlist: ${dfc.name}")
                            null
                        }
                    }
            }.getOrElse {
                Timber.e(it, "Unable to query playlist directory")
                emptyList()
            }

        fun delete(name: String): Boolean =
            StorageManager.getPlaylistDirectory().mapCatching { dir ->
                val playlist = dir.findFile("$name${Constants.SUFFIX}")
                    ?: throw XmpException("Playlist not found: $name")

                playlist.delete()
            }.getOrElse {
                Timber.e(it, "Failed to delete playlist: $name")
                false
            }
    }
}
