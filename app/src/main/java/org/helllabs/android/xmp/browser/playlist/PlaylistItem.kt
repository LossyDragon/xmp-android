package org.helllabs.android.xmp.browser.playlist

import java.io.File

import org.helllabs.android.xmp.R

class PlaylistItem(val type: Int, val name: String, val comment: String) : Comparable<PlaylistItem> {

    // Accessors

    var id: Int = 0
    var file: File? = null
    var imageRes: Int = 0

    val filename: String
        get() = file!!.name

    init {

        when (type) {
            TYPE_DIRECTORY -> imageRes = R.drawable.folder
            TYPE_PLAYLIST -> imageRes = R.drawable.list
            TYPE_FILE -> imageRes = R.drawable.file
            else -> imageRes = -1
        }
    }

    override fun toString(): String {
        return String.format("%s:%s:%s\n", file!!.path, comment, name)
    }

    // Comparable

    override fun compareTo(info: PlaylistItem): Int {
        val d1 = this.file!!.isDirectory
        val d2 = info.file!!.isDirectory

        return if (d1 xor d2) {
            if (d1) -1 else 1
        } else {
            this.name.compareTo(info.name)
        }
    }

    companion object {
        val TYPE_DIRECTORY = 1
        val TYPE_PLAYLIST = 2
        val TYPE_FILE = 3
        val TYPE_SPECIAL = 4
    }
}
