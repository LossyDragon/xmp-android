package org.helllabs.android.xmp.player

import org.helllabs.android.xmp.util.logW

object Util {

    private val digits = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    )

    private val hexDigits = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    )

    val NOTES = arrayOf(
        "C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B "
    )

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

    fun to03X(res: CharArray, value: Int) {
        res[0] = hexDigits[value shr 8]
        res[1] = hexDigits[(value shr 4) and 0x0f]
        res[2] = hexDigits[value and 0x0f]
    }

    /* Shitty lookup table for Effects*/
    fun Int.effect(): String =
        when (this) {
            in 0..15 -> hexDigits[this].toString()
            16 -> "G" // FX_GLOBALVOL
            17 -> "H" // FX_GVOL_SLIDE
            21 -> "L" // FX_ENVPOS
            27 -> "Q" // FX_MULTI_RETRIG
            96 -> "A" // FX_669_PORTA_UP
            97 -> "B" // FX_669_PORTA_DN
            98 -> "C" // FX_669_TPORTA
            99 -> "D" // FX_669_FINETUNE
            10 -> "E" // FX_669_VIBRATO
            12 -> "F" // FX_SPEED_CP
            else -> {
                logW("Unknown Effect: $this")
                "?"
            }
        }
}
