package org.helllabs.android.xmp.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class FileItem(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean = false,
    val lastModified: Long = 0,
    val size: Long = 0,
    val customComment: String? = null
) {
    // TODO: Separate Playlists and Explorer items
    constructor(name: String, comment: String, uri: Uri) :
        this(
            name = name,
            uri = uri,
            isDirectory = false,
            lastModified = 0,
            size = 0,
            customComment = comment
        )

    val formattedComment: String
        get() = customComment ?: if (isDirectory) {
            ""
        } else {
            val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(Date(lastModified))
            "$date ($size kB)"
        }

    val comment: String
        get() = formattedComment
}
