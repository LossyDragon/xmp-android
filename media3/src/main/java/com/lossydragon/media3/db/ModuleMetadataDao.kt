package com.lossydragon.media3.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ModuleMetadataDao {

    @Query(
        "SELECT * FROM module_metadata WHERE fileName = :fileName AND sizeBytes = :sizeBytes LIMIT 1"
    )
    suspend fun get(fileName: String, sizeBytes: Long): ModuleMetadataEntity?

    @Query("SELECT * FROM module_metadata WHERE headerHash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): ModuleMetadataEntity?

    @Upsert
    suspend fun upsert(entity: ModuleMetadataEntity)

    @Query("DELETE FROM module_metadata WHERE lastSeen < :cutoff")
    suspend fun evictStale(cutoff: Long)
}
