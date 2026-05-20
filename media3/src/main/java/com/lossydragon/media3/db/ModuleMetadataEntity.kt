package com.lossydragon.media3.db

import androidx.room.Entity

@Entity(
    tableName = "module_metadata",
    primaryKeys = ["fileName", "sizeBytes"],
)
data class ModuleMetadataEntity(
    val fileName: String,
    val sizeBytes: Long,
    val headerHash: String,
    val name: String,
    val type: String,
    val extension: String,
    val lastSeen: Long = System.currentTimeMillis()
)
