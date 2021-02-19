package org.helllabs.android.xmp.modarchive.model

import android.text.Html

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
            field = Html.fromHtml(license).toString()
        }
    var licenseDescription: String? = null
        set(licenseDescription) {
            field = Html.fromHtml(licenseDescription).toString()
        }
    var legalUrl: String? = null
        set(legalUrl) {
            field = Html.fromHtml(legalUrl).toString()
        }
    var instruments: String? = null
        private set
    var id: Long = 0

    fun setInstruments(instruments: String) {
        val lines = instruments.split("\n").toTypedArray()
        val buffer = StringBuilder()
        for (line in lines) {
            buffer.append(Html.fromHtml(line).toString())
            buffer.append('\n')
        }
        this.instruments = buffer.toString()
    }

    fun setSongTitle(songtitle: String) {
        songTitle = if (songtitle.isEmpty()) {
            UNTITLED
        } else {
            Html.fromHtml(songtitle).toString()
        }
    }

    override fun toString(): String {
        return songTitle!!
    }

    companion object {
        private const val UNTITLED = "(untitled)"
    }
}