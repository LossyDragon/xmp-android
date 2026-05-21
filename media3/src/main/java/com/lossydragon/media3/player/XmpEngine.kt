package com.lossydragon.media3.player

import android.content.Context
import com.lossydragon.media3.model.ChannelSnapshot
import com.lossydragon.media3.model.FrameSnapshot
import com.lossydragon.media3.model.ModuleFile
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.helllabs.libxmp.Xmp
import org.helllabs.libxmp.model.ChannelInfo
import org.helllabs.libxmp.model.FrameInfo
import org.helllabs.libxmp.model.ModInfo
import org.helllabs.libxmp.model.ModVars
import timber.log.Timber

class XmpEngine(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 44100
        private const val BUFFER_MS = 200
        private const val CHANNELS = Xmp.MAX_CHANNELS
    }

    val frameFlow: StateFlow<FrameSnapshot?>
        field = MutableStateFlow<FrameSnapshot?>(null)

    val isPlaying: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val positionMs: StateFlow<Long>
        field = MutableStateFlow(0L)

    val durationMs: StateFlow<Long>
        field = MutableStateFlow(0L)

    @Volatile
    private var paused = false

    @Volatile
    var stopRequest = false
        private set

    @Volatile
    private var initialized = false

    @Volatile
    var endedNaturally: Boolean = false
        private set

    private var renderThread: Thread? = null

    private val frameInfo = FrameInfo()
    private val modVars = ModVars()
    private val channelInfo = ChannelInfo()

    // Maybe stop GC
    private val channelSnapshots = Array(CHANNELS) {
        ChannelSnapshot(0, 0, 0, 0, 0, 0)
    }

    /** Loads [file] into the engine. Returns false on failure. */
    fun load(file: ModuleFile): Boolean {
        endedNaturally = false
        stopRequest = false

        if (initialized) {
            stop()
        }

        if (!Xmp.init(SAMPLE_RATE, BUFFER_MS)) {
            Timber.e("Xmp.init() failed")
            return false
        }

        initialized = true

        val modInfo = ModInfo()
        val result = Xmp.loadFromFd(context, file.uri, modInfo)
        if (result != 0) {
            Timber.e("Xmp.loadFromFd() returned $result")
            Xmp.deinit()
            initialized = false
            return false
        }

        Xmp.getModVars(modVars)

        durationMs.value = modVars.seqDuration.toLong()
        positionMs.value = 0L

        return true
    }

    /** Starts the render thread and audio stream. */
    fun start() {
        Timber.d("start() called")
        if (!initialized) return
        if (renderThread?.isAlive == true) return

        stopRequest = false
        paused = false

        if (Xmp.startPlayer(SAMPLE_RATE) != 0) {
            Timber.e("Xmp.startPlayer() failed")
            return
        }

        // Unmute all channels
        for (i in 0 until Xmp.MAX_CHANNELS) {
            Xmp.mute(i, 0)
        }

        Xmp.setPlayer(Xmp.PLAYER_AMP, 2)
        Xmp.setPlayer(Xmp.PLAYER_INTERP, Xmp.INTERP_LINEAR)
        Xmp.setPlayer(Xmp.PLAYER_DSP, Xmp.DSP_LOWPASS)
        Xmp.setPlayer(Xmp.PLAYER_MIX, 70)
        Xmp.setPlayer(Xmp.PLAYER_VOLUME, 100)

        var prefillCount = 0
        while (Xmp.hasFreeBuffer() && prefillCount++ < 100) {
            if (Xmp.fillBuffer(false) < 0) break
        }

        Xmp.playAudio()
        isPlaying.value = true

        renderThread = Thread(::renderLoop, "XmpRenderThread").also {
            it.priority = Thread.MAX_PRIORITY
            it.start()
        }
    }

    /** Pauses playback — audio stream stopped, position preserved. */
    fun pause() {
        endedNaturally = false
        paused = true
        Xmp.stopAudio()
        isPlaying.value = false
    }

    /** Resumes from paused state. No-op if not paused. */
    fun resume() {
        if (!paused) return

        paused = false
        Xmp.dropAudio()
        Xmp.playAudio()
        isPlaying.value = true
    }

    /** Seeks to [posMs] milliseconds. */
    fun seek(posMs: Int) {
        Timber.d("engine.seek posMs=$posMs")
        Xmp.seek(posMs)
        positionMs.value = posMs.toLong()
    }

    /** Stops playback and releases all native resources. */
    fun stop() {
        endedNaturally = false
        stopRequest = true
        renderThread?.interrupt()
        renderThread?.join(2_000)
        renderThread = null

        if (initialized) {
            Xmp.endPlayer()
            Xmp.releaseModule()
            Xmp.deinit()
            initialized = false
        }

        isPlaying.value = false
        positionMs.value = 0L
        frameFlow.value = null
    }

    private fun renderLoop() {
        while (!stopRequest) {
            try {
                // Fast-path: skip JNI calls entirely while paused
                if (paused) {
                    Thread.sleep(50)
                    continue
                }

                while (!Xmp.hasFreeBuffer() && !paused && !stopRequest) {
                    Thread.sleep(40)
                }

                if (stopRequest) break

                val endReached = Xmp.fillBuffer(false) < 0

                Xmp.getInfo(frameInfo)
                Xmp.getModVars(modVars)
                Xmp.getChannelData(channelInfo)

                val timeMs = Xmp.time()
                val numCh = modVars.numChannels.coerceIn(0, CHANNELS)

                for (i in 0 until numCh) {
                    channelSnapshots[i].apply {
                        volume = channelInfo.volumes[i]
                        finalVol = channelInfo.finalVols[i]
                        pan = channelInfo.pans[i]
                        instrument = channelInfo.instruments[i]
                        note = channelInfo.keys[i]
                        period = channelInfo.periods[i]
                    }
                }

                frameFlow.value = FrameSnapshot(
                    position = frameInfo.pos,
                    pattern = frameInfo.pattern,
                    row = frameInfo.row,
                    numRows = frameInfo.numRows,
                    speed = frameInfo.speed,
                    bpm = frameInfo.bpm,
                    timeMs = timeMs,
                    totalTimeMs = modVars.seqDuration,
                    channels = channelSnapshots.take(numCh).toImmutableList(),
                    presentationNanos = System.nanoTime(),
                )

                positionMs.value = timeMs.toLong()

                if (endReached) {
                    endedNaturally = true
                    isPlaying.value = false
                    Xmp.stopAudio()
                    break
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
    }
}
