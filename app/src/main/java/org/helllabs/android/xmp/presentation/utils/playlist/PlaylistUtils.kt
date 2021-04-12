package org.helllabs.android.xmp.presentation.utils.playlist

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.io.IOException
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp.testModule
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.presentation.ui.preferences.Preferences
import org.helllabs.android.xmp.util.errorDialog
import org.helllabs.android.xmp.util.toast

object PlaylistUtils {

    // Get all the items that are a FILE type.
    fun getFilePathList(currentList: List<PlaylistItem>): List<String> {
        val list: MutableList<String> = ArrayList()
        for (item in currentList) {
            if (item.type == PlaylistItem.TYPE_FILE) {
                list.add(item.file!!.path)
            }
        }
        return list
    }

    // Get a count of any Directories in a current list.
    fun getDirectoryCount(list: List<PlaylistItem>): Int {
        var count = 0
        for (item in list) {
            if (item.type != PlaylistItem.TYPE_DIRECTORY) {
                break
            }
            count++
        }
        return count
    }

    /*
     * Send files to the specified playlist
     */
    private fun addFiles(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        fileList: List<String>,
        playlistName: String
    ) {
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
            if (!Playlist.addToList(playlistName, list)) {
                activity.errorDialog(
                    lifecycleOwner,
                    activity.getString(R.string.error_write_to_playlist)
                ) {}
            }
            activity.runOnUiThread { // Can't toast on a thread that has not called Looper.prepare()
                if (hasInvalid) {
                    if (list.size > 1) {
                        activity.toast(R.string.msg_only_valid_files_added)
                    } else {
                        activity.errorDialog(
                            lifecycleOwner,
                            activity.getString(R.string.unrecognized_format)
                        ) {}
                    }
                }
            }
        }
        renumberIds(list)
    }

    fun filesToPlaylist(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        fileList: List<String>,
        playlistName: String
    ) {
        activity.toast(R.string.msg_adding_files)
        addFiles(activity, lifecycleOwner, fileList, playlistName)
    }

    fun filesToPlaylist(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        filename: String,
        playlistName: String
    ) {
        addFiles(activity, lifecycleOwner, listOf(filename), playlistName)
    }

    fun list(): Array<String> {
        return Preferences.DATA_DIR.list { _, name ->
            name.endsWith(Playlist.PLAYLIST_SUFFIX)
        } ?: emptyArray()
    }

    fun listNoSuffix(): Array<String> {
        val pList = mutableListOf<String>()
        list().forEach {
            pList.add(it.substring(0, it.lastIndexOf(Playlist.PLAYLIST_SUFFIX)))
        }
        return pList.toTypedArray()
    }

    fun getPlaylistName(index: Int): String {
        val pList = list()
        return pList[index].substring(0, pList[index].lastIndexOf(Playlist.PLAYLIST_SUFFIX))
    }

    fun createEmptyPlaylist(name: String, comment: String): Boolean {
        return try {
            val playlist = Playlist(name)
            playlist.comment = comment
            playlist.commit()
            true
        } catch (e: IOException) {
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
