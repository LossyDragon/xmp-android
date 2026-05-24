package com.lossydragon.media3.db.entity

import androidx.room.Entity

// Junction Table
@Entity(
    tableName = "playlist_entries",
    primaryKeys = ["playlistId", "position"],
)
data class PlaylistEntryEntity(
    val playlistId: Long,
    val position: Int,
    val uri: String,
    val name: String,
    val extension: String
)
