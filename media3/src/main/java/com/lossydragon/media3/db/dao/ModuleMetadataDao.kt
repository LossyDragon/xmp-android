package com.lossydragon.media3.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lossydragon.media3.db.entity.ModuleMetadataEntity

@Dao
interface ModuleMetadataDao {

    @Query("SELECT * FROM module_metadata WHERE fileName IN (:fileNames)")
    suspend fun getByFileNames(fileNames: List<String>): List<ModuleMetadataEntity>

    @Query(
        "SELECT * FROM module_metadata WHERE fileName = :fileName AND sizeBytes = :sizeBytes LIMIT 1"
    )
    suspend fun get(fileName: String, sizeBytes: Long): ModuleMetadataEntity?

    @Upsert
    suspend fun upsert(entity: ModuleMetadataEntity)

    @Query("DELETE FROM module_metadata WHERE lastSeen < :cutoff")
    suspend fun evictStale(cutoff: Long)
}
