package com.lossydragon.media3.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class PlaybackStatus { IDLE, LOADING, PLAYING, PAUSED, ERROR }

@Immutable
data class ModuleFile(val uri: Uri, val name: String, val sizeBytes: Long, val extension: String)

@Immutable
data class ChannelSnapshot(
    val volume: Int,
    val finalVol: Int,
    val pan: Int,
    val instrument: Int,
    val note: Int,
    val period: Int
)

@Immutable
data class FrameSnapshot(
    val position: Int,
    val pattern: Int,
    val row: Int,
    val numRows: Int,
    val speed: Int,
    val bpm: Int,
    val timeMs: Int,
    val totalTimeMs: Int,
    val channels: ImmutableList<ChannelSnapshot>,
    val presentationNanos: Long
)

@Immutable
data class PlayerUiState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentModule: ModuleFile? = null,
    val moduleName: String = "",
    val moduleType: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val frame: FrameSnapshot? = null,
    val errorMessage: String? = null,
    val queue: ImmutableList<ModuleFile> = persistentListOf(),
    val currentQueueIndex: Int = 0
)
