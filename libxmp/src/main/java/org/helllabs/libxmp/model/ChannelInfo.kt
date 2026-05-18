package org.helllabs.libxmp.model

import androidx.compose.runtime.Immutable

/**
 * @see [org.helllabs.libxmp.Xmp.getChannelData]
 */
@Immutable
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
        if (other !is ChannelInfo) return false
        return volumes.contentEquals(other.volumes) &&
            finalVols.contentEquals(other.finalVols) &&
            pans.contentEquals(other.pans) &&
            instruments.contentEquals(other.instruments) &&
            keys.contentEquals(other.keys) &&
            periods.contentEquals(other.periods) &&
            holdVols.contentEquals(other.holdVols)
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
