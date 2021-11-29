package org.helllabs.android.xmp.util

import java.io.File
import java.io.IOException
import java.util.*
import org.helllabs.android.xmp.Xmp.testModule
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.model.PlaylistType
import org.helllabs.android.xmp.ui.MainActivity
import org.helllabs.android.xmp.ui.playlist_selected.Playlist

object PlaylistUtils {

    private const val OPTIONS_PREFIX = "options_"
    const val SHUFFLE_MODE = "_shuffleMode"
    const val LOOP_MODE = "_loopMode"

    const val COMMENT_SUFFIX = ".comment"
    const val PLAYLIST_SUFFIX = ".playlist"
    const val DEFAULT_SHUFFLE_MODE = true
    const val DEFAULT_LOOP_MODE = false

    enum class AddFilesResult {
        RESULT_IO_EXCEPTION,
        RESULT_SINGLE_UNRECOGNIZED,
        RESULT_OK,
        RESULT_OK_VALID_ONLY,
    }

    /**
     * Send files to the specified playlist
     * @return the result of adding files [AddFilesResult]
     */
    private fun addFiles(fileList: List<String>, playlistName: String): AddFilesResult {
        var result: AddFilesResult = AddFilesResult.RESULT_OK

        val list: MutableList<PlaylistItem> = ArrayList()
        var hasInvalid = false

        for (filename in fileList) {
            val modInfo = ModInfo()
            if (testModule(filename, modInfo)) {
                val item = PlaylistItem(PlaylistType.TYPE_FILE, modInfo.name, modInfo.type)
                item.file = File(filename)
                list.add(item)
            } else {
                hasInvalid = true
            }
        }
        if (list.isNotEmpty()) {
            if (!addToList(playlistName, list)) {
                result = AddFilesResult.RESULT_IO_EXCEPTION
                return result
            }

            if (hasInvalid) {
                result = if (list.size > 1)
                    AddFilesResult.RESULT_OK_VALID_ONLY
                else
                    AddFilesResult.RESULT_SINGLE_UNRECOGNIZED
            }
        }
        renumberIds(list)

        return result
    }

    fun filesToPlaylist(fileList: List<String>, playlistName: String): AddFilesResult {
        return addFiles(fileList, playlistName)
    }

    fun filesToPlaylist(filename: String, playlistName: String): AddFilesResult {
        val fileList = listOf(filename)
        return addFiles(fileList, playlistName)
    }

    // Get all the items that are a FILE type.
    fun getFilePathList(currentList: List<PlaylistItem>): List<String> {
        val list: MutableList<String> = ArrayList()
        for (item in currentList) {
            if (item.type == PlaylistType.TYPE_FILE) {
                list.add(item.file!!.path)
            }
        }
        return list
    }

    // Get a count of any Directories in a current list.
    fun getDirectoryCount(list: List<PlaylistItem>): Int {
        var count = 0
        for (item in list) {
            if (item.type != PlaylistType.TYPE_DIRECTORY) {
                break
            }
            count++
        }
        return count
    }

    fun list(): Array<String> {
        return MainActivity.DATA_DIR.list { _, name ->
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
     * @param name     The playlist name
     * @param items    The list of playlist items to add
     * @return True if successful, false if an IOException occurred.
     */
    private fun addToList(name: String, items: List<PlaylistItem>): Boolean {
        val lines = mutableListOf<String>()
        items.forEach { playlistItem ->
            lines.add(playlistItem.toString())
        }
        try {
            FileUtils.writeToFile(File(MainActivity.DATA_DIR, name + PLAYLIST_SUFFIX), lines)
        } catch (e: IOException) {
            return false
        }

        return true
    }

    /**
     * Read comment from a playlist file.
     *
     * @param name     The playlist name
     * @return The playlist comment
     */
    fun readComment(name: String): String? {
        return try {
            FileUtils.readFromFile(Playlist.CommentFile(name))
        } catch (e: IOException) {
            // Don't care
            null
        }
    }

    fun optionName(name: String?, option: String): String {
        return OPTIONS_PREFIX + name + option
    }
}
