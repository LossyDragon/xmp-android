package org.helllabs.android.xmp.ui.playlist_selected

import java.io.*
import java.util.*
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.model.PlaylistType
import org.helllabs.android.xmp.ui.MainActivity
import org.helllabs.android.xmp.util.Files.readFromFile
import org.helllabs.android.xmp.util.Files.removeLineFromFile
import org.helllabs.android.xmp.util.Files.writeToFile
import org.helllabs.android.xmp.util.PlaylistUtils
import org.helllabs.android.xmp.util.PlaylistUtils.COMMENT_SUFFIX
import org.helllabs.android.xmp.util.PlaylistUtils.DEFAULT_LOOP_MODE
import org.helllabs.android.xmp.util.PlaylistUtils.DEFAULT_SHUFFLE_MODE
import org.helllabs.android.xmp.util.PlaylistUtils.LOOP_MODE
import org.helllabs.android.xmp.util.PlaylistUtils.PLAYLIST_SUFFIX
import org.helllabs.android.xmp.util.PlaylistUtils.SHUFFLE_MODE
import org.helllabs.android.xmp.util.PrefManager
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.logE
import org.helllabs.android.xmp.util.logI

class Playlist(val name: String) {

    private var mListChanged = false
    private var mCommentChanged = false

    var comment: String = ""
    var isShuffleMode = false
    var isLoopMode = false
    val list = mutableListOf<PlaylistItem>()

    class ListFile : File {
        constructor(name: String) : super(MainActivity.DATA_DIR, name + PLAYLIST_SUFFIX)
        constructor(name: String, suffix: String) :
            super(MainActivity.DATA_DIR, name + PLAYLIST_SUFFIX + suffix)
    }

    class CommentFile : File {
        constructor(name: String) : super(MainActivity.DATA_DIR, name + COMMENT_SUFFIX)
        constructor(name: String, suffix: String) :
            super(MainActivity.DATA_DIR, name + COMMENT_SUFFIX + suffix)
    }

    init {
        val file: File = ListFile(name)
        if (file.exists()) {
            logI("Read playlist $name")
            val comment = readFromFile(CommentFile(name))

            // read list contents
            if (readList(name)) {
                this.comment = comment
                isShuffleMode = readShuffleModePref(name)
                isLoopMode = readLoopModePref(name)
            }
        } else {
            logI("New playlist $name")
            isShuffleMode = DEFAULT_SHUFFLE_MODE
            isLoopMode = DEFAULT_LOOP_MODE
            mListChanged = true
            mCommentChanged = true
        }
    }

    // Helper methods
    private fun readList(name: String): Boolean {
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

                if (File(filename).isFile) {
                    val item = PlaylistItem(PlaylistType.TYPE_FILE, title, comment)
                    item.file = File(filename)
                    list.add(item)
                } else {
                    invalidList.add(lineNum)
                }
                lineNum++
            }
            reader.close()
            PlaylistUtils.renumberIds(list)
        } catch (e: IOException) {
            logE("Error reading playlist " + file.path)
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
                logE("Playlist file " + file.path + " not found")
            } catch (e: IOException) {
                logE("I/O error removing invalid lines from " + file.path)
            }
        }
        return true
    }

    private fun writeList(name: String) {
        logI("Write list")
        val file: File = ListFile(name, ".new")
        file.delete()
        try {
            file.bufferedWriter().use { out ->
                list.forEach {
                    out.write(it.toString())
                }
                out.close()
            }
            val oldFile: File = ListFile(name)
            oldFile.delete()
            file.renameTo(oldFile)
        } catch (e: IOException) {
            logE("Error writing playlist file " + file.path)
        }
    }

    private fun writeComment(name: String) {
        logI("Write comment")
        val file: File = CommentFile(name, ".new")
        file.delete()
        try {
            writeToFile(file, comment)
            val oldFile: File = CommentFile(name)
            oldFile.delete()
            file.renameTo(oldFile)
        } catch (e: IOException) {
            logE("Error writing comment file " + file.path)
        }
    }

    private fun readShuffleModePref(name: String): Boolean {
        val prefName = PlaylistUtils.optionName(name, SHUFFLE_MODE)
        return PrefManager.getBooleanPref(prefName, DEFAULT_SHUFFLE_MODE)
    }

    private fun readLoopModePref(name: String): Boolean {
        val prefName = PlaylistUtils.optionName(name, LOOP_MODE)
        return PrefManager.getBooleanPref(prefName, DEFAULT_LOOP_MODE)
    }

    fun updateList(newList: List<PlaylistItem>) {
        list.clear()
        newList.forEach {
            logD("new: $it")
            list.add(it)
        }
        PlaylistUtils.renumberIds(list)
    }

    /**
     * Save the current playlist.
     */
    fun commit() {
        logI("Commit playlist: $name")
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
            PrefManager.setBooleanPref(PlaylistUtils.optionName(name, SHUFFLE_MODE), isShuffleMode)
            PrefManager.setBooleanPref(PlaylistUtils.optionName(name, LOOP_MODE), isLoopMode)
        }
    }

    /**
     * Remove an item from the playlist.
     *
     * @param index The index of the item to be removed
     */
    fun remove(index: Int) {
        logI("Remove item #" + index + ": " + list[index].name)
        list.removeAt(index)
        mListChanged = true
    }

    fun setListChanged(listChanged: Boolean) {
        mListChanged = listChanged
    }
}
