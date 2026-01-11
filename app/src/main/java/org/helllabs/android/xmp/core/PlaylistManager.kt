package org.helllabs.android.xmp.core

import android.content.Context
import android.net.Uri
import com.lazygeniouz.dfc.file.DocumentFileCompat
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
     * Creates a new playlist with the given name and comment.
     */
    suspend fun createPlaylist(
        name: String,
        comment: String = ""
    ): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val playlistDir = storageManager.getPlaylistsRootDirectory().getOrThrow()

            val xmpDir = playlistDir.findFile("xmp") ?: playlistDir.createDirectory("xmp")
                ?: throw XmpException("Can't create xmp dir")

            val sanitizedName = sanitizeFileName(name)
            val fileName = "$sanitizedName${Constants.SUFFIX}"

            // Check if file already exists
            if (xmpDir.findFile(fileName) != null) {
                return@withContext Result.failure(
                    IllegalArgumentException("Playlist with name '$name' already exists")
                )
            }

            val playlist = Playlist(
                name = name,
                comment = comment,
            )

            // Create and write to file
            val file = xmpDir.createFile("application/json", fileName)
                ?: throw XmpException("Can't create file")

            context.contentResolver.openOutputStream(file.uri)?.use {
                it.write(json.encodeToString(Playlist.serializer(), playlist).toByteArray())
            }

            Result.success(playlist)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create playlist: $name")
            Result.failure(e)
        }
    }

    /**
     * Loads a playlist from the given URI.
     */
    suspend fun loadPlaylist(uri: Uri): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            if (uri == Uri.EMPTY || uri.toString().isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid URI: URI is empty or null")
                )
            }

            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes().toString(StandardCharsets.UTF_8)
            } ?: return@withContext Result.failure(
                IOException("Failed to open input stream for URI: $uri")
            )

            val playlist = json.decodeFromString(Playlist.serializer(), content)

            val indexedPlaylist = playlist.copy(
                list = playlist.list.mapIndexed { index, item ->
                    item.copy(id = index)
                }.toImmutableList()
            )

            Result.success(indexedPlaylist)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load playlist from URI: $uri")
            Result.failure(e)
        }
    }

    /**
     * Saves the playlist to its URI location.
     */
    suspend fun savePlaylist(uri: Uri, playlist: Playlist): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    outputStream.write(
                        json.encodeToString(Playlist.serializer(), playlist).toByteArray()
                    )
                } ?: return@withContext Result.failure(
                    IOException("Failed to open output stream")
                )

                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save playlist to URI: $uri")
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
        uri: Uri,
        playlist: Playlist,
        newName: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val playlistDir = storageManager.getPlaylistsRootDirectory().getOrThrow()

            val xmpDir = playlistDir.findFile("xmp")
                ?: throw XmpException("xmp dir not found")

            val sanitizedName = sanitizeFileName(newName)
            val newFileName = "$sanitizedName${Constants.SUFFIX}"

            // Check if a file with the new name already exists
            if (xmpDir.findFile(newFileName) != null) {
                return@withContext Result.failure(
                    IllegalArgumentException("Playlist with name '$newName' already exists")
                )
            }

            // Get current file
            val currentFile = DocumentFileCompat.fromSingleUri(context, uri)
                ?: throw IOException("Invalid playlist URI")

            if (!currentFile.exists()) {
                throw IOException("Current playlist file not found")
            }

            // SAF doesn't support direct rename.

            val updatedPlaylist = playlist.copy(name = newName)

            val newFile = xmpDir.createFile("application/json", newFileName)
                ?: throw IOException("Failed to create new file")

            context.contentResolver.openOutputStream(newFile.uri)?.use {
                it.write(json.encodeToString(Playlist.serializer(), updatedPlaylist).toByteArray())
            }

            currentFile.delete()

            Result.success(newFile.uri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to rename playlist: $uri")
            Result.failure(e)
        }
    }

    /**
     * Deletes a playlist file.
     */
    suspend fun deletePlaylist(uri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = DocumentFileCompat.fromSingleUri(context, uri)
                ?: throw IOException("Invalid playlist URI")

            Result.success(file.delete())
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete playlist: $uri")
            Result.failure(e)
        }
    }

    /**
     * Lists all available playlists.
     */
    suspend fun listAllPlaylists(): Result<List<Pair<Playlist, Uri>>> = withContext(
        Dispatchers.IO
    ) {
        try {
            val playlistDir = storageManager.getPlaylistsRootDirectory().getOrThrow()

            val xmpDir = playlistDir.findFile("xmp")
                ?: return@withContext Result.success(emptyList())

            val playlistFiles = xmpDir.listFiles()
                .filter { it.isFile() && it.name.endsWith(Constants.SUFFIX, ignoreCase = true) }

            val playlists = playlistFiles.mapNotNull { file ->
                try {
                    loadPlaylist(file.uri).getOrNull()?.let { playlist ->
                        Pair(playlist, file.uri)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load playlist: ${file.name}")
                    null
                }
            }

            Result.success(playlists)
        } catch (e: Exception) {
            Timber.e(e, "Failed to list playlists")
            Result.failure(e)
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    companion object {

        fun Playlist.clearPlaylist(): Playlist =
            this.copy(list = persistentListOf())

        fun Playlist.setLoop(isLoop: Boolean): Playlist =
            this.copy(isLoop = isLoop)

        fun Playlist.setShuffle(isShuffle: Boolean): Playlist =
            this.copy(isShuffle = isShuffle)

        fun Playlist.setUseFileName(useFileName: Boolean): Playlist =
            this.copy(useFileName = useFileName)

        fun Playlist.addItem(item: PlaylistItem): Playlist {
            val newList = this.list.toMutableList().apply {
                add(item.copy(id = size))
            }.toImmutableList()

            return this.copy(list = newList)
        }

        fun Playlist.addItems(items: List<PlaylistItem>): Playlist {
            val currentSize = this.list.size
            val newList = this.list.toMutableList().apply {
                addAll(
                    items.mapIndexed { index, item ->
                        item.copy(id = currentSize + index)
                    }
                )
            }.toImmutableList()

            return this.copy(list = newList)
        }

        fun Playlist.removeItem(itemId: Int): Playlist {
            val newList = this.list
                .filterNot { it.id == itemId }
                .mapIndexed { index, item -> item.copy(id = index) }
                .toImmutableList()

            return this.copy(list = newList)
        }

        fun Playlist.removeItemAt(index: Int): Playlist {
            if (index !in this.list.indices) {
                return this
            }

            val newList = this.list.toMutableList().apply {
                removeAt(index)
            }.mapIndexed { idx, item -> item.copy(id = idx) }
                .toImmutableList()

            return this.copy(list = newList)
        }

        fun Playlist.moveItem(fromIndex: Int, toIndex: Int): Playlist {
            if (fromIndex !in this.list.indices || toIndex !in this.list.indices) {
                return this
            }

            val newList = this.list.toMutableList().apply {
                val item = removeAt(fromIndex)
                add(toIndex, item)
            }.mapIndexed { index, item -> item.copy(id = index) }
                .toImmutableList()

            return this.copy(list = newList)
        }

        fun Playlist.reorderPlaylist(newOrder: ImmutableList<PlaylistItem>): Playlist {
            val reindexed = newOrder.mapIndexed { index, item ->
                item.copy(id = index)
            }.toImmutableList()

            return this.copy(list = reindexed)
        }
    }
}
