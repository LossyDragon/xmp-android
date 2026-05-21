package com.lossydragon.media3.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lossydragon.media3.db.entity.DownloadHistoryEntity

@Dao
interface DownloadHistoryDao {

    @Query("SELECT * FROM download_history ORDER BY viewedAt DESC")
    suspend fun getAll(): List<DownloadHistoryEntity>

    @Upsert
    suspend fun upsert(entity: DownloadHistoryEntity)

    @Query("DELETE FROM download_history")
    suspend fun clearAll()
}
