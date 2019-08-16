package org.helllabs.android.xmp.util

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter

object FileUtils {

    @Throws(IOException::class)
    fun writeToFile(file: File, lines: Array<String>) {
        val out = BufferedWriter(FileWriter(file, true), 512)
        for (line in lines) {
            out.write(line)
            out.newLine()
        }
        out.close()
    }

    @Throws(IOException::class)
    fun writeToFile(file: File, line: String) {
        val lines = arrayOf(line)
        writeToFile(file, lines)
    }

    @Throws(IOException::class)
    fun readFromFile(file: File): String {
        val `in` = BufferedReader(FileReader(file), 512)
        val line = `in`.readLine()
        `in`.close()
        return line
    }

    @Throws(IOException::class)
    fun removeLineFromFile(file: File, num: Int): Boolean {
        val nums = intArrayOf(num)
        return removeLineFromFile(file, nums)
    }

    @Throws(IOException::class)
    fun removeLineFromFile(file: File, num: IntArray): Boolean {

        val tempFile = File(file.absolutePath + ".tmp")

        val reader = BufferedReader(FileReader(file), 512)
        val writer = PrintWriter(FileWriter(tempFile))

        var line: String?
        var flag: Boolean
        var lineNum = 0

        do {
            line = reader.readLine()

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

        } while (line != null)

        writer.close()
        reader.close()

        // Delete the original file
        return if (!file.delete()) {
            false
        } else tempFile.renameTo(file)

        // Rename the new file to the filename the original file had.

    }

    fun basename(pathname: String?): String {
        return if (pathname != null && !pathname.isEmpty()) {
            File(pathname).name
        } else {
            ""
        }
    }
}// do nothing
