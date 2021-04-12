package org.helllabs.android.xmp.util

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.presentation.ui.preferences.Preferences

object InfoCache {

    fun clearCache(filename: String): Boolean {
        val cacheFile = File(Preferences.CACHE_DIR, "$filename.cache")
        val skipFile = File(Preferences.CACHE_DIR, "$filename.skip")
        var ret = false
        if (cacheFile.isFile) {
            cacheFile.delete()
            ret = true
        }
        if (skipFile.isFile) {
            skipFile.delete()
            ret = true
        }
        return ret
    }

    private fun removeCacheDir(filename: String): Boolean {
        val cacheFile = File(Preferences.CACHE_DIR, "$filename.cache")
        var ret = false
        if (cacheFile.isDirectory) {
            cacheFile.delete()
            ret = true
        }
        return ret
    }

    fun delete(filename: String): Boolean {
        val file = File(filename)
        clearCache(filename)
        return file.delete()
    }

    fun deleteRecursive(filename: String): Boolean {
        val file = File(filename)
        return if (file.isDirectory) {
            for (f in file.listFiles()!!) {
                if (f.isDirectory) {
                    deleteRecursive(f.path)
                } else {
                    f.delete()
                }
            }
            file.delete()
            removeCacheDir(filename)
            true
        } else {
            clearCache(filename)
            file.delete()
        }
    }

    fun fileExists(filename: String): Boolean {
        val file = File(filename)
        if (file.isFile) {
            return true
        }
        clearCache(filename)
        return false
    }

    fun testModuleForceIfInvalid(filename: String): Boolean {
        val skipFile = File(Preferences.CACHE_DIR, "$filename.skip")
        if (skipFile.isFile) {
            skipFile.delete()
        }
        return testModule(filename)
    }

    fun testModule(filename: String): Boolean {
        return testModule(filename, ModInfo())
    }

    @Throws(IOException::class)
    private fun checkIfCacheValid(file: File, cacheFile: File, info: ModInfo): Boolean {
        var ret = false
        val reader = BufferedReader(FileReader(cacheFile), 512)
        val line = reader.readLine()
        if (line != null) {
            try {
                val size = line.toInt()
                if (size.toLong() == file.length()) {
                    info.name = reader.readLine()
                    reader.readLine() // skip filename
                    info.type = reader.readLine()
                    ret = true
                }
            } catch (e: NumberFormatException) {
                // Someone had binary contents in the cache file, breaking parseInt()
                ret = false
            }
        }
        reader.close()
        return ret
    }

    private fun testModule(filename: String, info: ModInfo): Boolean {
        if (!Preferences.CACHE_DIR.isDirectory && !Preferences.CACHE_DIR.mkdirs()) {
            // Can't use cache
            return Xmp.testModule(filename, info)
        }
        val file = File(filename)
        val cacheFile = File(Preferences.CACHE_DIR, "$filename.cache")
        val skipFile = File(Preferences.CACHE_DIR, "$filename.skip")
        return try {
            // If cache file exists and size matches, file is mod
            if (cacheFile.isFile) {

                // If we have cache and skip, delete skip
                if (skipFile.isFile) {
                    skipFile.delete()
                }

                // Check if our cache data is good
                if (checkIfCacheValid(file, cacheFile, info)) {
                    return true
                }
                cacheFile.delete() // Invalid or outdated cache file
            }
            if (skipFile.isFile) {
                return false
            }
            val isMod = Xmp.testModule(filename, info)
            if (isMod) {
                val lines = listOf(
                    file.length().toString(),
                    info.name,
                    filename,
                    info.type
                )
                val dir = cacheFile.parentFile!!
                if (!dir.isDirectory) {
                    dir.mkdirs()
                }
                cacheFile.createNewFile()
                FileUtils.writeToFile(cacheFile, lines)
            } else {
                val dir = skipFile.parentFile!!
                if (!dir.isDirectory) {
                    dir.mkdirs()
                }
                skipFile.createNewFile()
            }
            isMod
        } catch (e: IOException) {
            Xmp.testModule(filename, info)
        }
    }
}
