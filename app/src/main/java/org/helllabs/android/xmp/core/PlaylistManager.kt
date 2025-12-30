package org.helllabs.android.xmp.core

import android.content.Context
import android.net.Uri
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber

class PlaylistManager(
    private val context: Context,
    private val json: Json,
    private val storageManager: StorageManager
) {
    /**
     * Gets the playlists directory URI
     */
    private suspend fun getPlaylistsDir(): Result<Uri> =
        storageManager.getPlaylistDirectory().map { it.uri }

    /**
     * Creates a new playlist with the given name and comment.
     */
    suspend fun createPlaylist(
        name: String,
        comment: String = ""
    ): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val playlistDirResult = storageManager.getPlaylistDirectory()
            if (playlistDirResult.isFailure) {
                return@withContext Result.failure(
                    playlistDirResult.exceptionOrNull()
                        ?: Exception("Failed to get playlist directory")
                )
            }

            val playlistDir = playlistDirResult.getOrThrow()
            val sanitizedName = sanitizeFileName(name)
            val fileName = "$sanitizedName.json"

            // Check if file already exists
            if (playlistDir.findFile(fileName) != null) {
                return@withContext Result.failure(
                    IllegalArgumentException("Playlist with name '$name' already exists")
                )
            }

            // Create the file
            val newFile = playlistDir.createFile("application/json", fileName)
                ?: return@withContext Result.failure(
                    IOException("Failed to create playlist file")
                )

            val playlist = Playlist(
                name = name,
                comment = comment,
                uri = newFile.uri
            )

            // Write to file using ContentResolver
            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                outputStream.write(
                    json.encodeToString(Playlist.serializer(), playlist).toByteArray()
                )
            } ?: return@withContext Result.failure(
                IOException("Failed to open output stream")
            )

            Result.success(playlist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads a playlist from the given URI.
     */
    suspend fun loadPlaylist(uri: Uri): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes().toString(StandardCharsets.UTF_8)
            } ?: return@withContext Result.failure(
                IOException("Failed to open input stream for URI: $uri")
            )

            val playlist = json.decodeFromString(Playlist.serializer(), content)
            Result.success(playlist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves the playlist to its URI location.
     */
    suspend fun savePlaylist(playlist: Playlist): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(playlist.uri, "wt")?.use { outputStream ->
                outputStream.write(
                    json.encodeToString(Playlist.serializer(), playlist).toByteArray()
                )
            } ?: return@withContext Result.failure(
                IOException("Failed to open output stream")
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates the comment/description of a playlist.
     */
    fun setComment(playlist: Playlist, comment: String): Playlist {
        return playlist.copy(comment = comment)
    }

    /**
     * Renames a playlist and its corresponding file.
     */
    suspend fun renamePlaylist(
        playlist: Playlist,
        newName: String
    ): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val playlistDir = storageManager.getPlaylistDirectory().getOrThrow()
            val sanitizedName = sanitizeFileName(newName)
            val newFileName = "$sanitizedName.json"

            // Check if a file with the new name already exists
            val existingFile = playlistDir.findFile(newFileName)
            val currentFile = storageManager.getDocumentFileFromUri(playlist.uri)

            if (existingFile != null && existingFile.uri != playlist.uri) {
                return@withContext Result.failure(
                    IllegalArgumentException("Playlist with name '$newName' already exists")
                )
            }

            // Create new file
            val newFile = playlistDir.createFile("application/json", newFileName)
                ?: return@withContext Result.failure(
                    IOException("Failed to create new playlist file")
                )

            val updatedPlaylist = playlist.copy(
                name = newName,
                uri = newFile.uri
            )

            // Write to new file
            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                outputStream.write(
                    json.encodeToString(Playlist.serializer(), updatedPlaylist).toByteArray()
                )
            }

            // Delete old file if different
            if (currentFile != null && currentFile.uri != newFile.uri) {
                currentFile.delete()
            }

            Result.success(updatedPlaylist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(file: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val playlist = loadPlaylist(file)
        deletePlaylist(playlist.getOrThrow())
    }

    /**
     * Deletes a playlist file.
     */
    suspend fun deletePlaylist(playlist: Playlist): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val success = storageManager.deleteFileOrDirectory(playlist.uri)

            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Failed to delete playlist file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists all available playlists.
     */
    suspend fun listAllPlaylists(): Result<List<Playlist>> = withContext(Dispatchers.IO) {
        try {
            val playlistDir = storageManager.getPlaylistDirectory().getOrThrow()
            val playlistUris =
                storageManager.walkDownDirectory(playlistDir.uri, includeDirectories = false)

            val playlists = playlistUris.mapNotNull { uri ->
                val fileName = storageManager.getFileName(uri) ?: return@mapNotNull null

                // Only process .json files
                if (!fileName.endsWith(".json", ignoreCase = true)) {
                    return@mapNotNull null
                }

                try {
                    loadPlaylist(uri).getOrNull()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load playlist: $fileName")
                    null
                }
            }

            Result.success(playlists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Keep all the other methods (setLoop, setShuffle, addItem, etc.) as they were
    // since they don't interact with files directly

    fun setLoop(playlist: Playlist, isLoop: Boolean): Playlist {
        return playlist.copy(isLoop = isLoop)
    }

    fun setShuffle(playlist: Playlist, isShuffle: Boolean): Playlist {
        return playlist.copy(isShuffle = isShuffle)
    }

    fun setUseFileName(playlist: Playlist, useFileName: Boolean): Playlist {
        return playlist.copy(useFileName = useFileName)
    }

    fun addItem(playlist: Playlist, item: PlaylistItem): Playlist {
        val newList = playlist.list.toMutableList().apply {
            add(item.copy(id = size))
        }.toImmutableList()

        return playlist.copy(list = newList)
    }

    fun addItems(playlist: Playlist, items: List<PlaylistItem>): Playlist {
        val currentSize = playlist.list.size
        val newList = playlist.list.toMutableList().apply {
            addAll(
                items.mapIndexed { index, item ->
                    item.copy(id = currentSize + index)
                }
            )
        }.toImmutableList()

        return playlist.copy(list = newList)
    }

    fun removeItem(playlist: Playlist, itemId: Int): Playlist {
        val newList = playlist.list
            .filterNot { it.id == itemId }
            .mapIndexed { index, item -> item.copy(id = index) }
            .toImmutableList()

        return playlist.copy(list = newList)
    }

    fun removeItemAt(playlist: Playlist, index: Int): Playlist {
        if (index !in playlist.list.indices) {
            return playlist
        }

        val newList = playlist.list.toMutableList().apply {
            removeAt(index)
        }.mapIndexed { idx, item -> item.copy(id = idx) }
            .toImmutableList()

        return playlist.copy(list = newList)
    }

    fun moveItem(playlist: Playlist, fromIndex: Int, toIndex: Int): Playlist {
        if (fromIndex !in playlist.list.indices || toIndex !in playlist.list.indices) {
            return playlist
        }

        val newList = playlist.list.toMutableList().apply {
            val item = removeAt(fromIndex)
            add(toIndex, item)
        }.mapIndexed { index, item -> item.copy(id = index) }
            .toImmutableList()

        return playlist.copy(list = newList)
    }

    fun reorderPlaylist(playlist: Playlist, newOrder: ImmutableList<PlaylistItem>): Playlist {
        val reindexed = newOrder.mapIndexed { index, item ->
            item.copy(id = index)
        }.toImmutableList()

        return playlist.copy(list = reindexed)
    }

    fun clearPlaylist(playlist: Playlist): Playlist {
        return playlist.copy(list = persistentListOf())
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
