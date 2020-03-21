package org.helllabs.android.xmp.modarchive.model

import org.helllabs.android.xmp.extension.fromHtml

class Module {
    var artist: String? = null
    var filename: String? = null
    var format: String? = null
    var url: String? = null
    var bytes: Int = 0
    var songTitle: String? = null
        set(songTitle) {
            field = if (songTitle!!.isEmpty()) {
                UNTITLED
            } else {
                fromHtml(songTitle).toString()
            }
        }
    var license: String? = null
        set(license) {
            field = fromHtml(license).toString()
        }
    var licenseDescription: String? = null
        set(licenseDescription) {
            field = fromHtml(licenseDescription).toString()
        }
    var legalUrl: String? = null
        set(legalUrl) {
            field = fromHtml(legalUrl).toString()
        }
    var instruments: String? = null
        set(instruments) {
            val lines = instruments!!
                    .split("\n".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val buffer = StringBuilder()
            lines.forEach {
                buffer.append(fromHtml(it).toString())
                buffer.append('\n')
            }
            field = buffer.toString()
        }
    var id: Long = 0

    init {
        artist = Artist.UNKNOWN
    }

    override fun toString(): String = songTitle!!

    companion object {
        private const val UNTITLED = "(untitled)"
    }
}
