package com.lossydragon.media3.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
