package org.helllabs.libxmp.model

import androidx.compose.runtime.Immutable

/**
 * @see [org.helllabs.libxmp.Xmp.getInfo]
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
