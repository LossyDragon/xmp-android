package org.helllabs.android.xmp.modarchive.model

class Artist {

    private var alias: String? = null
    var id: Long = 0

    fun getAlias(): String? {
        return alias
    }

    fun setAlias(alias: String?) {
        if (alias == null || alias.isEmpty()) {
            this.alias = UNKNOWN
        } else {
            this.alias = alias
        }
    }

    override fun toString(): String {
        return alias!!
    }

    companion object {
        const val UNKNOWN = "unknown"
    }
}
