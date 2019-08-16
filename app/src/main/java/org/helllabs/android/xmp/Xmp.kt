package org.helllabs.android.xmp

import org.helllabs.android.xmp.util.ModInfo


object Xmp {

    // Return codes
    val XMP_END = 1            // End of module reached

    // Sample format flags
    val FORMAT_MONO = 1 shl 2

    // player parameters
    val PLAYER_AMP = 0            // Amplification factor
    val PLAYER_MIX = 1            // Stereo mixing
    val PLAYER_INTERP = 2        // Interpolation type
    val PLAYER_DSP = 3            // DSP effect flags
    val PLAYER_CFLAGS = 5      // Current module flags
    val PLAYER_VOLUME = 7        // Player volume (for audio focus duck)
    val PLAYER_DEFPAN = 10        // Default pan separation

    // Interpolation types
    val INTERP_NEAREST = 0        // Nearest neighbor
    val INTERP_LINEAR = 1        // Linear (default)
    val INTERP_SPLINE = 2        // Cubic spline

    // Player flags
    val FLAGS_A500 = 1 shl 3

    // DSP effect types
    val DSP_LOWPASS = 1 shl 0    // Lowpass filter effect

    // Limits
    val MAX_CHANNELS = 64        // Max number of channels in module
    val loopCount: Int
        external get
    val version: String
        external get
    val modName: String
        external get
    val modType: String
        external get
    val comment: String
        external get
    val formats: Array<String>
        external get
    val instruments: Array<String>
        external get
    val volume: Int
        external get

    external fun init(rate: Int, ms: Int): Boolean
    external fun deinit(): Int
    external fun testModule(name: String, info: ModInfo): Boolean
    external fun loadModule(name: String): Int
    external fun releaseModule(): Int
    external fun startPlayer(rate: Int): Int
    external fun endPlayer(): Int
    external fun playAudio(): Int
    external fun dropAudio()
    external fun stopAudio(): Boolean
    external fun restartAudio(): Boolean
    external fun hasFreeBuffer(): Boolean
    external fun fillBuffer(loop: Boolean): Int
    external fun nextPosition(): Int
    external fun prevPosition(): Int
    external fun setPosition(num: Int): Int
    external fun stopModule(): Int
    external fun restartModule(): Int
    external fun seek(time: Int): Int
    external fun time(): Int
    external fun mute(chn: Int, status: Int): Int
    external fun getInfo(values: IntArray)
    external fun getPlayer(parm: Int): Int
    external fun setPlayer(parm: Int, `val`: Int)
    external fun getModVars(vars: IntArray)
    external fun getChannelData(volumes: IntArray, finalvols: IntArray, pans: IntArray, instruments: IntArray, keys: IntArray, periods: IntArray)
    external fun getPatternRow(pat: Int, row: Int, rowNotes: ByteArray, rowInstruments: ByteArray)
    external fun getSampleData(trigger: Boolean, ins: Int, key: Int, period: Int, chn: Int, width: Int, buffer: ByteArray)
    external fun setSequence(seq: Int): Boolean
    external fun getSeqVars(vars: IntArray)
    external fun setVolume(vol: Int): Int

    init {
        System.loadLibrary("xmp")
    }
}
