package org.helllabs.android.xmp.ui.browser.playlist

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.io.IOException
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp.testModule
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.preferences.Preferences
import org.helllabs.android.xmp.util.FileUtils
import org.helllabs.android.xmp.util.dialogMessage
import org.helllabs.android.xmp.util.toast

object PlaylistUtils {

    private const val OPTIONS_PREFIX = "options_"
    const val SHUFFLE_MODE = "_shuffleMode"
    const val LOOP_MODE = "_loopMode"

    const val COMMENT_SUFFIX = ".comment"
    const val PLAYLIST_SUFFIX = ".playlist"
    const val DEFAULT_SHUFFLE_MODE = true
    const val DEFAULT_LOOP_MODE = false

    /*
     * Send files to the specified playlist
     */
    private fun addFiles(
        lifecycleOwner: LifecycleOwner,
        activity: Activity,
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
            addToList(lifecycleOwner, activity, playlistName, list)
            activity.runOnUiThread {
                if (hasInvalid) {
                    if (list.size > 1) {
                        activity.toast(R.string.msg_only_valid_files_added)
                    } else {
                        activity.dialogMessage(
                            lifecycleOwner = lifecycleOwner,
                            message = activity.getString(R.string.unrecognized_format)
                        )
                    }
                }
            }
        }
        renumberIds(list)
    }

    fun filesToPlaylist(
        lifecycleOwner: LifecycleOwner,
        activity: Activity,
        fileList: List<String>,
        playlistName: String
    ) {
        activity.toast(R.string.msg_adding_files)
        addFiles(lifecycleOwner, activity, fileList, playlistName)
    }

    fun filesToPlaylist(
        lifecycleOwner: LifecycleOwner,
        activity: Activity,
        filename: String,
        playlistName: String
    ) {
        addFiles(lifecycleOwner, activity, listOf(filename), playlistName)
    }

    fun list(): Array<String> {
        return Preferences.DATA_DIR.list { _, name ->
            name.endsWith(PLAYLIST_SUFFIX)
        } ?: emptyArray()
    }

    fun listNoSuffix(): Array<String> {
        val pList = list()
        for (i in pList.indices) {
            pList[i] = pList[i].substring(0, pList[i].lastIndexOf(PLAYLIST_SUFFIX))
        }
        return pList
    }

    fun getPlaylistName(index: Int): String {
        val pList = list()
        return pList[index].substring(0, pList[index].lastIndexOf(PLAYLIST_SUFFIX))
    }

    fun createEmptyPlaylist(
        lifecycleOwner: LifecycleOwner,
        activity: Activity,
        name: String,
        comment: String
    ): Boolean {
        return try {
            val playlist = Playlist(name)
            playlist.comment = comment
            playlist.commit()
            true
        } catch (e: IOException) {
            activity.dialogMessage(
                lifecycleOwner = lifecycleOwner,
                message = activity.getString(R.string.error_create_playlist)
            )
            false
        }
    }

    // Stable IDs for used by Advanced RecyclerView
    fun renumberIds(list: List<PlaylistItem>) {
        list.forEachIndexed { index, playlistItem ->
            playlistItem.id = index
        }
    }

    /**
     * Rename a playlist.
     *
     * @param oldName The current name of the playlist
     * @param newName The new name of the playlist
     * @return Whether the rename was successful
     */
    fun rename(oldName: String, newName: String): Boolean {
        val old1: File = Playlist.ListFile(oldName)
        val old2: File = Playlist.CommentFile(oldName)
        val new1: File = Playlist.ListFile(newName)
        val new2: File = Playlist.CommentFile(newName)
        var error = false
        if (!old1.renameTo(new1)) {
            error = true
        } else if (!old2.renameTo(new2)) {
            new1.renameTo(old1)
            error = true
        }
        if (error) {
            return false
        }

        PrefManager.run {
            setBooleanPref(
                optionName(newName, LOOP_MODE),
                getBooleanPref(optionName(oldName, LOOP_MODE), DEFAULT_LOOP_MODE)
            )
            setBooleanPref(
                optionName(newName, SHUFFLE_MODE),
                getBooleanPref(optionName(oldName, SHUFFLE_MODE), DEFAULT_SHUFFLE_MODE)
            )
            removeBooleanPref(optionName(oldName, SHUFFLE_MODE))
            removeBooleanPref(optionName(oldName, LOOP_MODE))
        }

        return true
    }

    /**
     * Edit a playlist's comment
     *
     * @param file The file to delete in order to rename
     * @param info The updated comment info
     *
     * @return Whether the comment rename was successful
     *
     */
    fun editComment(file: File, info: String): Boolean {
        try {
            file.delete()
            file.createNewFile()
            FileUtils.writeToFile(file, info)
        } catch (e: IOException) {
            return false
        }
        return true
    }

    /**
     * Delete the specified playlist.
     *
     * @param name    The playlist name
     */
    fun delete(name: String) {
        Playlist.ListFile(name).delete()
        Playlist.CommentFile(name).delete()
        PrefManager.removeBooleanPref(optionName(name, SHUFFLE_MODE))
        PrefManager.removeBooleanPref(optionName(name, LOOP_MODE))
    }

    /**
     * Add a list of items to the specified playlist file.
     *
     * @param activity The activity we're running
     * @param name     The playlist name
     * @param items    The list of playlist items to add
     */
    private fun addToList(
        lifecycleOwner: LifecycleOwner,
        activity: Activity,
        name: String,
        items: List<PlaylistItem>
    ) {
        val lines = mutableListOf<String>()
        items.forEach { playlistItem ->
            lines.add(playlistItem.toString())
        }
        try {
            FileUtils.writeToFile(File(Preferences.DATA_DIR, name + PLAYLIST_SUFFIX), lines)
        } catch (e: IOException) {
            activity.dialogMessage(
                lifecycleOwner = lifecycleOwner,
                message = activity.getString(R.string.error_write_to_playlist)
            )
        }
    }

    /**
     * Read comment from a playlist file.
     *
     * @param activity The activity we're running
     * @param name     The playlist name
     * @return The playlist comment
     */
    fun readComment(lifecycleOwner: LifecycleOwner, activity: Activity, name: String): String {
        var comment: String? = null
        try {
            comment = FileUtils.readFromFile(Playlist.CommentFile(name))
        } catch (e: IOException) {
            activity.dialogMessage(
                lifecycleOwner = lifecycleOwner,
                message = activity.getString(R.string.error_read_comment)
            )
        }
        if (comment == null || comment.trim { it <= ' ' }.isEmpty()) {
            comment = activity.getString(R.string.no_comment)
        }
        return comment
    }

    fun optionName(name: String?, option: String): String {
        return OPTIONS_PREFIX + name + option
    }
}
