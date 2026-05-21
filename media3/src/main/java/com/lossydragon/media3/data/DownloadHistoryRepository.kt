package com.lossydragon.media3.data

import com.lossydragon.media3.db.dao.DownloadHistoryDao
import com.lossydragon.media3.db.entity.DownloadHistoryEntity
import com.lossydragon.media3.model.Artist
import com.lossydragon.media3.model.ArtistInfo
import com.lossydragon.media3.model.Module
import java.lang.System

class DownloadHistoryRepository(private val dao: DownloadHistoryDao) {

    suspend fun getAll(): List<Module> = dao.getAll().map { it.toModule() }

    suspend fun add(module: Module) = dao.upsert(module.toEntity())

    suspend fun clear() = dao.clearAll()

    private fun DownloadHistoryEntity.toModule() = Module(
        id = id,
        filename = filename,
        songtitle = songTitle,
        format = format,
        bytes = bytes,
        artistInfo = ArtistInfo(
            artist = listOf(Artist(alias = artist))
        ),
    )

    private fun Module.toEntity() = DownloadHistoryEntity(
        id = id,
        filename = filename,
        songTitle = songtitle,
        format = format,
        bytes = bytes,
        artist = artist,
        viewedAt = System.currentTimeMillis(),
    )
}
