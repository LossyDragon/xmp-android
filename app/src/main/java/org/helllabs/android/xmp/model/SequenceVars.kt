package org.helllabs.android.xmp.model

import androidx.compose.runtime.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * @see [org.helllabs.libxmp.Xmp.getSeqVars]
 */
@Immutable
data class SequenceVars(val sequence: ImmutableList<Int> = persistentListOf())
