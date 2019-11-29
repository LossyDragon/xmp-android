package org.helllabs.android.xmp.browser

import android.content.Context
import org.helllabs.android.xmp.util.Log
import java.io.*
import java.lang.Exception

object Examples {

    private val TAG = Examples::class.java.simpleName

    fun install(context: Context, path: String, examples: Boolean): Int {
        val dir = File(path)

        if (dir.isDirectory) {
            Log.d(TAG, "install: $path directory not found")
            return 0
        }

        if (!dir.mkdirs()) {
            Log.e(TAG, "can't create directory: $path")
            return -1
        }

        val am = context.resources.assets
        val assets: Array<String>?

        try {
            assets = am.list("mod")

            if (!examples || assets == null) {
                return 0
            }

            for (a in assets) {
                copyAsset(am.open("mod/$a"), "$path/$a")
            }
        } catch (e: IOException) {
            return -1
        }

        return 0
    }

    private fun copyAsset(inputStream: InputStream, destination: String) {
        try {
            Log.i(TAG, "copying asset")
            val file = File(destination)
            val out = FileOutputStream(file)

            inputStream.copyTo(out, 1024)
            inputStream.close()
            out.close()
        } catch (e: Exception) {
            when (e) {
                is FileNotFoundException ->
                    Log.e(TAG, "copyAsset FileNotFoundException")
                is IOException ->
                    Log.e(TAG, "copyAsset IOException")
            }
        }
    }
}
