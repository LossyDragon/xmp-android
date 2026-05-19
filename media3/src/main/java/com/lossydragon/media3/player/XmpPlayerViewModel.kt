package com.lossydragon.media3.player

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.lossydragon.media3.model.ModuleFile
import com.lossydragon.media3.model.PlaybackStatus
import com.lossydragon.media3.model.PlayerUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.helllabs.libxmp.Xmp
import timber.log.Timber

@Suppress("ktlint:standard:class-signature")
@OptIn(UnstableApi::class)
class XmpPlayerViewModel(
    private val appContext: Context,
    private val player: XmpPlayer
) : ViewModel() {

    val state: StateFlow<PlayerUiState>
        field = MutableStateFlow(PlayerUiState())

    init {
        player.frameFlow.onEach { frame ->
            if (frame == null) {
                return@onEach
            }

            state.update { state ->
                state.copy(
                    positionMs = frame.timeMs.toLong(),
                    durationMs = frame.totalTimeMs.toLong(),
                    frame = frame,
                )
            }
        }.launchIn(viewModelScope)

        player.isPlaying.onEach { playing ->
            state.update { state ->
                val status = if (playing) {
                    PlaybackStatus.PLAYING
                } else if (state.currentModule != null) {
                    PlaybackStatus.PAUSED
                } else {
                    PlaybackStatus.IDLE
                }
                state.copy(status = status)
            }
        }.launchIn(viewModelScope)

        player.queueFlow.onEach { queue ->
            Timber.d("queueFlow emitted size=${queue.size}")
            state.update {
                it.copy(
                    queue = queue.toImmutableList(),
                    currentModule = if (queue.isEmpty()) null else it.currentModule,
                    status = if (queue.isEmpty()) PlaybackStatus.IDLE else it.status,
                    frame = if (queue.isEmpty()) null else it.frame,
                )
            }
            Timber.d("after queueFlow update currentModule=${state.value.currentModule}")
        }.launchIn(viewModelScope)

        player.currentIndexFlow.onEach { index ->
            val file = player.queueFlow.value.getOrNull(index)
            if (file != null) {
                state.update {
                    it.copy(
                        currentModule = file,
                        moduleName = file.name,
                        moduleType = file.extension.uppercase(),
                        currentQueueIndex = index,
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun play(file: ModuleFile) {
        state.update {
            it.copy(
                status = PlaybackStatus.LOADING,
                currentModule = file,
                moduleName = file.name,
            )
        }

        appContext.startService(Intent(appContext, XmpService::class.java))

        player.loadQueue(listOf(file), startAt = 0, loop = false)
    }

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
                moduleName = ordered[startIndex].name,
            )
        }

        appContext.startService(Intent(appContext, XmpService::class.java))
        player.loadQueue(ordered, startIndex, isLoop)
    }

    fun pause() = player.pause()

    fun resume() = player.play()

    fun seek(posMs: Long) {
        player.seekTo(player.currentMediaItemIndex, posMs)
    }

    fun next() = player.next()

    fun previous() = player.previous()

    fun stop() = player.stop()

    fun playAtIndex(index: Int) = player.jumpToIndex(index)

    fun togglePlayPause() = if (state.value.status == PlaybackStatus.PLAYING) {
        pause()
    } else {
        resume()
    }

    fun toggleShuffle() = state.update { it.copy(isShuffle = !it.isShuffle) }

    fun toggleLoop() = state.update { it.copy(isLoop = !it.isLoop) }

    fun muteChannel(ch: Int, muted: Boolean) = Xmp.mute(ch, if (muted) 1 else 0)

    // override fun onCleared() {
    //     super.onCleared()
    //     // player.releaseEngine()
    // }
}
