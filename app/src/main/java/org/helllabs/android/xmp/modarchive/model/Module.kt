package org.helllabs.android.xmp.modarchive.model

import android.text.Html

class Module {
    var artist: String? = null
    var filename: String? = null
    var format: String? = null
    var url: String? = null
    var bytes: Int = 0
    var songTitle: String? = null
        set(songtitle) {
            field = if (songtitle!!.isEmpty()) {
                UNTITLED
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(songtitle).toString()
            }
        }
    var license: String? = null
        set(license) {
            @Suppress("DEPRECATION")
            field = Html.fromHtml(license).toString()
        }
    var licenseDescription: String? = null
        set(licenseDescription) {
            @Suppress("DEPRECATION")
            field = Html.fromHtml(licenseDescription).toString()
        }
    var legalUrl: String? = null
        set(legalUrl) {
            @Suppress("DEPRECATION")
            field = Html.fromHtml(legalUrl).toString()
        }
    var instruments: String? = null
        set(instruments) {
            val lines = instruments!!
                    .split("\n".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val buffer = StringBuilder()
            for (line in lines) {
                @Suppress("DEPRECATION")
                buffer.append(Html.fromHtml(line).toString())
                buffer.append('\n')
            }
            field = buffer.toString()
        }
    var id: Long = 0

    init {
        artist = Artist.UNKNOWN
    }

    override fun toString(): String {
        return songTitle!!
    }

    companion object {
        private const val UNTITLED = "(untitled)"
    }
}
