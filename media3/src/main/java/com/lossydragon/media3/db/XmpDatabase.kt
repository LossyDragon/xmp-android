package com.lossydragon.media3.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ModuleMetadataEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class XmpDatabase : RoomDatabase() {
    abstract fun moduleMetadataDao(): ModuleMetadataDao
}
