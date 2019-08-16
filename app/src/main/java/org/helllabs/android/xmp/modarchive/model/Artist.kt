package org.helllabs.android.xmp.modarchive.model

class Artist {
    var alias: String? = null
        set(alias) = if (alias == null || alias.isEmpty()) {
            field = Artist.UNKNOWN
        } else {
            field = alias
        }
    var id: Long = 0

    override fun toString(): String {
        return alias!!
    }

    companion object {
        val UNKNOWN = "unknown"
    }
}
