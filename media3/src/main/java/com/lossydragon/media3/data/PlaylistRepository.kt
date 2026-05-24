package com.lossydragon.media3.data

import androidx.core.net.toUri
import com.lossydragon.media3.db.dao.PlaylistDao
import com.lossydragon.media3.db.entity.PlaylistEntity
import com.lossydragon.media3.db.entity.PlaylistEntryEntity
import com.lossydragon.media3.model.ModuleFile
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(private val dao: PlaylistDao) {

    val playlists: Flow<List<PlaylistEntity>> = dao.getAllPlaylists()

    suspend fun createPlaylist(entity: PlaylistEntity): Long = dao.createPlaylist(entity)

    suspend fun createPlaylist(name: String, comment: String): Long =
        dao.createPlaylist(PlaylistEntity(name = name, comment = comment))

    suspend fun addToPlaylist(playlistId: Long, file: ModuleFile) {
        val entries = dao.getEntries(playlistId)
        dao.addEntry(
            PlaylistEntryEntity(
                playlistId = playlistId,
                position = entries.size,
                uri = file.uri.toString(),
                name = file.resolvedName.ifBlank { file.name },
                extension = file.extension,
            )
        )
    }

    suspend fun getPlaylistFiles(playlistId: Long): List<ModuleFile> =
        dao.getEntries(playlistId).map {
            ModuleFile(
                uri = it.uri.toUri(),
                name = it.name,
                sizeBytes = 0L,
                extension = it.extension,
            )
        }

    suspend fun deletePlaylist(id: Long) {
        dao.clearEntries(id)
        dao.deletePlaylist(id)
    }

    suspend fun getAllPlaylistsOnce(): List<PlaylistEntity> =
        dao.getAllPlaylistsOnce() // add non-Flow version to DAO

    suspend fun getEntriesRaw(playlistId: Long): List<PlaylistEntryEntity> =
        dao.getEntries(playlistId)

    suspend fun addRawEntry(entry: PlaylistEntryEntity) =
        dao.addEntry(entry)

    suspend fun renamePlaylist(id: Long, name: String, comment: String) =
        dao.updatePlaylist(id, name, comment)

    suspend fun removeFromPlaylist(playlistId: Long, uri: String) =
        dao.removeEntry(playlistId, uri)

    suspend fun reorderEntries(playlistId: Long, files: List<ModuleFile>) {
        dao.clearEntries(playlistId)
        files.forEachIndexed { index, file ->
            dao.addEntry(
                PlaylistEntryEntity(
                    playlistId = playlistId,
                    position = index,
                    uri = file.uri.toString(),
                    name = file.resolvedName.ifBlank { file.name },
                    extension = file.extension,
                )
            )
        }
    }
}
