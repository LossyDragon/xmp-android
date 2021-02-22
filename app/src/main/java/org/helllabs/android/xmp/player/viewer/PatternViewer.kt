package org.helllabs.android.xmp.player.viewer

import android.content.Context
import android.graphics.*
import android.os.RemoteException
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.player.Util
import org.helllabs.android.xmp.player.Util.NOTES
import org.helllabs.android.xmp.service.ModInterface
import org.helllabs.android.xmp.util.logD

class PatternViewer(context: Context) : Viewer(context) {

    private var oldRow = 0
    private var oldPosX = 0
    private var oldOrd = 0

    private val c = CharArray(3)
    private val rowNotes = ByteArray(64)
    private val rowInstruments = ByteArray(64)
    private val rect = Rect()
    private val notePaint: Paint
    private val muteNotePaint: Paint
    private val muteInsPaint: Paint
    private val insPaint: Paint
    private val hexByte = mutableListOf<String>()
    private val headerTextPaint: Paint
    private val headerPaint: Paint
    private val fontWidth: Int
    private val fontHeight: Int
    private val barPaint: Paint
    private val allNotes = mutableListOf<String>()

    private val fontSize: Float =
        resources.getDimensionPixelSize(R.dimen.patternview_font_size).toFloat()

    init {

        // Note Paint
        notePaint = Paint().apply {
            setARGB(255, 140, 140, 160)
            typeface = Typeface.MONOSPACE
            textSize = fontSize
            isAntiAlias = true
        }

        // Muted Note Paint
        muteNotePaint = Paint().apply {
            setARGB(255, 60, 60, 60)
            typeface = Typeface.MONOSPACE
            textSize = fontSize
            isAntiAlias = true
        }

        // Instrument Paint
        insPaint = Paint().apply {
            setARGB(255, 160, 80, 80)
            typeface = Typeface.MONOSPACE
            textSize = fontSize
            isAntiAlias = true
        }

        // Muted Instrument Paint
        muteInsPaint = Paint().apply {
            setARGB(255, 80, 40, 40)
            typeface = Typeface.MONOSPACE
            textSize = fontSize
            isAntiAlias = true
        }

        // Header Text Paint
        headerTextPaint = Paint().apply {
            setARGB(255, 220, 220, 220)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = fontSize
            isAntiAlias = true
        }

        // Header Paint
        headerPaint = Paint().apply { setARGB(255, 140, 140, 220) }

        // Bar Paint
        barPaint = Paint().apply { setARGB(255, 40, 40, 40) }

        fontWidth = notePaint.measureText("X").toInt()
        fontHeight = (fontSize * 12 / 10).toInt()

        for (i in 0 until MAX_NOTES) {
            allNotes.add(NOTES[i % 12] + i / 12)
        }

        for (i in 0..255) {
            Util.to02X(c, i)
            hexByte.add(String(c))
        }
    }

    override fun setup(modPlayer: ModInterface, modVars: IntArray) {
        super.setup(modPlayer, modVars)
        logD("Viewer Setup")

        oldRow = -1
        oldOrd = -1
        oldPosX = -1
        setMaxX((modVars[3] * 6 + 2) * fontWidth)
    }

    override fun update(info: Info?, paused: Boolean) {
        super.update(info, paused)

        val row = info!!.values[2]
        val ord = info.values[0]

        if (oldRow == row && oldOrd == ord && oldPosX == posX.toInt()) {
            return
        }

        if (info.values[3] != 0) {
            // Skip first invalid infos
            oldRow = row
            oldOrd = ord
            oldPosX = posX.toInt()
        }

        requestCanvasLock { canvas ->
            doDraw(canvas, modPlayer!!, info)
        }
    }

    private fun doDraw(canvas: Canvas, modPlayer: ModInterface, info: Info?) {

        val lines = canvasHeight / fontHeight
        val barLine = lines / 2 + 1
        val barY = barLine * fontHeight
        val row = info!!.values[2]
        val pat = info.values[1]
        val chn = modVars[3]
        val numRows = info.values[3]

        // Get the number of rows dynamically
        // Side effect of https://github.com/cmatsuoka/xmp-android/pull/15
        if (numRows > 0 && hexByte.size != numRows) {
            resizeRows(numRows)
        }

        // Clear screen
        canvas.drawColor(Color.BLACK)

        // Header
        rect[0, 0, canvasWidth - 1] = fontHeight - 1
        canvas.drawRect(rect, headerPaint)
        for (i in 0 until chn) {
            val adj = if (i + 1 < 10) 1 else 0
            val x = (3 + i * 6 + 1 + adj) * fontWidth - posX.toInt()
            if (x > -2 * fontWidth && x < canvasWidth) {
                canvas.drawText(
                    (i + 1).toString(),
                    x.toFloat(),
                    fontSize,
                    headerTextPaint
                )
            }
        }

        // Current line bar
        rect.set(0, barY - fontHeight + 10, canvasWidth, barY + 10)
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
                canvas.drawText(hexByte[lineInPattern], -posX, y.toFloat(), headerTextPaint)
            }

            for (j in 0 until chn) {
                try {

                    // Be very careful here!
                    // Our variables are latency-compensated but pattern data is current
                    // so caution is needed to avoid retrieving data using old variables
                    // from a module with pattern data from a newly loaded one.
                    modPlayer.getPatternRow(pat, lineInPattern, rowNotes, rowInstruments)
                } catch (e: RemoteException) {
                    // fail silenty
                }

                // is muted paint
                if (isMuted[j]) {
                    paint = muteNotePaint
                    paint2 = muteInsPaint
                } else {
                    paint = notePaint
                    paint2 = insPaint
                }

                x = (3 + j * 6) * fontWidth - posX.toInt()
                if (x < -6 * fontWidth || x > canvasWidth) {
                    continue
                }

                // Notes
                val note = rowNotes[j]
                val notes = when {
                    note > MAX_NOTES -> ">>>"
                    note > 0 -> allNotes[note - 1]
                    note < 0 -> "==="
                    else -> "---"
                }
                canvas.drawText(notes, x.toFloat(), y.toFloat(), paint)

                // Instruments
                x = (3 + j * 6 + 3) * fontWidth - posX.toInt()
                val inst = if (rowInstruments[j] > 0) hexByte[rowInstruments[j].toInt()] else "--"
                canvas.drawText(inst, x.toFloat(), y.toFloat(), paint2)
            }
        }
    }

    private fun resizeRows(numRows: Int) {
        logD("Resizing numRows: $numRows")
        hexByte.clear()
        for (i in 0 until numRows) {
            if (i <= 255) {
                Util.to02X(c, i)
            } else {
                Util.to03X(c, i)
            }
            hexByte.add(String(c))
        }
    }

    companion object {
        private const val MAX_NOTES = 120
    }
}
