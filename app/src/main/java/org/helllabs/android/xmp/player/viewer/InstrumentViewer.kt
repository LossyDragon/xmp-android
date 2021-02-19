package org.helllabs.android.xmp.player.viewer

import android.content.Context
import android.graphics.*
import android.os.RemoteException
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.service.ModInterface
import org.helllabs.android.xmp.util.Log

class InstrumentViewer(context: Context) : Viewer(context) {

    private lateinit var insName: Array<String>
    private val insPaint: Array<Paint?> = arrayOfNulls(8)
    private val barPaint: Array<Paint?> = arrayOfNulls(8)
    private val fontSize: Int = resources.getDimensionPixelSize(R.dimen.instrumentview_font_size)
    private val fontHeight: Int
    private val fontWidth: Int
    private val rect = Rect()

    init {
        for (i in 0..7) {
            val `val` = 120 + 10 * i
            insPaint[i] = Paint()
            insPaint[i]!!.setARGB(255, `val`, `val`, `val`)
            insPaint[i]!!.typeface = Typeface.MONOSPACE
            insPaint[i]!!.textSize = fontSize.toFloat()
            insPaint[i]!!.isAntiAlias = true
        }
        for (i in 0..7) {
            val `val` = 15 * i
            barPaint[i] = Paint()
            barPaint[i]!!.setARGB(255, `val` / 4, `val` / 2, `val`)
        }
        fontWidth = insPaint[0]!!.measureText("X").toInt()
        fontHeight = fontSize * 14 / 10
    }

    override fun setup(modPlayer: ModInterface, modVars: IntArray) {
        super.setup(modPlayer, modVars)
        val insNum = modVars[4]
        try {
            insName = modPlayer.instruments
        } catch (e: RemoteException) {
            Log.e(TAG, "Can't get instrument name")
        }
        setMaxY(insNum * fontHeight + fontHeight / 2)
    }

    override fun update(info: Info?, paused: Boolean) {
        super.update(info, paused)
        var canvas: Canvas? = null
        try {
            canvas = surfaceHolder.lockCanvas(null)
            if (canvas != null) {
                synchronized(surfaceHolder) { doDraw(canvas, modPlayer!!, info) }
            }
        } finally {
            // do this in a finally so that if an exception is thrown
            // during the above, we don't leave the Surface in an
            // inconsistent state
            if (canvas != null) {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }

    private fun doDraw(canvas: Canvas, modPlayer: ModInterface, info: Info?) {
        val chn = modVars[3]
        val ins = modVars[4]

        // Clear screen
        canvas.drawColor(Color.BLACK)
        for (i in 0 until ins) {
            val y = (i + 1) * fontHeight - posY.toInt()
            val width = (canvasWidth - 3 * fontWidth) / chn
            var maxVol: Int

            // Don't draw if not visible
            if (y < 0 || y > canvasHeight + fontHeight) {
                continue
            }
            maxVol = 0
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
                    rect[x, y - fontSize + 1, x + width * 8 / 10] = y + 1
                    canvas.drawRect(rect, barPaint[vol]!!)
                    if (vol > maxVol) {
                        maxVol = vol
                    }
                }
            }
            canvas.drawText(insName[i], 0f, y.toFloat(), insPaint[maxVol]!!)
        }
    }

    companion object {
        private const val TAG = "InstrumentViewer"
    }
}