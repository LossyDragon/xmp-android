package org.helllabs.android.xmp.ui.browser.playlist

import android.app.Activity
import java.io.File
import java.io.IOException
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp.testModule
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.ui.preferences.Preferences
import org.helllabs.android.xmp.util.generalError
import org.helllabs.android.xmp.util.toast

object PlaylistUtils {

    /*
     * Send files to the specified playlist
     */
    private fun addFiles(activity: Activity, fileList: List<String>, playlistName: String) {
        val list: MutableList<PlaylistItem> = ArrayList()
        val modInfo = ModInfo()
        var hasInvalid = false
        for (filename in fileList) {
            if (testModule(filename, modInfo)) {
                val item = PlaylistItem(PlaylistItem.TYPE_FILE, modInfo.name, modInfo.type)
                item.file = File(filename)
                list.add(item)
            } else {
                hasInvalid = true
            }
        }
        if (list.isNotEmpty()) {
            Playlist.addToList(activity, playlistName, list)
            activity.runOnUiThread { // Can't toast on a thread that has not called Looper.prepare()
                if (hasInvalid) {
                    if (list.size > 1) {
                        activity.toast(R.string.msg_only_valid_files_added)
                    } else {
                        activity.generalError(activity.getString(R.string.unrecognized_format))
                    }
                }
            }
        }
        renumberIds(list)
    }

    fun filesToPlaylist(activity: Activity, fileList: List<String>, playlistName: String) {
        activity.toast(R.string.msg_adding_files)
        addFiles(activity, fileList, playlistName)
    }

    fun filesToPlaylist(activity: Activity, filename: String, playlistName: String) {
        addFiles(activity, listOf(filename), playlistName)
    }

    fun list(): Array<String> {
        return Preferences.DATA_DIR.list { _, name ->
            name.endsWith(Playlist.PLAYLIST_SUFFIX)
        } ?: emptyArray()
    }

    fun listNoSuffix(): Array<String> {
        val pList = list()
        for (i in pList.indices) {
            pList[i] = pList[i].substring(0, pList[i].lastIndexOf(Playlist.PLAYLIST_SUFFIX))
        }
        return pList
    }

    fun getPlaylistName(index: Int): String {
        val pList = list()
        return pList[index].substring(0, pList[index].lastIndexOf(Playlist.PLAYLIST_SUFFIX))
    }

    fun createEmptyPlaylist(activity: Activity, name: String, comment: String): Boolean {
        return try {
            val playlist = Playlist(name)
            playlist.comment = comment
            playlist.commit()
            true
        } catch (e: IOException) {
            activity.generalError(activity.getString(R.string.error_create_playlist))
            false
        }
    }

    // Stable IDs for used by Advanced RecyclerView
    fun renumberIds(list: List<PlaylistItem>) {
        list.forEachIndexed { index, playlistItem ->
            playlistItem.id = index
        }
    }
}
