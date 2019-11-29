package org.helllabs.android.xmp.player.viewer

import android.content.Context
import android.graphics.*
import android.os.RemoteException
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.player.to02X
import org.helllabs.android.xmp.service.ModInterface


// http://developer.android.com/guide/topics/graphics/2d-graphics.html

class PatternViewer(context: Context) : Viewer(context) {
    private val headerPaint: Paint
    private val headerTextPaint: Paint
    private val notePaint: Paint = Paint()
    private val insPaint: Paint
    private val barPaint: Paint
    private val muteNotePaint: Paint
    private val muteInsPaint: Paint
    private val fontSize: Int = resources.getDimensionPixelSize(R.dimen.patternview_font_size)
    private val fontHeight: Int
    private val fontWidth: Int
    private val allNotes = arrayOfNulls<String>(MAX_NOTES)
    private val hexByte = arrayOfNulls<String>(256)
    private val rowNotes = ByteArray(64)
    private val rowInstruments = ByteArray(64)
    private var oldRow: Int = 0
    private var oldOrd: Int = 0
    private var oldPosX: Int = 0
    private val rect = Rect()

    private var backGroundColor: Int = Color.BLACK

    init {
        backGroundColor = getBackgroundColor()

        notePaint.setARGB(255, 140, 140, 160)
        notePaint.typeface = Typeface.MONOSPACE
        notePaint.textSize = fontSize.toFloat()
        notePaint.isAntiAlias = true

        insPaint = Paint()
        insPaint.setARGB(255, 160, 80, 80)
        insPaint.typeface = Typeface.MONOSPACE
        insPaint.textSize = fontSize.toFloat()
        insPaint.isAntiAlias = true

        muteNotePaint = Paint()
        muteNotePaint.setARGB(255, 60, 60, 60)
        muteNotePaint.typeface = Typeface.MONOSPACE
        muteNotePaint.textSize = fontSize.toFloat()
        muteNotePaint.isAntiAlias = true

        muteInsPaint = Paint()
        muteInsPaint.setARGB(255, 80, 40, 40)
        muteInsPaint.typeface = Typeface.MONOSPACE
        muteInsPaint.textSize = fontSize.toFloat()
        muteInsPaint.isAntiAlias = true

        headerTextPaint = Paint()
        headerTextPaint.setARGB(255, 220, 220, 220)
        headerTextPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        headerTextPaint.textSize = fontSize.toFloat()
        headerTextPaint.isAntiAlias = true

        headerPaint = Paint()
        headerPaint.setARGB(255, 140, 140, 220)

        barPaint = Paint()
        barPaint.setARGB(255, 40, 40, 40)

        fontWidth = notePaint.measureText("X").toInt()
        fontHeight = fontSize * 12 / 10

        for (i in 0 until MAX_NOTES) {
            allNotes[i] = NOTES[i % 12] + i / 12
        }

        val c = CharArray(2)
        for (i in 0..255) {
            to02X(c, i)
            hexByte[i] = String(c)
        }
    }

    override fun setup(modPlayer: ModInterface, modVars: IntArray) {
        super.setup(modPlayer, modVars)

        oldRow = -1
        oldOrd = -1
        oldPosX = -1

        val chn = modVars[3]
        setMaxX((chn * 6 + 2) * fontWidth)
    }

    //@Override
    //public void setViewerRotation(final int val) {
    // 	super.setViewerRotation(val);
    //}

    override fun update(info: Info, paused: Boolean) {
        super.update(info, paused)

        val row = info.values[2]
        val ord = info.values[0]

        if (oldRow == row && oldOrd == ord && oldPosX == posX.toInt()) {
            return
        }

        val numRows = info.values[3]
        var canvas: Canvas? = null

        if (numRows != 0) {        // Skip first invalid infos
            oldRow = row
            oldOrd = ord
            oldPosX = posX.toInt()
        }

        try {
            canvas = surfaceHolder.lockCanvas(null)
            if (canvas != null) {
                synchronized(surfaceHolder) {
                    doDraw(canvas, modPlayer!!, info)
                }
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

    private fun doDraw(canvas: Canvas, modPlayer: ModInterface, info: Info) {
        val lines = canvasHeight / fontHeight
        val barLine = lines / 2 + 1
        val barY = barLine * fontHeight
        val row = info.values[2]
        val pat = info.values[1]
        val chn = modVars[3]
        val numRows = info.values[3]

        // Clear screen
        canvas.drawColor(backGroundColor)

        // Header
        rect.set(0, 0, canvasWidth - 1, fontHeight - 1)
        canvas.drawRect(rect, headerPaint)
        for (i in 0 until chn) {
            val adj = if (i + 1 < 10) 1 else 0
            val x = (3 + i * 6 + 1 + adj) * fontWidth - posX.toInt()
            if (x > -2 * fontWidth && x < canvasWidth) {
                canvas.drawText((i + 1).toString(), x.toFloat(), fontSize.toFloat(), headerTextPaint)
            }
        }

        // Current line bar
        rect.set(0, barY - fontHeight + 1, canvasWidth - 1, barY)
        canvas.drawRect(rect, barPaint)

        // Pattern data
        for (i in 1 until lines) {
            val lineInPattern = i + row - barLine + 1
            val y = (i + 1) * fontHeight
            var paint: Paint
            var paint2: Paint
            var x: Int

            if (lineInPattern < 0 || lineInPattern >= numRows) {
                continue
            }

            if (posX > -2 * fontWidth) {
                canvas.drawText(hexByte[lineInPattern]!!, -posX, y.toFloat(), headerTextPaint)
            }

            for (j in 0 until chn) {
                try {

                    // Be very careful here!
                    // Our variables are latency-compensated but pattern data is current
                    // so caution is needed to avoid retrieving data using old variables
                    // from a module with pattern data from a newly loaded one.

                    modPlayer.getPatternRow(pat, lineInPattern, rowNotes, rowInstruments)
                } catch (e: RemoteException) {
                    // fail silently
                }

                x = (3 + j * 6) * fontWidth - posX.toInt()

                if (x < -6 * fontWidth || x > canvasWidth) {
                    continue
                }

                if (isMuted[j]) {
                    paint = muteNotePaint
                    paint2 = muteInsPaint
                } else {
                    paint = notePaint
                    paint2 = insPaint
                }

                val note = rowNotes[j]
                when {
                    note < 0 -> canvas.drawText("===", x.toFloat(), y.toFloat(), paint)
                    note > MAX_NOTES -> canvas.drawText(">>>", x.toFloat(), y.toFloat(), paint)
                    note > 0 -> canvas.drawText(allNotes[note - 1]!!, x.toFloat(), y.toFloat(), paint)
                    else -> canvas.drawText("---", x.toFloat(), y.toFloat(), paint)
                }

                x = (3 + j * 6 + 3) * fontWidth - posX.toInt()
                if (rowInstruments[j] > 0) {
                    canvas.drawText(hexByte[rowInstruments[j].toInt()]!!, x.toFloat(), y.toFloat(), paint2)
                } else {
                    canvas.drawText("--", x.toFloat(), y.toFloat(), paint2)
                }
            }
        }
    }

    companion object {
        private val TAG = PatternViewer::class.java.simpleName

        private const val MAX_NOTES = 120

        private val NOTES = arrayOf("C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B ")
    }
}
