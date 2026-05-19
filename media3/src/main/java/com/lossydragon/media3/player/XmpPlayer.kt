package com.lossydragon.media3.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.lossydragon.media3.model.FrameSnapshot
import com.lossydragon.media3.model.ModuleFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.helllabs.libxmp.Xmp
import timber.log.Timber

@Suppress("ktlint:standard:class-signature")
@OptIn(UnstableApi::class)
class XmpPlayer(
    context: Context,
    private val engine: XmpEngine
) : SimpleBasePlayer(Looper.getMainLooper()) {

    companion object {
        fun ModuleFile.toMediaItem() = MediaItem.Builder()
            .setUri(this.uri)
            .setMediaId(this.uri.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(this.name)
                    .setArtist(this.extension.uppercase())
                    .setIsPlayable(true)
                    .build()
            )
            .build()

        // Great naming!
        fun ModuleFile.toMediaItem2(duration: Long): MediaMetadata {
            val realName = Xmp.getModName().ifBlank { this.name }
            val realType = Xmp.getModType().ifBlank { this.extension }
            return MediaMetadata.Builder()
                .setTitle(realName)
                .setArtist(realType)
                .setDurationMs(duration)
                .setIsPlayable(true)
                .build()
        }
    }

    @Volatile
    private var pendingSeekPositionMs: Long = -1L

    private val playlist = mutableListOf<MediaItem>()
    private val queue = mutableListOf<ModuleFile>()
    private var playWhenReady = false
    private var isLooping = false
    private var currentIndex: Int = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    val frameFlow: StateFlow<FrameSnapshot?> get() = engine.frameFlow
    val isPlaying: StateFlow<Boolean> get() = engine.isPlaying
    val positionMs: StateFlow<Long> get() = engine.positionMs

    val currentIndexFlow: StateFlow<Int>
        field = MutableStateFlow(0)
    val queueFlow: StateFlow<List<ModuleFile>>
        field = MutableStateFlow<List<ModuleFile>>(emptyList())

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionUpdateJob: Job? = null

    init {
        Timber.d("XmpSimplePlayer init")
        scope.launch {
            engine.isPlaying.collect { playing ->
                Timber.d("isPlaying=$playing endedNaturally=${engine.endedNaturally}")
                invalidateState()
                if (playing) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                    if (engine.endedNaturally) {
                        Timber.d("calling advanceToNext")
                        mainHandler.post { advanceToNext() }
                    }
                }
            }
        }
    }

    fun loadQueue(files: List<ModuleFile>, startAt: Int, loop: Boolean = false) {
        isLooping = loop
        queue.clear()
        queue.addAll(files)

        playlist.clear()
        playlist.addAll(files.map { it.toMediaItem() })

        playWhenReady = true
        currentIndex = startAt.coerceIn(0, playlist.lastIndex)
        queueFlow.value = files
        currentIndexFlow.value = currentIndex

        invalidateState()
        loadAndStartAt(currentIndex)
    }

    private fun loadAndStartAt(index: Int) {
        Timber.d("loadAndStartAt index=$index queueSize=${queue.size}")
        val file = queue.getOrNull(index) ?: return

        Thread {
            if (engine.load(file)) {
                val metaData = file.toMediaItem2(engine.durationMs.value)

                val realItem = MediaItem.Builder()
                    .setUri(file.uri)
                    .setMediaId(file.uri.toString())
                    .setMediaMetadata(metaData)
                    .build()

                mainHandler.post {
                    playlist[index] = realItem
                    invalidateState()
                }

                engine.start()
                mainHandler.post { invalidateState() }
            } else {
                // Load failed — skip to next
                mainHandler.post { advanceToNext() }
            }
        }.start()
    }

    private fun advanceToNext() {
        val next = currentIndex + 1
        Timber.d("advanceToNext next=$next queueSize=${queue.size} isLooping=$isLooping")
        when {
            next < queue.size -> {
                currentIndex = next
                currentIndexFlow.value = currentIndex
                pendingSeekPositionMs = -1L
                invalidateState()
                loadAndStartAt(currentIndex)
            }

            isLooping -> {
                currentIndex = 0
                currentIndexFlow.value = currentIndex
                pendingSeekPositionMs = -1L
                invalidateState()
                loadAndStartAt(currentIndex)
            }

            else -> {
                Timber.d("queue exhausted — clearing")
                playlist.clear()
                queue.clear()
                currentIndex = 0
                currentIndexFlow.value = currentIndex
                queueFlow.value = emptyList()
                invalidateState()
            }
        }
    }

    private fun advanceToPrevious() {
        val prev = currentIndex - 1
        if (prev >= 0) {
            currentIndex = prev
            currentIndexFlow.value = currentIndex
            pendingSeekPositionMs = -1L
            invalidateState()
            loadAndStartAt(currentIndex)
        } else if (isLooping) {
            currentIndex = queue.lastIndex
            currentIndexFlow.value = currentIndex
            pendingSeekPositionMs = -1L
            invalidateState()
            loadAndStartAt(currentIndex)
        }
    }

    override fun getState(): State {
        val commands = Player.Commands.Builder().addAll(
            COMMAND_PLAY_PAUSE,
            COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            COMMAND_SEEK_TO_MEDIA_ITEM,
            COMMAND_SEEK_TO_PREVIOUS,
            COMMAND_SEEK_TO_NEXT,
            COMMAND_GET_CURRENT_MEDIA_ITEM,
            COMMAND_GET_METADATA,
            COMMAND_GET_TIMELINE,
            COMMAND_STOP,
        ).build()

        val playlistItems = playlist.mapIndexed { i, item ->
            val uid = item.mediaId.ifEmpty {
                item.localConfiguration?.uri?.toString() ?: i.toString()
            }
            val duration = if (i == currentIndex && engine.durationMs.value > 0) {
                engine.durationMs.value * 1_000L
            } else {
                C.TIME_UNSET
            }
            MediaItemData.Builder(uid)
                .setMediaItem(item)
                .setIsSeekable(true)
                .setDurationUs(duration)
                .build()
        }
        val position = if (pendingSeekPositionMs >= 0) {
            pendingSeekPositionMs
        } else {
            engine.positionMs.value
        }

        return State.Builder()
            .setAvailableCommands(commands)
            .setPlaylist(playlistItems)
            .setCurrentMediaItemIndex(currentIndex)
            .setPlayWhenReady(engine.isPlaying.value, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(if (playlist.isEmpty()) STATE_IDLE else STATE_READY)
            .setContentPositionMs(position)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        this.playWhenReady = playWhenReady
        if (playWhenReady) engine.resume() else engine.pause()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int
    ): ListenableFuture<*> {
        Timber.d(
            "handleSeek mediaItemIndex=$mediaItemIndex positionMs=$positionMs seekCommand=$seekCommand"
        )
        when (seekCommand) {
            COMMAND_SEEK_TO_NEXT,
            COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> advanceToNext()

            COMMAND_SEEK_TO_PREVIOUS,
            COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                // If more than 3s in, restart current track; otherwise go to previous
                if (engine.positionMs.value > 3_000L) {
                    pendingSeekPositionMs = 0L
                    engine.seek(0)
                    invalidateState()
                } else {
                    advanceToPrevious()
                }
            }

            else -> {
                Timber.d("handleSeek else branch — calling engine.seek($positionMs)")
                pendingSeekPositionMs = positionMs
                engine.seek(positionMs.toInt())
                invalidateState()
            }
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        Thread { engine.stop() }.start()
        queue.clear()
        playlist.clear()
        playWhenReady = false
        currentIndex = 0
        currentIndexFlow.value = currentIndex
        queueFlow.value = emptyList()
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    fun releaseEngine() {
        scope.cancel("Releasing Engine")
        engine.release()
    }

    fun next() {
        advanceToNext()
    }

    fun previous() {
        if (engine.positionMs.value > 3_000L) {
            engine.seek(0)
            pendingSeekPositionMs = 0L
            invalidateState()
        } else {
            advanceToPrevious()
        }
    }

    fun jumpToIndex(index: Int) {
        if (index in queue.indices) {
            currentIndex = index
            currentIndexFlow.value = currentIndex
            pendingSeekPositionMs = -1L
            invalidateState()
            loadAndStartAt(currentIndex)
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (true) {
                delay(500L)
                if (pendingSeekPositionMs >= 0) {
                    val diff = kotlin.math.abs(engine.positionMs.value - pendingSeekPositionMs)
                    if (diff < 2_000L) pendingSeekPositionMs = -1L
                }
                invalidateState()
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
}
