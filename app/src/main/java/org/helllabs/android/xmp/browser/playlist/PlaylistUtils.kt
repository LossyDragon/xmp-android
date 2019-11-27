package org.helllabs.android.xmp.browser.playlist

import android.app.Activity
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.util.ModInfo
import org.helllabs.android.xmp.util.error
import org.helllabs.android.xmp.util.toast
import java.io.File
import java.io.IOException
import java.util.*

/*
 * Send files to the specified playlist
 */
fun addFiles(activity: Activity, fileList: List<String>, playlistName: String) {
    val list = ArrayList<PlaylistItem>()
    val modInfo = ModInfo()
    var hasInvalid = false

    fileList.forEach {
        if (Xmp.testModule(it, modInfo)) {
            val item = PlaylistItem(PlaylistItem.TYPE_FILE, modInfo.name, modInfo.type)
            item.file = File(it)
            list.add(item)
        } else {
            hasInvalid = true
        }
    }

    if (list.isNotEmpty()) {
        Playlist.addToList(activity, playlistName, list)

        if (hasInvalid) {
            activity.runOnUiThread {
                if (list.size > 1) {
                    activity.toast(R.string.msg_only_valid_files_added)
                } else {
                    activity.error(R.string.unrecognized_format)
                }
            }
        }
    }

    renumberIds(list)
}

fun filesToPlaylist(activity: Activity, fileList: List<String>, playlistName: String) {
    addFiles(activity, fileList, playlistName)
}

fun filesToPlaylist(activity: Activity, filename: String, playlistName: String) {
    val fileList = arrayListOf<String>()
    fileList.add(filename)
    addFiles(activity, fileList, playlistName)
}

fun list(): Array<String> {
    return Preferences.DATA_DIR.list(PlaylistFilter()) ?: emptyArray()
}

fun listNoSuffix(): Array<String> {
    val pList = list()
    pList.forEachIndexed { index, _ ->
        pList[index] = pList[index].substring(0, pList[index].lastIndexOf(Playlist.PLAYLIST_SUFFIX))
    }
    return pList
}

fun getPlaylistName(index: Int): String {
    val pList = list()
    return pList[index].substring(0, pList[index].lastIndexOf(Playlist.PLAYLIST_SUFFIX))
}

fun createEmptyPlaylist(activity: Activity, name: String, comment: String): Boolean {
    return try {
        val playlist = Playlist(activity, name)
        playlist.comment = comment
        playlist.commit()
        true
    } catch (e: IOException) {
        activity.error(R.string.error_create_playlist)
        false
    }
}

// Stable IDs for used by Advanced RecyclerView
fun renumberIds(list: List<PlaylistItem>) {
    list.forEachIndexed { index, playlistItem ->
        playlistItem.id = index
    }
}

