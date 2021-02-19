package org.helllabs.android.xmp.service.utils

import java.util.*

class RandomIndex(start: Int, size: Int) {
    private var idx: IntArray
    private val random: Random

    init {
        idx = IntArray(size)
        random = Random()
        val date = Date()
        random.setSeed(date.time)
        for (i in 0 until size) {
            idx[i] = i
        }
        randomize(start, size - start)
    }

    @JvmOverloads
    fun randomize(start: Int = 0, length: Int = idx.size) {
        val end = start + length
        for (i in start until end) {
            val num = start + random.nextInt(length)
            val temp = idx[i]
            idx[i] = idx[num]
            idx[num] = temp
        }
    }

    fun extend(amount: Int, index: Int) {
        val length = idx.size
        val newIdx = IntArray(length + amount)
        System.arraycopy(idx, 0, newIdx, 0, length)
        for (i in length until length + amount) {
            newIdx[i] = i
        }
        idx = newIdx
        randomize(index, idx.size - index)
    }

    fun getIndex(num: Int): Int {
        return idx[num]
    }
}
