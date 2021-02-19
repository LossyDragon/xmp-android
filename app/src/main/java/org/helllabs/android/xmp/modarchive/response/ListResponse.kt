package org.helllabs.android.xmp.modarchive.response

import java.util.*

abstract class ListResponse<T> : ModArchiveResponse() {

    private val list: MutableList<T> = ArrayList()

    fun add(item: T) {
        list.add(item)
    }

    fun getList(): List<T> {
        return list
    }

    val isEmpty: Boolean
        get() = list.isEmpty()

    operator fun get(location: Int): T {
        return list[location]
    }
}