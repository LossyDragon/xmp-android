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

class XmpRenderEngine(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 44100
        private const val BUFFER_MS = 200
        private const val CHANNELS = Xmp.MAX_CHANNELS
    }

    private val _frameFlow = MutableStateFlow<FrameSnapshot?>(null)
    val frameFlow: StateFlow<FrameSnapshot?> = _frameFlow.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

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

        _durationMs.value = modVars.seqDuration.toLong()
        _positionMs.value = 0L

        return true
    }

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

        while (Xmp.hasFreeBuffer()) {
            if (Xmp.fillBuffer(false) == Xmp.XMP_END) break
        }

        Xmp.playAudio()
        _isPlaying.value = true

        renderThread = Thread(::renderLoop, "XmpRenderThread").also {
            it.priority = Thread.MAX_PRIORITY
            it.start()
        }
    }

    fun pause() {
        endedNaturally = false
        paused = true
        Xmp.stopAudio()
        _isPlaying.value = false
    }

    fun resume() {
        if (!paused) return
        paused = false
        Xmp.dropAudio()
        Xmp.playAudio()
        _isPlaying.value = true
    }

    fun seek(posMs: Int) {
        Timber.d("engine.seek posMs=$posMs")
        Xmp.seek(posMs)
        _positionMs.value = posMs.toLong()
    }

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

        _isPlaying.value = false
        _positionMs.value = 0L
        _frameFlow.value = null
    }

    fun release() = stop()

    private fun renderLoop() {
        while (!stopRequest) {
            try {
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
                val timeMs = Xmp.time()
                Xmp.getModVars(modVars)
                Xmp.getChannelData(channelInfo)

                val numCh = modVars.numChannels.coerceIn(0, CHANNELS)

                for (i in 0 until numCh) {
                    channelSnapshots[i] = ChannelSnapshot(
                        volume = channelInfo.volumes[i],
                        finalVol = channelInfo.finalVols[i],
                        pan = channelInfo.pans[i],
                        instrument = channelInfo.instruments[i],
                        note = channelInfo.keys[i],
                        period = channelInfo.periods[i],
                    )
                }

                _frameFlow.value = FrameSnapshot(
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

                _positionMs.value = timeMs.toLong()

                if (endReached) {
                    endedNaturally = true
                    _isPlaying.value = false
                    Xmp.stopAudio()
                    Timber.d("renderLoop endReached — setting endedNaturally=$endedNaturally")
                    break
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
    }
}
