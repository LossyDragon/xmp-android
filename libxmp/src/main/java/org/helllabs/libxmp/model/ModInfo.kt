package org.helllabs.libxmp.model

import androidx.compose.runtime.Immutable

/**
 * @see [org.helllabs.libxmp.Xmp.testFromFd]
 * @see [org.helllabs.libxmp.Xmp.testModuleFd]
 */
@Immutable
data class ModInfo(val name: String = "", val type: String = "")
