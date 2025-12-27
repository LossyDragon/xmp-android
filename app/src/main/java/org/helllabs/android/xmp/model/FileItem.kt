package org.helllabs.android.xmp.model

import androidx.compose.runtime.Stable
import com.lazygeniouz.dfc.file.DocumentFileCompat

@Stable
data class FileItem(
    val name: String,
    val comment: String,
    val docFile: DocumentFileCompat?,
    val isSpecial: Boolean = false
) : Comparable<FileItem> {

    val isFile: Boolean
        get() = docFile?.isFile() ?: false

    override fun compareTo(other: FileItem): Int {
        if (isSpecial != other.isSpecial) {
            return if (isSpecial) -1 else 1
        }

        val isDirectory = docFile?.isDirectory() ?: false
        val otherIsDirectory = other.docFile?.isDirectory() ?: false
        if (isDirectory != otherIsDirectory) {
            return if (isDirectory) -1 else 1
        }

        return name.compareTo(other.name, ignoreCase = true)
    }
}
