package org.helllabs.android.xmp.browser.playlist

import android.app.Activity
import android.content.*
import androidx.preference.PreferenceManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.preferences.Preferences
import org.helllabs.android.xmp.util.FileUtils.readFromFile
import org.helllabs.android.xmp.util.FileUtils.removeLineFromFile
import org.helllabs.android.xmp.util.FileUtils.writeToFile
import org.helllabs.android.xmp.util.InfoCache.fileExists
import org.helllabs.android.xmp.util.Log.e
import org.helllabs.android.xmp.util.Log.i
import org.helllabs.android.xmp.util.Message.error
import java.io.*
import java.util.*

class Playlist(context: Context?, val name: String?) {

    var comment: String? = null
    private var mListChanged = false
    private var mCommentChanged = false
    var isShuffleMode = false
    var isLoopMode = false
    val list: MutableList<PlaylistItem>
    private val mPrefs: SharedPreferences

    private class ListFile : File {
        constructor(name: String?) : super(Preferences.DATA_DIR, name + PLAYLIST_SUFFIX)
        constructor(name: String?, suffix: String) : super(Preferences.DATA_DIR, name + PLAYLIST_SUFFIX + suffix)
    }

    private class CommentFile : File {
        constructor(name: String?) : super(Preferences.DATA_DIR, name + COMMENT_SUFFIX)
        constructor(name: String?, suffix: String) : super(Preferences.DATA_DIR, name + COMMENT_SUFFIX + suffix)
    }

    init {
        list = ArrayList()
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val file: File = ListFile(name)
        if (file.exists()) {
            i(TAG, "Read playlist $name")
            val comment = readFromFile(CommentFile(name))

            // read list contents
            if (readList(name)) {
                this.comment = comment
                isShuffleMode = readShuffleModePref(name)
                isLoopMode = readLoopModePref(name)
            }
        } else {
            i(TAG, "New playlist $name")
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
        i(TAG, "Commit playlist $name")
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
    //	/**
    //	 * Add a new item to the playlist.
    //	 * 
    //	 * @param item The item to be added
    //	 */
    //	public void add(final PlaylistItem item) {
    //		mList.add(item);
    //	}
    //	
    //	/**
    //	 * Add new items to the playlist.
    //	 * 
    //	 * @param items The items to be added
    //	 */
    //	public void add(final PlaylistItem[] items) {
    //		for (final PlaylistItem item : items) {
    //			add(item);
    //		}
    //	}
    /**
     * Remove an item from the playlist.
     *
     * @param index The index of the item to be removed
     */
    fun remove(index: Int) {
        i(TAG, "Remove item #" + index + ": " + list[index].name)
        list.removeAt(index)
        mListChanged = true
    }

    // Helper methods
    private fun readList(name: String?): Boolean {
        list.clear()
        val file: File = ListFile(name)
        var lineNum: Int
        val invalidList: MutableList<Int> = ArrayList()
        try {
            val reader = BufferedReader(FileReader(file), 512)
            lineNum = 0

            reader.forEachLine {
                val fields = it.split(":".toRegex(), 3).toTypedArray()
                val filename = fields[0]
                val comment = if (fields.size > 1) fields[1] else ""
                val title = if (fields.size > 2) fields[2] else ""

                if (fileExists(filename)) {
                    val item = PlaylistItem(PlaylistItem.TYPE_FILE, title, comment)
                    item.file = File(filename)
                    item.imageRes = R.drawable.grabber
                    list.add(item)
                } else {
                    invalidList.add(lineNum)
                }
                lineNum++
            }
            reader.close()
            PlaylistUtils.renumberIds(list)
        } catch (e: IOException) {
            e(TAG, "Error reading playlist " + file.path)
            return false
        }
        if (invalidList.isNotEmpty()) {
            val array = IntArray(invalidList.size)
            val iterator: Iterator<Int> = invalidList.iterator()
            for (i in array.indices) {
                array[i] = iterator.next()
            }
            try {
                removeLineFromFile(file, array)
            } catch (e: FileNotFoundException) {
                e(TAG, "Playlist file " + file.path + " not found")
            } catch (e: IOException) {
                e(TAG, "I/O error removing invalid lines from " + file.path)
            }
        }
        return true
    }

    private fun writeList(name: String?) {
        i(TAG, "Write list")
        val file: File = ListFile(name, ".new")
        file.delete()
        try {
            val out = BufferedWriter(FileWriter(file), 512)
            for (item in list) {
                out.write(item.toString())
            }
            out.close()
            val oldFile: File = ListFile(name)
            oldFile.delete()
            file.renameTo(oldFile)
        } catch (e: IOException) {
            e(TAG, "Error writing playlist file " + file.path)
        }
    }

    private fun writeComment(name: String?) {
        i(TAG, "Write comment")
        val file: File = CommentFile(name, ".new")
        file.delete()
        try {
            writeToFile(file, comment!!)
            val oldFile: File = CommentFile(name)
            oldFile.delete()
            file.renameTo(oldFile)
        } catch (e: IOException) {
            e(TAG, "Error writing comment file " + file.path)
        }
    }

    private fun readShuffleModePref(name: String?): Boolean {
        return mPrefs.getBoolean(optionName(name, SHUFFLE_MODE), DEFAULT_SHUFFLE_MODE)
    }

    private fun readLoopModePref(name: String?): Boolean {
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
         * Rename a playlist.
         *
         * @param context The context we're running in
         * @param oldName The current name of the playlist
         * @param newName The new name of the playlist
         * @return Whether the rename was successful
         */
        fun rename(context: Context?, oldName: String?, newName: String?): Boolean {
            val old1: File = ListFile(oldName)
            val old2: File = CommentFile(oldName)
            val new1: File = ListFile(newName)
            val new2: File = CommentFile(newName)
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
         * @param name    The playlist name
         */
        fun delete(context: Context?, name: String?) {
            ListFile(name).delete()
            CommentFile(name).delete()
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = prefs.edit()
            editor.remove(optionName(name, SHUFFLE_MODE))
            editor.remove(optionName(name, LOOP_MODE))
            editor.apply()
        }

        ///**
        // * Add an item to the specified playlist file.
        // *
        // * @param context The context we're running in
        // * @param name The playlist name
        // * @param item The playlist item to add
        // */
        /*public static void addToList(final Context context, final String name, final PlaylistItem item) {
		try {
			FileUtils.writeToFile(new File(Preferences.DATA_DIR, name + PLAYLIST_SUFFIX), item.toString());
		} catch (IOException e) {
			Message.error(context, context.getString(R.string.error_write_to_playlist));
		}
	    }
	    */

        /**
         * Add a list of items to the specified playlist file.
         *
         * @param activity The activity we're running
         * @param name     The playlist name
         * @param items    The list of playlist items to add
         */
        fun addToList(activity: Activity, name: String?, items: List<PlaylistItem>) {
            val lines = mutableListOf<String>()
            var i = 0
            for (item in items) {
                lines[i++] = item.toString()
            }
            try {
                writeToFile(File(Preferences.DATA_DIR, name + PLAYLIST_SUFFIX), lines)
            } catch (e: IOException) {
                error(activity, activity.getString(R.string.error_write_to_playlist))
            }
        }

        /**
         * Read comment from a playlist file.
         *
         * @param activity The activity we're running
         * @param name     The playlist name
         * @return The playlist comment
         */
        fun readComment(activity: Activity, name: String?): String {
            var comment: String? = null
            try {
                comment = readFromFile(CommentFile(name))
            } catch (e: IOException) {
                error(activity, activity.getString(R.string.error_read_comment))
            }
            if (comment == null || comment.trim { it <= ' ' }.isEmpty()) {
                comment = activity.getString(R.string.no_comment)
            }
            return comment
        }

        private fun optionName(name: String?, option: String): String {
            return OPTIONS_PREFIX + name + option
        }
    }
}