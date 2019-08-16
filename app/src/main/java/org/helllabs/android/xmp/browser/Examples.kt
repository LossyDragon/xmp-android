package org.helllabs.android.xmp.browser

import java.io.File

import android.content.Context
import android.content.res.AssetManager

import org.helllabs.android.xmp.util.Log

import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.FileOutputStream


object Examples {

    private val TAG = "Examples"

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

    private fun copyAsset(`in`: InputStream, dst: String) {

        Log.i(TAG, "copying asset")

        try {
            val out = FileOutputStream(File(dst))

            `in`.copyTo(out, 1024)

            `in`.close()
            out.close()

        } catch (ignored: FileNotFoundException) {
        } catch (ignored: IOException) {
        }
    }
}
