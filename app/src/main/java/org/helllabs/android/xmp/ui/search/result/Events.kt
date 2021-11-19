package org.helllabs.android.xmp.ui.search.result

sealed class ModuleEvent {
    object RandomModule : ModuleEvent()
    data class Module(val id: Int) : ModuleEvent()
    data class DownloadModule(val mod: String, val url: String, val file: String) : ModuleEvent()
}
