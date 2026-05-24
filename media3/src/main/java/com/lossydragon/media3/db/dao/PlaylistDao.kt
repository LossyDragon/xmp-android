package com.lossydragon.media3.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lossydragon.media3.db.entity.PlaylistEntity
import com.lossydragon.media3.db.entity.PlaylistEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_entries WHERE playlistId = :id ORDER BY position")
    suspend fun getEntries(id: Long): List<PlaylistEntryEntity>

    @Insert
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Insert
    suspend fun addEntry(entry: PlaylistEntryEntity)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId AND uri = :uri")
    suspend fun removeEntry(playlistId: Long, uri: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :id")
    suspend fun clearEntries(id: Long)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getAllPlaylistsOnce(): List<PlaylistEntity>

    @Query("UPDATE playlists SET name = :name, comment = :comment WHERE id = :id")
    suspend fun updatePlaylist(id: Long, name: String, comment: String)
}
