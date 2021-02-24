package org.helllabs.android.xmp.browser.playlist

import android.app.Activity
import android.app.ProgressDialog
import java.io.File
import java.io.IOException
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp.testModule
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.util.ModInfo
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
            if (hasInvalid) {
                activity.runOnUiThread {
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
        val progressDialog = ProgressDialog.show(
            activity,
            "Please wait",
            "Scanning module files...",
            true
        )
        object : Thread() {
            override fun run() {
                addFiles(activity, fileList, playlistName)
                progressDialog.dismiss()
            }
        }.start()
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
