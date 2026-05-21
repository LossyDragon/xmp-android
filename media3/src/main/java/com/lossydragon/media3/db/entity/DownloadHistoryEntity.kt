package com.lossydragon.media3.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class DownloadHistoryEntity(
    @PrimaryKey val id: Int,
    val filename: String,
    val songTitle: String,
    val format: String,
    val bytes: Int,
    val artist: String,
    val viewedAt: Long
)
