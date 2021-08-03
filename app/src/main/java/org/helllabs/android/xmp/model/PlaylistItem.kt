package org.helllabs.android.xmp.model

import java.io.File
import java.util.*

enum class PlaylistType(val value: Int) {
    TYPE_DIRECTORY(1),
    TYPE_PLAYLIST(2),
    TYPE_FILE(3),
    TYPE_SPECIAL(4),
}

data class PlaylistItem(
    val type: PlaylistType,
    val name: String,
    val comment: String?,
    var id: Int = 0,
    var file: File? = null,
) : Comparable<PlaylistItem> {

    fun isDirectory() =
        file?.isDirectory ?: false

    override fun toString(): String =
        String.format("%s:%s:%s\n", file!!.path, comment, name)

    // Comparable
    override fun compareTo(other: PlaylistItem): Int {
        val locale = Locale.getDefault()
        val d1 = file!!.isDirectory
        val d2 = other.file!!.isDirectory
        return if (d1 xor d2) {
            if (d1) -1 else 1
        } else {
            name.uppercase(locale).compareTo(other.name.uppercase(locale))
        }
    }
}
