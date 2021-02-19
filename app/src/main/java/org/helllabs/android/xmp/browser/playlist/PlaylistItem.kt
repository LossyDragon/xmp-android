package org.helllabs.android.xmp.browser.playlist

import org.helllabs.android.xmp.R
import java.io.File

class PlaylistItem(val type: Int, val name: String?, val comment: String) : Comparable<PlaylistItem> {
    // Accessors
    var id = 0
    var file: File? = null
    var imageRes = 0
    val filename: String
        get() = file!!.name

    init {
        imageRes = when (type) {
            TYPE_DIRECTORY -> R.drawable.folder
            TYPE_PLAYLIST -> R.drawable.list
            TYPE_FILE -> R.drawable.file
            else -> -1
        }
    }

    override fun toString(): String {
        return String.format("%s:%s:%s\n", file!!.path, comment, name)
    }

    // Comparable
    override fun compareTo(other: PlaylistItem): Int {
        val d1 = file!!.isDirectory
        val d2 = other.file!!.isDirectory
        return if (d1 xor d2) {
            if (d1) -1 else 1
        } else {
            name!!.compareTo(other.name!!)
        }
    }

    companion object {
        const val TYPE_DIRECTORY = 1
        const val TYPE_PLAYLIST = 2
        const val TYPE_FILE = 3
        const val TYPE_SPECIAL = 4
    }
}