package com.lossydragon.media3.player

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.model.PlaybackStatus
import com.lossydragon.media3.model.PlayerUiState
import java.nio.charset.StandardCharsets
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.helllabs.libxmp.Xmp

/** Bridges [XmpPlayer] state to the UI via [PlayerUiState]. */
@OptIn(UnstableApi::class)
class XmpPlayerViewModel(
    private val appContext: Context,
    private val player: XmpPlayer
) : ViewModel() {

    val state: StateFlow<PlayerUiState>
        field = MutableStateFlow(PlayerUiState())

    init {
        // Frame updates — position, duration, channel data
        player.frameFlow.onEach { frame ->
            frame ?: return@onEach
            state.update {
                it.copy(
                    positionMs = frame.timeMs.toLong(),
                    durationMs = frame.totalTimeMs.toLong(),
                    frame = frame,
                )
            }
        }.launchIn(viewModelScope)

        player.moduleLoadedFlow.onEach {
            state.update { s ->
                s.copy(
                    sequenceDurations = player.getSequenceDurations().toImmutableList(),
                    currentSequence = 0,
                )
            }
        }.launchIn(viewModelScope)

        player.currentSequenceFlow.onEach { sequence ->
            state.update { it.copy(currentSequence = sequence) }
        }.launchIn(viewModelScope)

        // Playback status
        player.isPlaying.onEach { playing ->
            state.update {
                it.copy(
                    status = when {
                        playing -> PlaybackStatus.PLAYING
                        it.currentModule != null -> PlaybackStatus.PAUSED
                        else -> PlaybackStatus.IDLE
                    }
                )
            }
        }.launchIn(viewModelScope)

        // Queue changes — clear UI state when queue empties
        player.queueFlow.onEach { queue ->
            state.update {
                it.copy(
                    queue = queue.toImmutableList(),
                    currentModule = if (queue.isEmpty()) null else it.currentModule,
                    status = if (queue.isEmpty()) PlaybackStatus.IDLE else it.status,
                    frame = if (queue.isEmpty()) null else it.frame,
                )
            }
        }.launchIn(viewModelScope)

        // Track changes — update current module metadata
        player.currentIndexFlow.onEach { index ->
            val file = player.queueFlow.value.getOrNull(index) ?: return@onEach
            state.update {
                it.copy(
                    currentModule = file,
                    moduleName = file.resolvedName.ifBlank { file.name.ifBlank { "(Untitled)" } },
                    moduleType = file.resolvedType.ifBlank {
                        file.extension.uppercase().ifBlank { "???" }
                    },
                    currentQueueIndex = index,
                    sequenceDurations = player.getSequenceDurations().toImmutableList(),
                    currentSequence = 0,
                )
            }
        }.launchIn(viewModelScope)
    }

    /** Loads [file] as a single-item queue and starts playback. */
    fun play(file: ModuleFile) {
        state.update {
            it.copy(
                status = PlaybackStatus.LOADING,
                currentModule = file,
                moduleName = file.resolvedName.ifBlank { file.name },
                moduleType = file.resolvedType.ifBlank { file.extension.uppercase() },
            )
        }

        appContext.startService(Intent(appContext, XmpService::class.java))
        player.loadQueue(listOf(file), startAt = 0)
    }

    /** Loads [files] as a queue, optionally shuffled, and starts playback at [startAt]. */
    fun playAll(
        files: ImmutableList<ModuleFile>,
        startAt: Int,
        isShuffle: Boolean,
        isLoop: Boolean
    ) {
        val ordered = if (isShuffle) files.shuffled() else files.toList()
        val startIndex = if (isShuffle) 0 else startAt.coerceIn(0, ordered.lastIndex)

        state.update {
            it.copy(
                status = PlaybackStatus.LOADING,
                currentModule = ordered[startIndex],
                moduleName = ordered[startIndex].resolvedName.ifBlank { ordered[startIndex].name },
                moduleType = ordered[startIndex].resolvedType.ifBlank {
                    ordered[startIndex].extension.uppercase()
                },
            )
        }

        appContext.startService(Intent(appContext, XmpService::class.java))
        player.loadQueue(ordered, startIndex)
    }

    fun togglePlayPause() = if (state.value.status == PlaybackStatus.PLAYING) {
        player.pause()
    } else {
        player.play()
    }

    fun seek(posMs: Long) = player.seekTo(player.currentMediaItemIndex, posMs)

    fun next() = player.next()

    fun previous() = player.previous()

    fun stop() = player.stop()

    fun playAtIndex(index: Int) = player.jumpToIndex(index)

    fun toggleShuffle() {
        val newShuffle = !state.value.isShuffle
        player.shuffleModeEnabled = newShuffle
        state.update { it.copy(isShuffle = newShuffle) }
    }

    fun toggleLoop() {
        val newMode = when (state.value.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        player.setRepeatMode(newMode)
        state.update { it.copy(repeatMode = newMode) }
    }

    fun muteChannel(ch: Int, muted: Boolean) = Xmp.mute(ch, if (muted) 1 else 0)

    fun setSequence(index: Int) {
        if (player.setSequence(index)) { // need to expose engine or route through player
            state.update { it.copy(currentSequence = index) }
        }
    }

    fun toggleAllSequences() {
        val new = !state.value.playAllSequences
        player.playAllSequences = new
        state.update { it.copy(playAllSequences = new) }
    }

    fun getModInstruments(): Boolean {
        val songInstruments = Xmp.getInstruments().toPersistentList()
        state.update { it.copy(songInstruments = songInstruments) }
        return songInstruments.isNotEmpty()
    }

    fun closeModInstruments() = state.update { it.copy(songInstruments = persistentListOf()) }

    fun getModComment(): Boolean {
        val songMessageText = String(Xmp.getComment(), StandardCharsets.UTF_8)
        state.update { it.copy(songMessage = songMessageText) }
        return songMessageText.isNotBlank()
    }

    fun closeModComment() = state.update { it.copy(songMessage = "") }
}
