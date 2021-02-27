package org.helllabs.android.xmp.player.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.RemoteException
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.logE

@SuppressLint("ViewConstructor")
class InstrumentViewer(context: Context, background: Int) : Viewer(context, background) {

    // TODO: Programmatically calc this from primary to background, with more steps.
    // Theme the bar paint from background up to the 'accent' color
    private var back = String.format("#%06X", 0xFFFFFF and background)
    private val barTheme =
        arrayOf(back, "#14264a", "#1a305d", "#1f396f", "#244382", "#294c94", "#2e56a7", "#3460ba")

    private lateinit var insName: Array<String>
    private val barPaint = arrayListOf<Paint>()
    private val fontHeight: Int
    private val fontSize: Int = resources.getDimensionPixelSize(R.dimen.instrumentview_font_size)
    private val fontWidth: Int
    private val insPaint = arrayListOf<Paint>()
    private val rect = Rect()

    // Draw Loop Variables
    private var chn: Int = 0
    private var drawWidth: Int = 0
    private var drawX: Int = 0
    private var drawY: Int = 0
    private var ins: Int = 0
    private var maxVol: Int = 0
    private var vol: Int = 0

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
            // val value = 15 * i
            barPaint.add(
                Paint().apply {
                    // setARGB(255, (value / 4), (value / 2), value)
                    color = Color.parseColor(barTheme[i])
                    alpha = 255
                }
            )
        }

        fontWidth = insPaint[0].measureText("X").toInt()
        fontHeight = fontSize * 14 / 10
    }

    override fun setup(modVars: IntArray) {
        super.setup(modVars)
        logD("Viewer Setup")

        try {
            insName = Xmp.getInstruments()
        } catch (e: RemoteException) {
            logE("Can't get instrument name")
        }

        setMaxY(modVars[4] * fontHeight + fontHeight / 2)
    }

    override fun update(info: Info?, paused: Boolean) {
        super.update(info, paused)

        requestCanvasLock { canvas ->
            doDraw(canvas, info)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun doDraw(canvas: Canvas, info: Info?) {
        chn = modVars[3]
        ins = modVars[4]

        // Clear screen
        canvas.drawColor(bgColor)

        for (i in 0 until ins) {
            drawY = (i + 1) * fontHeight - posY.toInt()
            drawWidth = (canvasWidth - 3 * fontWidth) / chn
            maxVol = 0

            // Don't draw if not visible
            if (drawY < 0 || drawY > canvasHeight + fontHeight) {
                continue
            }

            for (j in 0 until chn) {
                if (isMuted[j]) {
                    continue
                }
                if (info!!.instruments[j] == i) {
                    drawX = 3 * fontWidth + drawWidth * j
                    vol = info.volumes[j] / 8

                    if (vol > 7) {
                        vol = 7
                    }

                    // TODO: Center or pad bars a bit more
                    rect[drawX, drawY - fontSize + 1, drawX + drawWidth * 8 / 10] = drawY + 1
                    canvas.drawRect(rect, barPaint[vol])
                    if (vol > maxVol) {
                        maxVol = vol
                    }
                }
            }
            canvas.drawText(insName[i], 0f, drawY.toFloat(), insPaint[maxVol])
        }
    }
}
