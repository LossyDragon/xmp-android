package org.helllabs.android.xmp.model

import androidx.compose.runtime.*

/**
 * @see [org.helllabs.android.xmp.Xmp.testFromFd]
 * @see [org.helllabs.android.xmp.Xmp.testModuleFd]
 */
@Stable
data class ModInfo(val name: String = "", val type: String = "")

/**
 * @see [org.helllabs.android.xmp.Xmp.getChannelData]
 */
@Stable
data class ChannelInfo(
    val volumes: IntArray = IntArray(64),
    val finalVols: IntArray = IntArray(64),
    val pans: IntArray = IntArray(64),
    val instruments: IntArray = IntArray(64),
    val keys: IntArray = IntArray(64),
    val periods: IntArray = IntArray(64),
    val holdVols: IntArray = IntArray(64)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChannelInfo

        if (!volumes.contentEquals(other.volumes)) return false
        if (!finalVols.contentEquals(other.finalVols)) return false
        if (!pans.contentEquals(other.pans)) return false
        if (!instruments.contentEquals(other.instruments)) return false
        if (!keys.contentEquals(other.keys)) return false
        if (!periods.contentEquals(other.periods)) return false
        if (!holdVols.contentEquals(other.holdVols)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = volumes.contentHashCode()
        result = 31 * result + finalVols.contentHashCode()
        result = 31 * result + pans.contentHashCode()
        result = 31 * result + instruments.contentHashCode()
        result = 31 * result + keys.contentHashCode()
        result = 31 * result + periods.contentHashCode()
        result = 31 * result + holdVols.contentHashCode()
        return result
    }
}

/**
 * @see [org.helllabs.android.xmp.Xmp.getModVars]
 */
@Immutable
data class ModVars(
    val seqDuration: Int = 0,
    val lengthInPatterns: Int = 0,
    val numPatterns: Int = 0,
    val numChannels: Int = 0,
    val numInstruments: Int = 0,
    val numSamples: Int = 0,
    val numSequence: Int = 0,
    val currentSequence: Int = 0
)

/**
 * @see [org.helllabs.android.xmp.Xmp.getInfo]
 */
@Immutable
data class FrameInfo(
    val pos: Int = 0,
    val pattern: Int = 0,
    val row: Int = 0,
    val numRows: Int = 0,
    val frame: Int = 0,
    val speed: Int = 0,
    val bpm: Int = 0
)

/**
 * @see [org.helllabs.android.xmp.Xmp.getSeqVars]
 */
@Stable
data class SequenceVars(val sequence: IntArray = intArrayOf()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SequenceVars

        return sequence.contentEquals(other.sequence)
    }

    override fun hashCode(): Int = sequence.contentHashCode()
}
