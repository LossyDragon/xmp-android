package com.lossydragon.media3.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.lossydragon.media3.db.dao.DownloadHistoryDao
import com.lossydragon.media3.db.dao.ModuleMetadataDao
import com.lossydragon.media3.db.entity.DownloadHistoryEntity
import com.lossydragon.media3.db.entity.ModuleMetadataEntity

@Database(
    entities = [ModuleMetadataEntity::class, DownloadHistoryEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class XmpDatabase : RoomDatabase() {
    abstract fun moduleMetadataDao(): ModuleMetadataDao
    abstract fun downloadHistoryDao(): DownloadHistoryDao
}
