package com.lossydragon.media3.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** UI state and domain models for the SAF file browser. */

@Immutable
data class FileItem(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val size: Long
)

@Immutable
data class BrowserUiState(
    val currentPath: String = "",
    val files: ImmutableList<ModuleFile> = persistentListOf(),
    val directories: ImmutableList<FileItem> = persistentListOf(),
    val breadcrumbs: ImmutableList<String> = persistentListOf(),
    val isLoading: Boolean = true,
    val hasStorageAccess: Boolean = false,
    val isShuffle: Boolean = false,
    val isLoop: Boolean = false,
    val error: String? = null
)
