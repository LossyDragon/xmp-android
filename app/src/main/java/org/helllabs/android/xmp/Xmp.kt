@file:Suppress("MemberVisibilityCanBePrivate")

package org.helllabs.android.xmp

import android.content.Context
import android.net.Uri
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.model.ChannelInfo
import org.helllabs.android.xmp.model.FrameInfo
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.model.ModVars
import org.helllabs.android.xmp.model.SequenceVars
import timber.log.Timber

object Xmp {

    const val MIN_BUFFER_MS = 80

    const val MAX_BUFFER_MS = 1000

    const val DUCK_VOLUME = 0x500

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

    // external fun loadModule(name: String?): Int

    // external fun testModule(name: String?, info: ModInfo?): Boolean

    external fun loadModuleFd(fd: Int): Int

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

    external fun setPlayer(parm: Int, `val`: Int)

    external fun startPlayer(rate: Int): Int

    external fun stopAudio(): Boolean

    external fun stopModule(): Int

    external fun testModuleFd(fd: Int, modInfo: ModInfo): Boolean

    external fun time(): Int

    external fun getChannelData(ci: ChannelInfo)

    external fun getComment(): ByteArray

    external fun getFormats(): Array<String>?

    external fun getInstruments(): Array<String>?

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

    external fun getSeqVars(vars: SequenceVars)

    external fun getVersion(): String

    external fun getVolume(): Int

    external fun nextPosition(): Int

    external fun prevPosition(): Int

    external fun restartModule(): Int

    external fun setPosition(num: Int): Int

    external fun setSequence(seq: Int): Boolean

    external fun setVolume(vol: Int): Int

    /**
     * Helper to get formats
     */
    val formats: ImmutableList<String>
        get() = getFormats().orEmpty().sorted().toPersistentList()

    /**
     * Test module from File Descriptor
     */
    fun testFromFd(
        context: Context,
        storageManager: StorageManager,
        uri: Uri,
        modInfo: ModInfo = ModInfo()
    ): Boolean {
        Timber.d("Testing: ${storageManager.getFileName(uri)}")

        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        val res = if (pfd != null) {
            val fd = pfd.detachFd()
            pfd.close()

            testModuleFd(fd, modInfo)
        } else {
            false
        }

        if (res) {
            Timber.i("Test Success: ${modInfo.name} | ${modInfo.type}")
        }

        return res
    }

    /**
     * Load module from File Descriptor
     */
    fun loadFromFd(
        context: Context,
        storageManager: StorageManager,
        uri: Uri
    ): Int {
        Timber.d("Loading: ${storageManager.getFileName(uri)}")

        // Open ONCE for both test and load
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        if (pfd == null) {
            Timber.e("Failed to open file descriptor for $uri")
            return -1
        }

        return try {
            val testFd = pfd.dup()?.detachFd()
            if (testFd == null) {
                Timber.e("Failed to duplicate file descriptor")
                pfd.close()
                return -1
            }

            val modInfo = ModInfo()
            val testResult = testModuleFd(testFd, modInfo)

            if (!testResult) {
                Timber.d("Test failed for $uri")
                pfd.close()
                return -1
            }

            Timber.i("Test Success: ${modInfo.name} | ${modInfo.type}")

            val loadFd = pfd.dup()?.detachFd()
            if (loadFd == null) {
                Timber.e("Failed to duplicate file descriptor for loading")
                pfd.close()
                return -1
            }

            val result = loadModuleFd(loadFd)

            pfd.close()

            Timber.d("Load Module from file descriptor, result: $result")
            result
        } catch (e: Exception) {
            Timber.e(e, "Exception loading module")
            pfd.close()
            -1
        }
    }
}
