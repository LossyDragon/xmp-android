package org.helllabs.android.xmp.modarchive.response

import java.util.ArrayList

abstract class ListResponse<T> : ModArchiveResponse() {

    val list = ArrayList<T>()

    val isEmpty: Boolean
        get() = list.isEmpty()

    fun add(item: T) {
        list.add(item)
    }

    fun getList(): List<T> {
        return list
    }

    operator fun get(location: Int): T {
        return list[location]
    }
}
