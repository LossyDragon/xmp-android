package com.lossydragon.media3.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lossydragon.media3.db.dao.DownloadHistoryDao
import com.lossydragon.media3.db.dao.ModuleMetadataDao
import com.lossydragon.media3.db.dao.PlaylistDao
import com.lossydragon.media3.db.entity.DownloadHistoryEntity
import com.lossydragon.media3.db.entity.ModuleMetadataEntity
import com.lossydragon.media3.db.entity.PlaylistEntity
import com.lossydragon.media3.db.entity.PlaylistEntryEntity

@Database(
    entities = [
        ModuleMetadataEntity::class,
        DownloadHistoryEntity::class,
        PlaylistEntity::class,
        PlaylistEntryEntity::class
    ],
    version = 4,
    exportSchema = true,
)
abstract class XmpDatabase : RoomDatabase() {
    abstract fun moduleMetadataDao(): ModuleMetadataDao
    abstract fun downloadHistoryDao(): DownloadHistoryDao
    abstract fun playlistDao(): PlaylistDao
}
