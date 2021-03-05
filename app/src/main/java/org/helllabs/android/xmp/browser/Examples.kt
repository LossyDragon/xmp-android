package org.helllabs.android.xmp.browser

import android.content.Context
import java.io.File
import org.helllabs.android.xmp.util.logE
import org.helllabs.android.xmp.util.logW

// Should be moved with FileUtils.
/**
 * Handles installing sample modules from assets into the specified folder
 * @param path the directory to install the sample(s).
 * @param shouldInstall return false if we shouldn't install the sample.
 */
fun Context.installAssets(path: String, shouldInstall: Boolean): Int {
    val filePath = File(path)

    // Ignore installing examples if preference is false
    if (!shouldInstall) {
        return 0
    }

    // Return false if the filepath is not a directory.
    if (filePath.isDirectory) {
        logW("Install: $path directory not found")
        return -1
    }

    // Try and make the directory.
    if (!filePath.mkdirs()) {
        logE("Can't create directory: $path")
        return -1
    }

    val am = assets
    val assets: Array<String>? = am.list("mod")

    // Asset folder is empty.
    if (assets.isNullOrEmpty()) {
        return 0
    }

    assets.forEach { item ->
        am.open("mod/$item").use { stream ->
            File("$path/$item").outputStream().use {
                stream.copyTo(it)
            }
        }
    }

    return 1
}
