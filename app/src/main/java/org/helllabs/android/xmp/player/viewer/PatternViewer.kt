package org.helllabs.android.xmp.player.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.RemoteException
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.player.Util
import org.helllabs.android.xmp.player.Util.NOTES
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.logD

@SuppressLint("ViewConstructor")
class PatternViewer(context: Context, background: Int) : Viewer(context, background) {

    // TODO: Number row padding (Rows 100+ clips very close to patterns)
    // TODO: Find a way to accurately implement effects params...
    // private val muteEffectsPaint: Paint
    // private var effectsPaint: Paint
    // private var paint3: Paint = Paint()

    private val allNotes = mutableListOf<String>()
    private val c = CharArray(3)
    private val fontHeight: Int
    private val fontWidth: Int
    private val instHexByte = mutableListOf<String>()
    private val rect = Rect()
    private val rowInsts = ByteArray(64)
    private val rowNotes = ByteArray(64)
    private var hexByte = mutableListOf<String>()
    private var oldOrd = 0f
    private var oldPosX = 0f
    private var oldRow = 0f

    private val barPaint: Paint
    private val headerPaint: Paint
    private val headerTextPaint: Paint
    private val insPaint: Paint
    private val muteNotePaint: Paint
    private val notePaint: Paint
    private val numRowsTextPaint: Paint
    private var muteInsPaint: Paint
    private var paint1: Paint = Paint()
    private var paint2: Paint = Paint()

    // Draw Loop Variables
    private var adj: Float = 0f
    private var barLine: Int = 0
    private var barY: Int = 0
    private var chn: Int = 0
    private var headerX: Float = 0f
    private var lineInPattern: Int = 0
    private var lines: Int = 0
    private var numRows: Int = 0
    private var ord: Float = 0f
    private var pat: Int = 0
    private var patternX: Float = 0f
    private var patternY: Float = 0f
    private var row: Float = 0f
    private var updateRow: Float = 0f

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

        // Effects Paint
        // effectsPaint = Paint().apply {
        //     setARGB(255, 34, 158, 60) // Kinda digging the green.
        //     typeface = Typeface.MONOSPACE
        //     textSize = fontSize
        //     isAntiAlias = true
        // }

        // Muted Effects Paint
        // muteEffectsPaint = Paint().apply {
        //     setARGB(255, 16, 75, 28) // Darker shade of green
        //     typeface = Typeface.MONOSPACE
        //     textSize = fontSize
        //     isAntiAlias = true
        // }

        // Number Row Text Paint
        numRowsTextPaint = Paint().apply {
            setARGB(255, 200, 200, 200)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
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
            instHexByte.add(String(c))
        }
    }

    override fun setup(modPlayer: PlayerService, modVars: IntArray) {
        super.setup(modPlayer, modVars)
        logD("Viewer Setup")

        oldRow = -1f
        oldOrd = -1f
        oldPosX = -1f
        setMaxX((modVars[3] * 6 + 2) * fontWidth)
    }

    override fun update(info: Info?, paused: Boolean) {
        super.update(info, paused)

        updateRow = info!!.values[2].toFloat()
        ord = info.values[0].toFloat()

        if (oldRow == updateRow && oldOrd == ord && oldPosX == posX) {
            return
        }

        //  NumRows
        if (info.values[3] != 0) {
            // Skip first invalid infos
            oldRow = row
            oldOrd = ord
            oldPosX = posX
        }

        requestCanvasLock { canvas ->
            doDraw(canvas, modPlayer, info)
        }
    }

    private fun doDraw(canvas: Canvas, modPlayer: PlayerService, info: Info?) {

        lines = canvasHeight / fontHeight
        barLine = lines / 2 + 1
        barY = barLine * fontHeight
        row = info!!.values[2].toFloat()
        pat = info.values[1]
        chn = modVars[3]
        numRows = info.values[3]

        // Get the number of rows dynamically
        // Side effect of https://github.com/cmatsuoka/xmp-android/pull/15
        if (numRows > 0 && hexByte.size != numRows) {
            resizeRows()
        }

        // Clear screen
        canvas.drawColor(bgColor)

        // Header
        rect[0, 0, canvasWidth - 1] = fontHeight - 1
        canvas.drawRect(rect, headerPaint)
        for (i in 0 until chn) {
            adj = if (i + 1 < 10) 1f else 0f
            headerX = (3 + i * 6 + 1 + adj) * fontWidth - posX.toInt()
            if (headerX > -2 * fontWidth && headerX < canvasWidth) {
                canvas.drawText((i + 1).toString(), headerX, fontSize, headerTextPaint)
            }
        }

        // Current line bar
        rect.set(0, barY - fontHeight + 10, canvasWidth, barY + 10)
        canvas.drawRect(rect, barPaint)

        // Pattern data
        for (i in 1 until lines) {
            lineInPattern = (i + row - barLine + 1).toInt()
            patternY = ((i + 1) * fontHeight).toFloat()

            if (lineInPattern < 0 || lineInPattern >= numRows) {
                continue
            }

            // Row Number
            if (posX > -2 * fontWidth) {
                canvas.drawText(lineInPattern.toString(), -posX, patternY, numRowsTextPaint)
            }

            for (j in 0 until chn) {
                try {

                    // Be very careful here!
                    // Our variables are latency-compensated but pattern data is current
                    // so caution is needed to avoid retrieving data using old variables
                    // from a module with pattern data from a newly loaded one.
                    modPlayer.getPatternRow(pat, lineInPattern, rowNotes, rowInsts)
                } catch (e: RemoteException) {
                    // fail silently
                }

                // is muted paint
                if (isMuted[j]) {
                    paint1 = muteNotePaint
                    paint2 = muteInsPaint
                    // paint3 = muteEffectsPaint
                } else {
                    paint1 = notePaint
                    paint2 = insPaint
                    // paint3 = effectsPaint
                }

                patternX = (3 + j * 6) * fontWidth - posX
                if (patternX < -6 * fontWidth || patternX > canvasWidth) {
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
                canvas.drawText(notes, patternX, patternY, paint1)

                // Instruments
                patternX = (3 + j * 6 + 3) * fontWidth - posX
                val inst = if (rowInsts[j] > 0) instHexByte[rowInsts[j].toInt()] else "--"
                canvas.drawText(inst, patternX, patternY, paint2)
            }
        }
    }

    private fun resizeRows() {
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
