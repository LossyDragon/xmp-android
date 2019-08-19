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


object PlaylistUtils {

    /*
	 * Send files to the specified playlist
	 */
    private fun addFiles(activity: Activity, fileList: List<String>, playlistName: String) {
        val list = ArrayList<PlaylistItem>()
        val modInfo = ModInfo()
        var hasInvalid = false

        for (filename in fileList) {
            if (Xmp.testModule(filename, modInfo)) {
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
                        activity.error(R.string.unrecognized_format)
                    }
                }
            }
        }

        renumberIds(list)
    }

    fun filesToPlaylist(activity: Activity, fileList: List<String>, playlistName: String) {

        activity.toast(text = "Please wait\nScanning module files...")

        object : Thread() {
            override fun run() {
                addFiles(activity, fileList, playlistName)
                activity.toast(text = "Scan finished")

            }
        }.start()
    }

    fun filesToPlaylist(activity: Activity, filename: String, playlistName: String) {
        val fileList = ArrayList<String>()
        fileList.add(filename)
        addFiles(activity, fileList, playlistName)
    }

    fun list(): Array<String> {
        val ret = Preferences.DATA_DIR.list(PlaylistFilter())
        return ret ?: emptyArray()
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
        for ((index, item) in list.withIndex()) {
            item.id = index
        }
    }
}
