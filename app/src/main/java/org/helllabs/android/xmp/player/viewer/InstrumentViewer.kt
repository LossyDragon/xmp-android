package org.helllabs.android.xmp.player.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.RemoteException
import androidx.core.graphics.ColorUtils
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.util.color
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.logE

@SuppressLint("ViewConstructor")
class InstrumentViewer(context: Context, val background: Int) : Viewer(context, background) {

    private val startBlue: Int = resources.color(R.color.accent)

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
        // White text volume shades
        for (i in 0..10) {
            val value: Float = (i / 10f)
            logD("Text Value $i: $value")
            insPaint.add(
                Paint().apply {
                    color = ColorUtils.blendARGB(Color.GRAY, Color.WHITE, value)
                    alpha = 255
                    typeface = Typeface.MONOSPACE
                    textSize = fontSize.toFloat()
                    isAntiAlias = true
                }
            )
        }

        // Blue bar volume shades
        for (i in 10 downTo 0) {
            val value: Float = (i / 10f)
            logD("Bar Value $i: $value")
            barPaint.add(
                Paint().apply {
                    color = ColorUtils.blendARGB(startBlue, background, value)
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
                    vol = info.volumes[j] / 6

                    // Clamp
                    if (vol > 60) {
                        vol = 60
                    }

                    rect.set(drawX, drawY - fontSize + 4, drawX + drawWidth * 8 / 10, drawY + 8)
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
