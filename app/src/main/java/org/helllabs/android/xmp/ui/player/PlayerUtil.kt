package org.helllabs.android.xmp.ui.player

import android.content.Context
import android.content.Intent
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.logI
import org.helllabs.android.xmp.util.toast

object PlayerUtil {

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

    fun addToQueue(
        context: Context,
        list: List<String>,
        isShuffle: Boolean = false,
        isLoop: Boolean = false,
        onBind: () -> Unit,
    ) {
        val realList = mutableListOf<String>()
        var realSize = 0
        var invalid = false

        for (filename in list) {
            if (Xmp.testModule(filename)) {
                realList.add(filename)
                realSize++
            } else {
                invalid = true
            }
        }

        if (invalid) {
            context.toast(R.string.msg_only_valid_files_sent)
        }

        if (realSize > 0) {
            if (PlayerService.isPlayerAlive.value == true) {
                onBind()
            } else {
                playModule(
                    context = context,
                    modList = list,
                    isShuffle = isShuffle,
                    isLoop = isLoop
                )
            }
        }
    }

    fun playModule(
        context: Context,
        modList: List<String>,
        start: Int = 0,
        isLoop: Boolean = false,
        isShuffle: Boolean = false,
        keepFirst: Boolean = false,
    ) {
        XmpApplication.fileList = modList
        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.PARM_SHUFFLE, isShuffle)
            putExtra(PlayerActivity.PARM_LOOP, isLoop)
            putExtra(PlayerActivity.PARM_START, start)
            putExtra(PlayerActivity.PARM_KEEPFIRST, keepFirst)
        }
        logI("Start Player activity")
        context.startActivity(intent)
    }
}
