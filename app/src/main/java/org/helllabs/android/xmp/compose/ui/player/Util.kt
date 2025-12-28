package org.helllabs.android.xmp.compose.ui.player

import android.annotation.SuppressLint
import org.helllabs.android.xmp.core.PrefManager

object Util {

    private val s = StringBuilder()
    private val c = CharArray(2)

    private val digits = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    )
    private val hexDigits = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    )

    private val noteName =
        arrayOf("C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B ")

    @SuppressLint("DefaultLocale")
    fun note(num: Int): String = if (num > 128) {
        "==="
    } else if (num > 0) {
        String.format("%s%d", noteName[(num - 1) % 12], (num - 1) / 12)
    } else {
        "---"
    }

    fun num(num: Int): String = if (num <= 0) "--" else String.format("%02X", num)

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
        res[1] = hexDigits[value shr 4 and 0x0f]
        res[2] = hexDigits[value and 0x0f]
    }

    /**
     * Updates the Player Info text either by Hex or Numerical Value
     */
    fun updateFrameInfo(showHex: Boolean, value: Int): String {
        s.delete(0, s.length)
        if (showHex) {
            to02X(c, value)
            s.append(c)
        } else {
            value.let {
                if (it < 10) s.append(0)
                s.append(it)
            }
        }

        return s.toString()
    }

    fun updateTime(value: Int): String {
        val t = if (value < 0) 0 else value
        s.delete(0, s.length)
        to2d(c, t / 60)
        s.append(c)
        s.append(":")
        to02d(c, t % 60)
        s.append(c)

        return s.toString()
    }
}
