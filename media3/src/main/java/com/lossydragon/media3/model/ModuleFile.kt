package com.lossydragon.media3.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class ModuleFile(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val extension: String,
    val resolvedName: String = "",
    val resolvedType: String = ""
)
