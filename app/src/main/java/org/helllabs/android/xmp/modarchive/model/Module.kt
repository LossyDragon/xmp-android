package org.helllabs.android.xmp.modarchive.model

import org.helllabs.android.xmp.util.asHtml

class Module {
    var artist: String = Artist.UNKNOWN
    var filename: String? = null
    var format: String? = null
    var url: String? = null
    var bytes = 0
    var songTitle: String? = null
        private set
    var license: String? = null
        set(license) {
            field = license.asHtml()
        }
    var licenseDescription: String? = null
        set(licenseDescription) {
            field = licenseDescription.asHtml()
        }
    var legalUrl: String? = null
        set(legalUrl) {
            field = legalUrl.asHtml()
        }
    var instruments: String? = null
        private set
    var id: Long = 0

    fun setInstruments(instruments: String) {
        val lines = instruments.split("\n").toTypedArray()
        val buffer = StringBuilder()
        for (line in lines) {
            buffer.append(line.asHtml())
            buffer.append('\n')
        }
        this.instruments = buffer.toString()
    }

    fun setSongTitle(title: String) {
        songTitle = if (title.isEmpty()) {
            UNTITLED
        } else {
            title.asHtml()
        }
    }

    override fun toString(): String {
        return songTitle!!
    }

    companion object {
        private const val UNTITLED = "(untitled)"
    }
}
