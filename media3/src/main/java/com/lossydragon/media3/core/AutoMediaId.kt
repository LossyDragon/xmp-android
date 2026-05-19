package com.lossydragon.media3.core

import android.net.Uri
import androidx.core.net.toUri

object AutoMediaId {
    const val ROOT = "root"
    const val FILE_BROWSER = "browser"
    const val PLAYLISTS = "playlists"

    // Encode both tree URI and doc ID: "dir::treeUri||docId"
    fun dir(treeUri: Uri, docId: String) = "dir::$treeUri||$docId"
    fun isDir(id: String) = id.startsWith("dir::")
    fun treeUriFromDir(id: String) = id.removePrefix("dir::").substringBefore("||").toUri()
    fun docIdFromDir(id: String) = id.removePrefix("dir::").substringAfter("||")

    fun file(uri: String) = "file::$uri"
    fun isFile(id: String) = id.startsWith("file::")
    fun uriFromFile(id: String) = id.removePrefix("file::")
}
