package org.helllabs.android.xmp.compose.ui.player

import android.annotation.SuppressLint
import kotlinx.collections.immutable.persistentListOf

object Util {

    private val noteName =
        persistentListOf("C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B ")

    @SuppressLint("DefaultLocale")
    fun note(num: Int): String = if (num > 128) {
        "==="
    } else if (num > 0) {
        String.format("%s%d", noteName[(num - 1) % 12], (num - 1) / 12)
    } else {
        "---"
    }

    fun num(num: Int): String = if (num <= 0) "--" else String.format("%02X", num)

    /**
     * Updates the Player Info text either by Hex or Numerical Value
     */
    fun updateFrameInfo(showHex: Boolean, value: Int): String {
        return if (showHex) {
            "%02X".format(value)
        } else {
            "%02d".format(value)
        }
    }

    fun updateTime(value: Int): String {
        val t = value.coerceAtLeast(0)
        val minutes = t / 60
        val seconds = t % 60
        return "%2d:%02d".format(minutes, seconds)
    }
}
