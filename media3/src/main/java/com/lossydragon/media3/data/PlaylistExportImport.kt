package com.lossydragon.media3.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.lossydragon.media3.db.entity.PlaylistEntity
import com.lossydragon.media3.db.entity.PlaylistEntryEntity
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

@Serializable
data class PlaylistExport(
    val version: Int = 1,
    val exportedAt: String = Instant.now().toString(),
    val playlists: List<PlaylistExportItem>
)

@Serializable
data class PlaylistExportItem(
    val name: String,
    val comment: String,
    val createdAt: Long,
    val entries: List<PlaylistEntryExport>
)

@Serializable
data class PlaylistEntryExport(
    val position: Int,
    val uri: String,
    val name: String,
    val extension: String
)

data class ImportResult(
    val playlistsImported: Int,
    val entriesImported: Int,
    val skipped: Int // entries whose URIs are no longer accessible
)

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Serializes [playlists] and their [entries] to a JSON string.
 *
 * @param playlists List of [PlaylistEntity] to export.
 * @param entries Map of playlistId → list of [PlaylistEntryEntity].
 */
fun exportPlaylistsToJson(
    playlists: List<PlaylistEntity>,
    entries: Map<Long, List<PlaylistEntryEntity>>
): String {
    val export = PlaylistExport(
        playlists = playlists.map { playlist ->
            PlaylistExportItem(
                name = playlist.name,
                comment = playlist.comment,
                createdAt = playlist.createdAt,
                entries = (entries[playlist.id] ?: emptyList()).map { entry ->
                    PlaylistEntryExport(
                        position = entry.position,
                        uri = entry.uri,
                        name = entry.name,
                        extension = entry.extension,
                    )
                },
            )
        }
    )
    return json.encodeToString(export)
}

/**
 * Writes the JSON string to [outputUri] via SAF.
 * Call from a coroutine on [kotlinx.coroutines.Dispatchers.IO].
 */
fun writeExportToUri(
    context: Context,
    outputUri: Uri,
    jsonString: String
): Result<Unit> = runCatching {
    context.contentResolver.openOutputStream(outputUri)?.use { stream ->
        stream.write(jsonString.toByteArray(Charsets.UTF_8))
    } ?: error("Could not open output stream for $outputUri")
}

/**
 * Reads JSON from [inputUri] and returns parsed [PlaylistExport].
 * Call from a coroutine on [kotlinx.coroutines.Dispatchers.IO].
 */
fun readImportFromUri(
    context: Context,
    inputUri: Uri
): Result<PlaylistExport> = runCatching {
    val jsonString = context.contentResolver.openInputStream(inputUri)?.use { stream ->
        stream.readBytes().toString(Charsets.UTF_8)
    } ?: error("Could not open input stream for $inputUri")

    json.decodeFromString<PlaylistExport>(jsonString)
}

/**
 * Converts a [PlaylistExport] into [PlaylistEntity] and [PlaylistEntryEntity] pairs
 * ready to insert into Room.
 *
 * Optionally validates that each entry URI is still accessible via [context].
 * Set [validateUris] to false to import all entries regardless.
 */
fun PlaylistExport.toRoomEntities(
    context: Context,
    validateUris: Boolean = true
): Triple<List<PlaylistEntity>, Map<String, List<PlaylistEntryEntity>>, ImportResult> {
    var totalEntries = 0
    var skipped = 0

    val playlistEntities = mutableListOf<PlaylistEntity>()
    // keyed by playlist name since IDs aren't known until Room inserts
    val entryMap = mutableMapOf<String, List<PlaylistEntryEntity>>()

    for (importedPlaylist in playlists) {
        val playlist = PlaylistEntity(
            name = importedPlaylist.name,
            comment = importedPlaylist.comment,
            createdAt = importedPlaylist.createdAt,
        )
        playlistEntities.add(playlist)

        val validEntries = importedPlaylist.entries.mapNotNull { entry ->
            totalEntries++
            if (validateUris && !entry.uri.isUriAccessible(context)) {
                Timber.w("Skipping inaccessible URI: ${entry.uri}")
                skipped++
                return@mapNotNull null
            }
            // ID will be set after Room insert — use 0 as placeholder
            PlaylistEntryEntity(
                playlistId = 0L,
                position = entry.position,
                uri = entry.uri,
                name = entry.name,
                extension = entry.extension,
            )
        }
        entryMap[importedPlaylist.name] = validEntries
    }

    return Triple(
        playlistEntities,
        entryMap,
        ImportResult(
            playlistsImported = playlistEntities.size,
            entriesImported = totalEntries - skipped,
            skipped = skipped,
        )
    )
}

/**
 * Checks whether a SAF URI is still accessible.
 * Queries the content resolver — returns false if the file no longer exists
 * or the app no longer holds permission.
 */
private fun String.isUriAccessible(context: Context): Boolean = try {
    val uri = this.toUri()
    context.contentResolver.query(
        uri,
        arrayOf(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID),
        null,
        null,
        null,
    )?.use { it.count >= 0 } ?: false
} catch (e: Exception) {
    Timber.w("URI accessibility check failed: $this — ${e.message}")
    false
}
