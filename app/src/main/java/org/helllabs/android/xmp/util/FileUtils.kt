package org.helllabs.android.xmp.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.*
import java.lang.Exception
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.presentation.ui.search.ModArchiveConstants

object FileUtils {

    /**
     * Handles installing sample modules from assets into the specified folder
     * @param context resource management.
     * @param path the directory to install the sample(s).
     * @param shouldInstall return false if we shouldn't install the sample.
     */
    fun installAssets(context: Context, path: String, shouldInstall: Boolean): Int {

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

        val am = context.assets
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

    /**
     * Get the File from an Intent URI
     * @param context activity context
     * @param uri the URI scheme from another application (ie: file manager)
     * @return a copy of the file in application cache dir.
     */
    fun getPathFromUri(context: Context, uri: Uri): String {
        val dest = File(context.cacheDir, getNameFromUri(context, uri))
        try {
            context.contentResolver.openInputStream(uri).use { ins ->
                dest.outputStream().use { out ->
                    ins!!.copyTo(out)
                    out.flush()
                    out.close()
                }
            }
        } catch (ex: Exception) {
            logE("URI Get File: ${ex.message}")
            ex.printStackTrace()
        }
        return dest.path
    }

    /**
     * Query the name from a URI (content://...)
     * @param context activity context
     * @param uri the URI scheme from another application (ie: file manager)
     * @return the actual name and extension of the file.
     */
    private fun getNameFromUri(context: Context, uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)!!
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        val name = cursor.getString(nameIndex)

        cursor.close()

        return name
    }

    @Throws(IOException::class)
    fun writeToFile(file: File, line: String) {
        writeToFile(file, listOf(line))
    }

    @Throws(IOException::class)
    fun writeToFile(file: File, lines: List<String>) {
        FileOutputStream(file, true).bufferedWriter().use { out ->
            lines.forEach {
                out.write(it)
                out.newLine()
            }
            out.close()
        }
    }

    @Throws(IOException::class)
    fun readFromFile(file: File): String {
        val line: String
        file.bufferedReader().use { reader ->
            line = reader.readLine()
            reader.close()
        }
        return line
    }

    /**
     * Remove a certain line in a file
     */
    @Throws(IOException::class)
    fun removeLineFromFile(file: File, num: IntArray): Boolean {
        val tempFile = File(file.absolutePath + ".tmp")
        val reader = BufferedReader(FileReader(file), 512)
        val writer = PrintWriter(FileWriter(tempFile))
        var line: String?
        var flag: Boolean
        var lineNum = 0
        while (reader.readLine().also { line = it } != null) {
            flag = false
            for (n in num) {
                if (lineNum == n) {
                    flag = true
                    break
                }
            }
            if (!flag) {
                writer.println(line)
                writer.flush()
            }
            lineNum++
        }
        writer.close()
        reader.close()

        // Delete the original file
        return if (!file.delete())
            false
        else
            tempFile.renameTo(file)
        // Rename the new file to the filename the original file had.
    }

    fun basename(pathname: String): String {
        return File(pathname).name.orEmpty()
    }

    fun localFile(module: Module?): File? {
        if (module == null)
            return null

        val url = module.url
        val moduleFilename = url!!.substring(url.lastIndexOf('#') + 1, url.length)
        return File(getDownloadPath(module), moduleFilename)
    }

    fun localFile(url: String, path: String): File {
        val filename = url.substring(url.lastIndexOf('#') + 1, url.length)
        return File(path, filename)
    }

    fun getDownloadPath(module: Module?): String {
        val sb = StringBuilder()
        sb.append(PrefManager.mediaPath)

        if (PrefManager.useModArchiveFolder) {
            sb.append(File.separatorChar)
            sb.append(ModArchiveConstants.DEFAULT_FOLDER)
        }

        if (PrefManager.useArtistFolder) {
            sb.append(File.separatorChar)
            sb.append(module!!.getArtist().asHtml())
        }

        return sb.toString()
    }
}
