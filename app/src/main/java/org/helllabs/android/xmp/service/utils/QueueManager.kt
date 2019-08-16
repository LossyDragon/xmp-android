package org.helllabs.android.xmp.service.utils

import java.util.Collections


class QueueManager(private val list: MutableList<String>, start: Int, private val shuffleMode: Boolean, private val loopListMode: Boolean, keepFirst: Boolean) {
    private val ridx: RandomIndex
    var index: Int = 0
    private var randomStart: Int = 0

    val filename: String
        get() {
            val idx = if (shuffleMode) ridx.getIndex(index) else index
            return list[idx]
        }

    init {
        var start = start
        if (start >= list.size) {
            start = list.size - 1
        }

        if (keepFirst) {
            Collections.swap(list, 0, start)
            start = 0
            randomStart = 1
        }

        index = start
        ridx = RandomIndex(randomStart, list.size)
    }

    fun add(fileList: List<String>) {
        if (!fileList.isEmpty()) {
            ridx.extend(fileList.size, index + 1)
            list.addAll(fileList)
        }
    }

    fun size(): Int {
        return list.size
    }

    operator fun next(): Boolean {
        index++
        if (index >= list.size) {
            if (loopListMode) {
                ridx.randomize()
                index = 0
            } else {
                return false
            }
        }
        return true
    }

    fun previous() {
        index -= 2
        if (index < -1) {
            if (loopListMode) {
                index += list.size
            } else {
                index = -1
            }
        }
    }

    fun restart() {
        index = -1
    }
}
