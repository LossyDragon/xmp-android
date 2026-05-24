package org.helllabs.libxmp

import android.content.Context
import android.net.Uri
import android.util.Log
import org.helllabs.libxmp.model.AudioStats
import org.helllabs.libxmp.model.ChannelInfo
import org.helllabs.libxmp.model.FrameInfo
import org.helllabs.libxmp.model.ModInfo
import org.helllabs.libxmp.model.ModVars

object Xmp {

    private const val TAG = "XMP Library"

    const val MIN_BUFFER_MS = 80

    const val MAX_BUFFER_MS = 1000

    const val DUCK_VOLUME = 700 // 30% (0f..1f)

    // Return codes
    const val XMP_END = 1 // End of module reached

    // Sample format flags
    const val FORMAT_MONO = 1 shl 2

    // player parameters
    const val PLAYER_AMP = 0 // Amplification factor
    const val PLAYER_MIX = 1 // Stereo mixing
    const val PLAYER_INTERP = 2 // Interpolation type
    const val PLAYER_DSP = 3 // DSP effect flags
    const val PLAYER_CFLAGS = 5 // Current module flags
    const val PLAYER_VOLUME = 7 // Player volume (for audio focus duck)
    const val PLAYER_DEFPAN = 10 // Default pan separation

    // Interpolation types
    const val INTERP_NEAREST = 0 // Nearest neighbor
    const val INTERP_LINEAR = 1 // Linear (default)
    const val INTERP_SPLINE = 2 // Cubic spline

    // Player flags
    const val FLAGS_A500 = 1 shl 3

    // DSP effect types
    const val DSP_LOWPASS = 1 shl 0 // Lowpass filter effect

    // Limits
    const val MAX_CHANNELS = 64 // Max number of channels in module

    const val MAX_BUFFERS = 256

    // MAX_SEQUENCES from common.h
    val maxSeqFromHeader: Int
        get() = getMaxSequences()

    init {
        System.loadLibrary("xmp-jni")
    }

    external fun loadModuleFd(fd: Int, modInfo: ModInfo): Int

    external fun deinit(): Int

    external fun dropAudio()

    external fun endPlayer(): Int

    external fun fillBuffer(loop: Boolean): Int

    external fun getInfo(values: FrameInfo)

    external fun getPlayer(parm: Int): Int

    external fun hasFreeBuffer(): Boolean

    external fun init(rate: Int, ms: Int): Boolean

    external fun mute(chn: Int, status: Int): Int

    external fun playAudio(): Int

    external fun releaseModule(): Int

    external fun restartAudio(): Boolean

    external fun seek(time: Int): Int

    external fun setPlayer(parm: Int, value: Int)

    external fun startPlayer(rate: Int): Int

    external fun stopAudio(): Boolean

    external fun stopModule(): Int

    external fun testModuleFd(fd: Int, modInfo: ModInfo): Boolean

    external fun time(): Int

    external fun getChannelData(ci: ChannelInfo)

    external fun getComment(): ByteArray

    external fun getFormats(): Array<String>

    external fun getInstruments(): Array<String>

    external fun getLoopCount(): Int

    private external fun getMaxSequences(): Int

    external fun getModName(): String

    external fun getModType(): String

    external fun getModVars(vars: ModVars)

    external fun getPatternRow(
        pat: Int,
        row: Int,
        rowNotes: ByteArray,
        rowInstruments: ByteArray,
        rowFxType: ByteArray,
        rowFxParm: ByteArray
    )

    external fun getSampleData(
        trigger: Boolean,
        ins: Int,
        key: Int,
        period: Int,
        chn: Int,
        width: Int,
        buffer: ByteArray?
    )

    external fun getSeqVars(): IntArray

    external fun getVersion(): String

    external fun getVolume(): Int

    external fun nextPosition(): Int

    external fun prevPosition(): Int

    external fun restartModule(): Int

    external fun setPosition(num: Int): Int

    external fun setSequence(seq: Int): Boolean

    external fun setVolume(vol: Int): Int

    external fun getAudioStats(): AudioStats

    external fun setExpectSilence(value: Boolean)

    /**
     * Helper to get formats
     */
    val formatsSorted: List<String>
        get() = getFormats().sorted()

    fun testFromFd(context: Context, uri: Uri, modInfo: ModInfo = ModInfo()): Boolean {
        Log.d(TAG, "Testing: $uri")
        return context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            testModuleFd(pfd.detachFd(), modInfo).also { success ->
                if (success) Log.i(TAG, "Test Success: ${modInfo.name} | ${modInfo.type}")
            }
        } ?: false
    }

    fun loadFromFd(context: Context, uri: Uri, modInfo: ModInfo = ModInfo()): Int {
        Log.d(TAG, "Loading: $uri")
        return context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            loadModuleFd(pfd.detachFd(), modInfo).also { result ->
                when (result) {
                    0 -> Log.i(TAG, "Loaded: ${modInfo.name} | ${modInfo.type}")
                    -2 -> Log.w(TAG, "Test failed for $uri")
                    else -> Log.e(TAG, "Load failed: $result for $uri")
                }
            }
        } ?: -1
    }
}
