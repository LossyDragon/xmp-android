package org.helllabs.android.xmp.player

object Util {
    private val digits = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    )
    private val hexDigits = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    )

    fun to2d(res: CharArray, `val`: Int) {
        res[0] = if (`val` < 10) ' ' else digits[`val` / 10]
        res[1] = digits[`val` % 10]
    }

    fun to02d(res: CharArray, `val`: Int) {
        res[0] = digits[`val` / 10]
        res[1] = digits[`val` % 10]
    }

    fun to02X(res: CharArray, `val`: Int) {
        res[0] = hexDigits[`val` shr 4]
        res[1] = hexDigits[`val` and 0x0f]
    }
}
