package com.lossydragon.media3.core

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.lossydragon.media3.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val LOG_CAT_COUNT = 500
        private const val CRASH_FILE_HISTORY_COUNT = 2
        private const val CRASH_FOLDER = "XmpCrashLogs"

        val timestamp: String
            get() = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())

        @Suppress("SameParameterValue")
        private fun logcatCommand(count: Int): String =
            "logcat -d -t $count --pid=${android.os.Process.myPid()}"

        fun initialize(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(
                CrashHandler(context.applicationContext, defaultHandler)
            )
        }
    }

    private val recentLogcat: String
        get() = try {
            val process = Runtime.getRuntime().exec(logcatCommand(LOG_CAT_COUNT))
            process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Failed to retrieve logcat: ${e.message}"
        }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        saveCrashToFile(throwable)
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun saveCrashToFile(throwable: Throwable) {
        try {
            val stackTrace = StringWriter().apply {
                throwable.printStackTrace(PrintWriter(this))
            }.toString()

            val time = timestamp
            val fileName = "xmp_crash_$time.txt"

            val crashReport = buildString {
                appendLine("Timestamp: $time")
                appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine()
                appendLine("---------- Device Information ----------")
                appendLine("${Build.MANUFACTURER} - ${Build.BRAND} - ${Build.MODEL}")
                appendLine("Android Version: ${Build.VERSION.RELEASE}")
                appendLine()
                appendLine("---------- Cause ----------")
                appendLine("Exception: ${throwable.javaClass.name}")
                appendLine("Message: ${throwable.message}")
                appendLine()
                appendLine("---------- Stack Trace ----------")
                appendLine(stackTrace)
                appendLine()
                appendLine("---------- Logcat ----------")
                appendLine(recentLogcat)
            }

            writeToDownloads(fileName, crashReport)
            cleanupOldCrashFiles()
        } catch (_: Exception) {
            defaultHandler?.uncaughtException(Thread.currentThread(), throwable)
        }
    }

    private fun writeToDownloads(fileName: String, content: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ — use MediaStore (no permissions needed)
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/$CRASH_FOLDER"
                )
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    stream.write(content.toByteArray())
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(it, values, null, null)
            }
        } else {
            // Android 9 and below — direct file write (needs WRITE_EXTERNAL_STORAGE permission)
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                CRASH_FOLDER
            ).apply { mkdirs() }

            File(downloadsDir, fileName).writeText(content)
        }
    }

    private fun cleanupOldCrashFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Query MediaStore for our crash files and delete the oldest beyond the limit
            val resolver = context.contentResolver
            val projection = arrayOf(
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.DATE_MODIFIED
            )
            val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ? AND " +
                "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$CRASH_FOLDER%", "xmp_crash_%.txt")
            val sortOrder = "${MediaStore.Downloads.DATE_MODIFIED} DESC"

            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                var count = 0
                while (cursor.moveToNext()) {
                    count++
                    if (count > CRASH_FILE_HISTORY_COUNT) {
                        val id = cursor.getLong(idCol)
                        val deleteUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                            .buildUpon().appendPath(id.toString()).build()
                        resolver.delete(deleteUri, null, null)
                    }
                }
            }
        } else {
            // Android 9 and below — direct file cleanup
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                CRASH_FOLDER
            )
            downloadsDir.listFiles()?.let { files ->
                if (files.size > CRASH_FILE_HISTORY_COUNT) {
                    files.sortedByDescending { it.lastModified() }
                        .drop(CRASH_FILE_HISTORY_COUNT)
                        .forEach { it.delete() }
                }
            }
        }
    }
}
