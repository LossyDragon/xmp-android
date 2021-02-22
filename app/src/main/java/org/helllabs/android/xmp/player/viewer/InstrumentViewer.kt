package org.helllabs.android.xmp.player.viewer

import android.content.Context
import android.graphics.*
import android.os.RemoteException
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.service.ModInterface
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.logE

class InstrumentViewer(context: Context) : Viewer(context) {

    private lateinit var insName: Array<String>
    private val insPaint = arrayListOf<Paint>()
    private val barPaint = arrayListOf<Paint>()
    private val fontSize: Int = resources.getDimensionPixelSize(R.dimen.instrumentview_font_size)
    private val fontHeight: Int
    private val fontWidth: Int
    private val rect = Rect()

    init {
        for (i in 0..7) {
            val value = 120 + 10 * i
            insPaint.add(
                Paint().apply {
                    setARGB(255, value, value, value)
                    typeface = Typeface.MONOSPACE
                    textSize = fontSize.toFloat()
                    isAntiAlias = true
                }
            )
        }

        for (i in 0..7) {
            val value = 15 * i
            barPaint.add(
                Paint().apply {
                    setARGB(255, (value / 4), (value / 2), value)
                }
            )
        }

        fontWidth = insPaint[0].measureText("X").toInt()
        fontHeight = fontSize * 14 / 10
    }

    override fun setup(modPlayer: ModInterface, modVars: IntArray) {
        super.setup(modPlayer, modVars)
        logD("Viewer Setup")

        try {
            insName = modPlayer.instruments
        } catch (e: RemoteException) {
            logE("Can't get instrument name")
        }

        setMaxY(modVars[4] * fontHeight + fontHeight / 2)
    }

    override fun update(info: Info?, paused: Boolean) {
        super.update(info, paused)

        requestCanvasLock { canvas ->
            doDraw(canvas, modPlayer!!, info)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun doDraw(canvas: Canvas, modPlayer: ModInterface, info: Info?) {
        val chn = modVars[3]
        val ins = modVars[4]

        // Clear screen
        canvas.drawColor(Color.BLACK)

        for (i in 0 until ins) {
            val y = (i + 1) * fontHeight - posY.toInt()
            val width = (canvasWidth - 3 * fontWidth) / chn
            var maxVol = 0

            // Don't draw if not visible
            if (y < 0 || y > canvasHeight + fontHeight) {
                continue
            }

            for (j in 0 until chn) {
                if (isMuted[j]) {
                    continue
                }
                if (info!!.instruments[j] == i) {
                    val x = 3 * fontWidth + width * j
                    var vol = info.volumes[j] / 8

                    if (vol > 7) {
                        vol = 7
                    }

                    // TODO: Center or pad bars a bit more
                    rect[x, y - fontSize + 1, x + width * 8 / 10] = y + 1
                    canvas.drawRect(rect, barPaint[vol])
                    if (vol > maxVol) {
                        maxVol = vol
                    }
                }
            }
            canvas.drawText(insName[i], 0f, y.toFloat(), insPaint[maxVol])
        }
    }
}
