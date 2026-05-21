package com.lossydragon.media3.model

import android.net.Uri
import androidx.compose.runtime.Immutable

/**
 * Represents a tracker module file on device storage.
 * [resolvedName] and [resolvedType] are populated from the Room cache
 * via libxmp's test result — empty until indexed.
 */

@Immutable
data class ModuleFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val extension: String,
    val resolvedName: String = "",
    val resolvedType: String = ""
)
