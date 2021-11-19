package org.helllabs.android.xmp.ui.search.result

import org.helllabs.android.xmp.model.ModuleResult

data class ModuleState(
    val module: ModuleResult? = null,
    val moduleExists: Boolean = false,
    val moduleSupported: Boolean = true,
    val softError: String? = null,
)
