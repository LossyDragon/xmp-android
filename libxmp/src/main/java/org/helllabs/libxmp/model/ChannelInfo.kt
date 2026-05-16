package org.helllabs.libxmp.model

/**
 * @see [org.helllabs.libxmp.Xmp.getChannelData]
 */
@Suppress("ArrayInDataClass") // TODO bad?
data class ChannelInfo(
    val volumes: IntArray = IntArray(64),
    val finalVols: IntArray = IntArray(64),
    val pans: IntArray = IntArray(64),
    val instruments: IntArray = IntArray(64),
    val keys: IntArray = IntArray(64),
    val periods: IntArray = IntArray(64),
    val holdVols: IntArray = IntArray(64)
)
