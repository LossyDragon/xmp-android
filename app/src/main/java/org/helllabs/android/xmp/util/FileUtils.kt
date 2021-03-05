package org.helllabs.android.xmp.util

import java.io.*

object FileUtils {

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
}
