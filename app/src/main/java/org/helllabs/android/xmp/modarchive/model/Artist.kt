package org.helllabs.android.xmp.modarchive.model

class Artist {
    var alias: String? = null
        set(alias) {
            field = if (alias == null || alias.isEmpty()) {
                UNKNOWN
            } else {
                alias
            }
        }

    var id: Long = 0

    override fun toString(): String {
        return alias!!
    }

    companion object {
        const val UNKNOWN = "unknown"
    }
}
