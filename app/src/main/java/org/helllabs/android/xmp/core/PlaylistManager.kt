package org.helllabs.android.xmp.core

import android.net.Uri
import com.lazygeniouz.dfc.file.DocumentFileCompat
import kotlinx.serialization.json.Json
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.PlaylistItem
import timber.log.Timber

class PlaylistManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    lateinit var playlist: Playlist

    private var oldName: String? = null

    fun new(name: String, comment: String): Result<Boolean> {
        playlist = Playlist(
            name = name.trim(),
            comment = comment.trim()
        )

        return save()
    }

    fun load(uri: Uri): Boolean {
        if (!uri.pathSegments.last().contains(".json")) {
            Timber.w("Uri: $uri is not a .json file")
            return false
        }

        val context = XmpApplication.instance!!.applicationContext
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            playlist = json.decodeFromString<Playlist>(jsonString)
        }

        return true
    }

    fun save(): Result<Boolean> = StorageManager.getPlaylistDirectory().mapCatching { dir ->
        if (!oldName.isNullOrEmpty()) {
            val oldFile = dir.findFile(oldName + Constants.SUFFIX)
            if (oldFile != null && !oldFile.delete()) {
                throw XmpException("Failed to delte old file.")
            }
        } else {
            dir.findFile(playlist.name + Constants.SUFFIX)?.delete()
        }

        val mimeType = "application/octet-stream"
        val newFile = dir.createFile(mimeType, playlist.name + Constants.SUFFIX)
            ?: throw IllegalStateException("Failed to create new file")

        playlist.uri = newFile.uri

        val jsonString = json.encodeToString(Playlist.serializer(), playlist)
        val context = XmpApplication.instance?.applicationContext
            ?: throw IllegalStateException("Application context is null")

        context.contentResolver.openOutputStream(playlist.uri)?.use { outputStream ->
            outputStream.writer().use { it.write(jsonString) }
        } ?: throw IllegalStateException("Failed to open output stream")

        true
    }

    fun rename(newName: String, newComment: String): Result<Boolean> {
        if (newName != playlist.name) {
            oldName = playlist.name
        }

        playlist.comment = newComment
        playlist.name = newName

        return save()
    }

    fun add(list: List<PlaylistItem>): Boolean {
        val newList = playlist.list.toMutableList()
        var res = newList.addAll(list)

        playlist.list = newList

        if (res) {
            res = save().isSuccess
        }

        return res
    }

    fun setLoop(value: Boolean) {
        playlist.isLoop = value
    }

    fun setShuffle(value: Boolean) {
        playlist.isShuffle = value
    }

    fun setList(list: List<PlaylistItem>) {
        playlist.list = list
    }

    companion object {
        fun listPlaylistsDF(): List<DocumentFileCompat> =
            StorageManager.getPlaylistDirectory().mapCatching { dir ->
                if (dir.isFile()) {
                    throw XmpException("Playlist dir is a file.")
                }
                dir.listFiles()
            }.getOrElse {
                Timber.e("Unable to query playlist dir")
                emptyList()
            }

        fun listPlaylists(): List<Playlist> =
            StorageManager.getPlaylistDirectory().mapCatching { dir ->
                if (dir.isFile()) {
                    throw XmpException("Playlist dir is a file")
                }

                val list = mutableListOf<Playlist>()
                for (dfc in dir.listFiles()) {
                    if (dfc.extension != "json") continue

                    PlaylistManager().run {
                        load(dfc.uri)
                        playlist
                    }.also(list::add)
                }

                list
            }.getOrElse {
                Timber.e("Unable to query playlist dir")
                emptyList()
            }

        fun delete(name: String): Boolean =
            StorageManager.getPlaylistDirectory().mapCatching { dir ->
                val playlist = dir.findFile(name + Constants.SUFFIX)
                    ?: throw XmpException("Unable to find playlist: $name")

                playlist.delete()
            }.getOrElse {
                Timber.e("Deleting playlist failed: ${it.message}")
                false
            }
    }
}
