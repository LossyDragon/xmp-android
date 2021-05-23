package org.helllabs.android.xmp.util

import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.model.ModInfo

/**
 * Singleton class to help test modules.
 */
object ModuleUtils {
//    fun testModule(file: File): Boolean {
//        val modInfo = ModInfo()
//        return Xmp.testModule(file.path, modInfo)
//    }

    fun testModule(filePath: String): Boolean {
        val modInfo = ModInfo()
        return Xmp.testModule(filePath, modInfo)
    }
}
