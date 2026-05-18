package org.helllabs.libxmp.model

import androidx.compose.runtime.Immutable

/**
 * @see [org.helllabs.libxmp.Xmp.getModVars]
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
