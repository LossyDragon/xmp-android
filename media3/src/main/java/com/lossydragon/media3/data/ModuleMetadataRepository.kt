package com.lossydragon.media3.data

import android.content.Context
import android.net.Uri
import com.lossydragon.media3.db.ModuleMetadataDao
import com.lossydragon.media3.db.ModuleMetadataEntity
import java.security.MessageDigest
import org.helllabs.libxmp.Xmp
import org.helllabs.libxmp.model.ModInfo
import timber.log.Timber

class ModuleMetadataRepository(
    private val context: Context,
    private val dao: ModuleMetadataDao
) {
    suspend fun get(
        uri: Uri,
        fileName: String,
        sizeBytes: Long
    ): ModuleMetadataEntity? {
        val candidate = dao.get(fileName, sizeBytes) ?: return null

        val hash = computeHash(uri) ?: return candidate
        return if (candidate.headerHash == hash) candidate else null
    }

    suspend fun fetchAndCache(
        uri: Uri,
        fileName: String,
        sizeBytes: Long,
        extension: String
    ): ModuleMetadataEntity? {
        return try {
            val modInfo = ModInfo()
            val success = Xmp.testFromFd(context, uri, modInfo)
            if (!success) return null

            val hash = computeHash(uri) ?: return null

            val entity = ModuleMetadataEntity(
                fileName = fileName,
                sizeBytes = sizeBytes,
                headerHash = hash,
                name = modInfo.name.trim().ifBlank { fileName },
                type = modInfo.type.trim(),
                extension = extension,
            )
            dao.upsert(entity)
            entity
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    suspend fun evictStale(olderThanDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        dao.evictStale(cutoff)
    }

    suspend fun exists(fileName: String, sizeBytes: Long): Boolean =
        dao.get(fileName, sizeBytes) != null

    private fun computeHash(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(4096)
                val read = stream.read(header)
                val digest = MessageDigest.getInstance("MD5")
                digest.update(header, 0, read)
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }
}
