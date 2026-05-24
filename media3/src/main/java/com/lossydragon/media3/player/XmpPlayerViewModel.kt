package com.lossydragon.media3.player

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.lossydragon.media3.db.XmpPreferences
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
import timber.log.Timber

/** Bridges [XmpPlayer] state to the UI via [PlayerUiState]. */
@OptIn(UnstableApi::class)
class XmpPlayerViewModel(
    private val appContext: Context,
    private val player: XmpPlayer,
    private val prefs: XmpPreferences
) : ViewModel() {

    val state: StateFlow<PlayerUiState>
        field = MutableStateFlow(PlayerUiState())

    init {
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

        player.moduleLoadedFlow
            .onEach { syncModuleInfo() }
            .launchIn(viewModelScope)

        player.currentSequenceFlow
            .onEach { state.update { s -> s.copy(currentSequence = it) } }
            .launchIn(viewModelScope)

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

        player.queueFlow.onEach { queue ->
            Timber.d("queueFlow fired, size=${queue.size}")
            state.update {
                it.copy(
                    queue = queue.toImmutableList(),
                    currentModule = if (queue.isEmpty()) null else it.currentModule,
                    status = if (queue.isEmpty()) PlaybackStatus.IDLE else it.status,
                    frame = if (queue.isEmpty()) null else it.frame,
                )
            }
        }.launchIn(viewModelScope)

        player.currentIndexFlow.onEach { index ->
            val file = player.queueFlow.value.getOrNull(index) ?: return@onEach
            state.update {
                it.copy(
                    currentModule = file,
                    currentQueueIndex = index,
                    moduleName = if (player.isReordering) it.moduleName else file.displayName(),
                    moduleType = if (player.isReordering) it.moduleType else file.displayType(),
                    currentSequence = if (player.isReordering) it.currentSequence else 0,
                )
            }
        }.launchIn(viewModelScope)

        player.moduleLoadedFlow.onEach {
            Timber.d("moduleLoadedFlow fired")
            syncModuleInfo()
        }.launchIn(viewModelScope)
    }

    private fun ModuleFile.displayName() =
        resolvedName.ifBlank { name.ifBlank { "(Untitled)" } }

    private fun ModuleFile.displayType() =
        resolvedType.ifBlank { extension.uppercase().ifBlank { "???" } }

    private fun ensureServiceRunning() {
        val intent = Intent(appContext, XmpService::class.java)
        appContext.startService(intent)
    }

    private fun syncModuleInfo() {
        state.update {
            it.copy(
                sequenceDurations = player.sequenceDurations.toImmutableList(),
                currentSequence = 0,
                numPatterns = player.numPatterns,
                numChannels = player.numChannels,
                numInstruments = player.numInstruments,
                numSamples = player.numSamples,
                numSequences = player.numSequences,
            )
        }
    }

    /** Returns a formatted string of current Oboe audio stream statistics. */
    fun getAudioStats(): String = Xmp.getAudioStats().let { stats ->
        """
        Audio Glitches: ${stats.xrunCount} (system), ${stats.underrunCount} (app)
        Sample Rate: ${stats.sampleRate} Hz
        Buffer: ${stats.bufferSize} / ${stats.bufferCapacity} frames
        Frames Per Burst: ${stats.framesPerBurst}
        Audio API: ${stats.audioApi}
        Sharing Mode: ${stats.sharingMode}
        """.trimIndent()
    }

    /** Loads [file] as a single-item queue and starts playback. */
    fun play(file: ModuleFile) {
        state.update {
            it.copy(
                status = PlaybackStatus.LOADING,
                currentModule = file,
                moduleName = file.displayName(),
                moduleType = file.displayType(),
            )
        }

        ensureServiceRunning()
        player.loadQueue(listOf(file), startAt = 0)
    }

    /** Loads [files] as a queue, optionally shuffled, and starts playback at [startAt]. */
    fun playAll(
        files: ImmutableList<ModuleFile>,
        startAt: Int,
        isShuffle: Boolean,
        repeatMode: Int = state.value.repeatMode
    ) {
        val startIndex = startAt.coerceIn(0, files.lastIndex)

        state.update {
            it.copy(
                status = PlaybackStatus.LOADING,
                currentModule = files[startIndex],
                moduleName = files[startIndex].displayName(),
                moduleType = files[startIndex].displayType(),
                isShuffle = isShuffle,
                repeatMode = repeatMode,
            )
        }

        ensureServiceRunning()
        player.loadQueue(
            files = files.toList(),
            startAt = startIndex,
            shuffle = isShuffle,
            repeatMode = repeatMode,
        )
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

    // TODO implement muting
    fun muteChannel(ch: Int, muted: Boolean) = Xmp.mute(ch, if (muted) 1 else 0)

    fun setSequence(index: Int) = player.setSequence(index)

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

    suspend fun getLastDirectoryUri(): String? = prefs.getLastDirectoryUri()
}
