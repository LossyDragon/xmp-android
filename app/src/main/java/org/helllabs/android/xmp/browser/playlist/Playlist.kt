package org.helllabs.android.xmp.browser.playlist

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.ArrayList

import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.util.FileUtils
import org.helllabs.android.xmp.util.InfoCache
import org.helllabs.android.xmp.util.Log

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.helllabs.android.xmp.util.error

class Playlist @Throws(IOException::class)
constructor(context: Context, val name: String) {
    var comment: String? = null
    private var mListChanged: Boolean = false
    private var mCommentChanged: Boolean = false
    var isShuffleMode: Boolean = false
    var isLoopMode: Boolean = false
    private val mList: MutableList<PlaylistItem>
    private val mPrefs: SharedPreferences

    val list: List<PlaylistItem>
        get() = mList

    private class ListFile : File {
        constructor(name: String) : super(Preferences.DATA_DIR, name + PLAYLIST_SUFFIX)

        constructor(name: String, suffix: String) : super(Preferences.DATA_DIR, name + PLAYLIST_SUFFIX + suffix)
    }

    private class CommentFile : File {
        constructor(name: String) : super(Preferences.DATA_DIR, name + COMMENT_SUFFIX)

        constructor(name: String, suffix: String) : super(Preferences.DATA_DIR, name + COMMENT_SUFFIX + suffix)
    }

    init {
        mList = ArrayList()
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        val file = ListFile(name)
        if (file.exists()) {
            Log.i(TAG, "Read playlist $name")
            val comment = FileUtils.readFromFile(CommentFile(name))

            // read list contents
            if (readList(name)) {
                this.comment = comment
                isShuffleMode = readShuffleModePref(name)
                isLoopMode = readLoopModePref(name)
            }
        } else {
            Log.i(TAG, "New playlist $name")
            isShuffleMode = DEFAULT_SHUFFLE_MODE
            isLoopMode = DEFAULT_LOOP_MODE
            mListChanged = true
            mCommentChanged = true
        }

        if (comment == null) {
            comment = ""
        }
    }

    /**
     * Save the current playlist.
     */
    fun commit() {
        Log.i(TAG, "Commit playlist $name")
        if (mListChanged) {
            writeList(name)
            mListChanged = false
        }
        if (mCommentChanged) {
            writeComment(name)
            mCommentChanged = false
        }

        var saveModes = false
        if (isShuffleMode != readShuffleModePref(name)) {
            saveModes = true
        }
        if (isLoopMode != readLoopModePref(name)) {
            saveModes = true
        }
        if (saveModes) {
            val editor = mPrefs.edit()
            editor.putBoolean(optionName(name, SHUFFLE_MODE), isShuffleMode)
            editor.putBoolean(optionName(name, LOOP_MODE), isLoopMode)
            editor.apply()
        }
    }

    /**
     * Remove an item from the playlist.
     *
     * @param index The index of the item to be removed
     */
    fun remove(index: Int) {
        Log.i(TAG, "Remove item #" + index + ": " + mList[index].name)
        mList.removeAt(index)
        mListChanged = true
    }


    // Helper methods

    private fun readList(name: String): Boolean {
        mList.clear()

        val file = ListFile(name)
        var lineNum: Int
        val invalidList = ArrayList<Int>()

        try {
            val reader = BufferedReader(FileReader(file), 512)
            lineNum = 0

            reader.forEachLine {
                val fields = it.split(":".toRegex(), 3).toTypedArray()
                val filename = fields[0]
                val comment = if (fields.size > 1) fields[1] else ""
                val title = if (fields.size > 2) fields[2] else ""

                if (InfoCache.fileExists(filename)) {
                    val item = PlaylistItem(PlaylistItem.TYPE_FILE, title, comment)
                    item.file = File(filename)
                    item.imageRes = R.drawable.ic_grabber
                    mList.add(item)
                } else {
                    invalidList.add(lineNum)
                }
                lineNum++
            }

            reader.close()
            PlaylistUtils.renumberIds(mList)
        } catch (e: IOException) {
            Log.e(TAG, "Error reading playlist " + file.path)
            return false
        }

        if (invalidList.isNotEmpty()) {
            val array = IntArray(invalidList.size)
            val iterator = invalidList.iterator()
            for (i in array.indices) {
                array[i] = iterator.next()
            }

            try {
                FileUtils.removeLineFromFile(file, array)
            } catch (e: FileNotFoundException) {
                Log.e(TAG, "Playlist file " + file.path + " not found")
            } catch (e: IOException) {
                Log.e(TAG, "I/O error removing invalid lines from " + file.path)
            }

        }

        return true
    }

    private fun writeList(name: String) {
        Log.i(TAG, "Write list")
        val file = ListFile(name, ".new")
        file.delete()

        try {
            val out = BufferedWriter(FileWriter(file), 512)
            for (item in mList) {
                out.write(item.toString())
            }
            out.close()

            val oldFile = ListFile(name)
            oldFile.delete()
            file.renameTo(oldFile)
        } catch (e: IOException) {
            Log.e(TAG, "Error writing playlist file " + file.path)
        }

    }

    private fun writeComment(name: String) {
        Log.i(TAG, "Write comment")
        val file = CommentFile(name, ".new")
        file.delete()
        try {
            FileUtils.writeToFile(file, comment!!)
            val oldFile = CommentFile(name)
            oldFile.delete()
            file.renameTo(oldFile)
        } catch (e: IOException) {
            Log.e(TAG, "Error writing comment file " + file.path)
        }

    }

    private fun readShuffleModePref(name: String): Boolean {
        return mPrefs.getBoolean(optionName(name, SHUFFLE_MODE), DEFAULT_SHUFFLE_MODE)
    }

    private fun readLoopModePref(name: String): Boolean {
        return mPrefs.getBoolean(optionName(name, LOOP_MODE), DEFAULT_LOOP_MODE)
    }

    fun setListChanged(listChanged: Boolean) {
        mListChanged = listChanged
    }

    companion object {
        private const val TAG = "Playlist"
        const val COMMENT_SUFFIX = ".comment"
        const val PLAYLIST_SUFFIX = ".playlist"
        private const val OPTIONS_PREFIX = "options_"
        private const val SHUFFLE_MODE = "_shuffleMode"
        private const val LOOP_MODE = "_loopMode"
        private const val DEFAULT_SHUFFLE_MODE = true
        private const val DEFAULT_LOOP_MODE = false

        // Static utilities
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
         * Rename a playlist.
         *
         * @param context The context we're running in
         * @param oldName The current name of the playlist
         * @param newName The new name of the playlist
         *
         * @return Whether the rename was successful
         */
        fun rename(context: Context, oldName: String, newName: String): Boolean {
            val old1 = ListFile(oldName)
            val old2 = CommentFile(oldName)
            val new1 = ListFile(newName)
            val new2 = CommentFile(newName)

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

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = prefs.edit()
            editor.putBoolean(optionName(newName, SHUFFLE_MODE), prefs.getBoolean(optionName(oldName, SHUFFLE_MODE), DEFAULT_SHUFFLE_MODE))
            editor.putBoolean(optionName(newName, LOOP_MODE), prefs.getBoolean(optionName(oldName, LOOP_MODE), DEFAULT_LOOP_MODE))
            editor.remove(optionName(oldName, SHUFFLE_MODE))
            editor.remove(optionName(oldName, LOOP_MODE))
            editor.apply()

            return true
        }

        /**
         * Delete the specified playlist.
         *
         * @param context The context the playlist is being created in
         * @param name The playlist name
         */
        fun delete(context: Context, name: String) {
            ListFile(name).delete()
            CommentFile(name).delete()

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = prefs.edit()
            editor.remove(optionName(name, SHUFFLE_MODE))
            editor.remove(optionName(name, LOOP_MODE))
            editor.apply()
        }

        /**
         * Add a list of items to the specified playlist file.
         *
         * @param activity The activity we're running
         * @param name The playlist name
         * @param items The list of playlist items to add
         */
        fun addToList(activity: Activity, name: String, items: List<PlaylistItem>) {
            val lines: MutableList<String> = mutableListOf()

            for (item in items) {
                lines.add(item.toString())
            }

            try {
                FileUtils.writeToFile(File(Preferences.DATA_DIR, name + PLAYLIST_SUFFIX), lines.toTypedArray())
            } catch (e: IOException) {
                activity.error(R.string.error_write_to_playlist)
            }
        }

        /**
         * Read comment from a playlist file.
         *
         * @param activity The activity we're running
         * @param name The playlist name
         *
         * @return The playlist comment
         */
        fun readComment(activity: Activity, name: String): String {
            var comment: String? = null
            try {
                comment = FileUtils.readFromFile(CommentFile(name))
            } catch (e: IOException) {
                activity.error(R.string.error_read_comment)
            }

            if (comment == null || comment.trim { it <= ' ' }.isEmpty()) {
                comment = activity.getString(R.string.no_comment)
            }
            return comment
        }

        private fun optionName(name: String, option: String): String {
            return OPTIONS_PREFIX + name + option
        }
    }
}
