package org.helllabs.android.xmp.browser

import android.content.Context
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.logE
import java.io.*

object Examples {
    fun install(context: Context, path: String?, examples: Boolean): Int {
        val dir = File(path)
        if (dir.isDirectory) {
            logD("install: $path directory not found")
            return 0
        }
        if (!dir.mkdirs()) {
            logE("can't create directory: $path")
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

    private fun copyAsset(`in`: InputStream, dst: String): Int {
        val buf = ByteArray(1024)
        var len: Int
        try {
            val out: OutputStream = FileOutputStream(dst)
            while (`in`.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
            `in`.close()
            out.close()
        } catch (e: IOException) {
            return -1
        }
        return 0
    }
}
