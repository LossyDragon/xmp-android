package org.helllabs.android.xmp.player

import android.content.Context
import android.content.res.Configuration


private val digits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

private val hexDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

fun Context.getScreenSize(): Int {
    return this.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
}

fun to2d(res: CharArray, value: Int) {
    res[0] = if (value < 10) ' ' else digits[value / 10]
    res[1] = digits[value % 10]
}

fun to02d(res: CharArray, value: Int) {
    res[0] = digits[value / 10]
    res[1] = digits[value % 10]
}

fun to02X(res: CharArray, value: Int) {
    res[0] = hexDigits[value shr 4]
    res[1] = hexDigits[value and 0x0f]
}
