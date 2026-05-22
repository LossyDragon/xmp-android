package com.lossydragon.media3.model

import androidx.compose.runtime.*
import androidx.media3.common.Player
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** UI state and domain models for the module player. */

enum class PlaybackStatus { IDLE, LOADING, PLAYING, PAUSED, ERROR }

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
    val currentQueueIndex: Int = 0,
    val isShuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val songMessage: String = "",
    val songInstruments: ImmutableList<String> = persistentListOf(),
    val sequenceDurations: ImmutableList<Int> = persistentListOf(),
    val currentSequence: Int = 0,
    val playAllSequences: Boolean = false,
    val numPatterns: Int = 0,
    val numChannels: Int = 0,
    val numInstruments: Int = 0,
    val numSamples: Int = 0,
    val numSequences: Int = 0
)
