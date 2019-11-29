package org.helllabs.android.xmp.browser.playlist

import java.io.File

import org.helllabs.android.xmp.R

class PlaylistItem(
        val type: Int,
        val name: String,
        val comment: String
) : Comparable<PlaylistItem> {

    // Accessors
    var id: Int = 0
    var file: File? = null
    var imageRes: Int = 0

    val filename: String
        get() = file!!.name

    init {
        imageRes = when (type) {
            TYPE_DIRECTORY -> R.drawable.ic_folder
            TYPE_PLAYLIST -> R.drawable.ic_list
            TYPE_FILE -> R.drawable.ic_file
            else -> -1
        }
    }

    override fun toString(): String {
        return String.format("%s:%s:%s\n", file!!.path, comment, name)
    }

    // Comparable
    override fun compareTo(other: PlaylistItem): Int {
        val d1 = this.file!!.isDirectory
        val d2 = other.file!!.isDirectory

        return if (d1 xor d2) {
            if (d1) -1 else 1
        } else {
            this.name.compareTo(other.name)
        }
    }

    companion object {
        const val TYPE_DIRECTORY = 1
        const val TYPE_PLAYLIST = 2
        const val TYPE_FILE = 3
        const val TYPE_SPECIAL = 4
    }
}
