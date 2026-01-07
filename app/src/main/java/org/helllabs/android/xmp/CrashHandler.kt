package org.helllabs.android.xmp

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A local running crash handler.
 * Any uncaught exceptions will be saved located locally in a text file, aka: Crash Report.
 * File location: /<user storage>/Android/data/org.helllabs.android.xmp/files/crash_logs/
 */
class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val LOG_CAT_COUNT = 500
        private const val CRASH_FILE_HISTORY_COUNT = 2

        val timestamp: String
            get() = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())

        /**
         * Logcat command
         */
        @Suppress("SameParameterValue")
        private fun logcatCommand(count: Int): String =
            "logcat -d -t $count --pid=${android.os.Process.myPid()}"

        fun initialize(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            val crashHandler = CrashHandler(context.applicationContext, defaultHandler)
            Thread.setDefaultUncaughtExceptionHandler(crashHandler)
        }
    }

    private val crashFileDir by lazy {
        File(context.getExternalFilesDir(null), "crash_logs").apply {
            if (!exists()) mkdirs()
        }
    }

    private val recentLogcat: String
        get() = try {
            val process = Runtime.getRuntime().exec(logcatCommand(LOG_CAT_COUNT))
            process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Failed to retrieve logcat: ${e.message}"
        }

    private val cleanupOldCrashFiles: () -> Unit = {
        crashFileDir.listFiles()?.let { files ->
            if (files.size > CRASH_FILE_HISTORY_COUNT) {
                files.sortByDescending { it.lastModified() }
                files.drop(CRASH_FILE_HISTORY_COUNT).forEach { it.delete() }
            }
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        saveCrashToFile(throwable)
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun saveCrashToFile(throwable: Throwable) {
        try {
            val stackTrace = StringWriter().apply {
                val pw = PrintWriter(this)
                throwable.printStackTrace(pw)
            }.toString()

            val time = timestamp

            val crashReport = buildString {
                appendLine("Timestamp: $time")
                appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine()
                appendLine("---------- Device Information ----------")
                appendLine(
                    "${android.os.Build.MANUFACTURER} - ${android.os.Build.BRAND} - ${android.os.Build.MODEL}"
                )
                appendLine("Android Version: ${android.os.Build.VERSION.RELEASE}")
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

            File(crashFileDir, "xmp_crash_$time.txt").writeText(crashReport)

            cleanupOldCrashFiles()
        } catch (_: Exception) {
            defaultHandler?.uncaughtException(Thread.currentThread(), throwable)
        }
    }
}
