package org.helllabs.android.xmp.model

import android.net.Uri
import androidx.compose.runtime.Stable

@Stable
data class FileItem(
    val name: String,
    val comment: String,
    val uri: Uri,
    val isDirectory: Boolean = false
)
