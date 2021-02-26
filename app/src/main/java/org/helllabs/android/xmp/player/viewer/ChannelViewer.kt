package org.helllabs.android.xmp.player.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.os.RemoteException
import android.view.Surface
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.player.Util
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.logE

@SuppressLint("ViewConstructor")
class ChannelViewer(context: Context, background: Int) : Viewer(context, background) {

    private lateinit var channelNumber: Array<String?>
    private lateinit var holdKey: IntArray
    private lateinit var insNameTrim: Array<String?>
    private val buffer: Array<ByteArray> // keep several buffers to hold data in pause
    private val bufferXY: FloatArray
    private val font2Height: Int
    private val font2Width: Int
    private val fontHeight: Int
    private val fontSize: Int = resources.getDimensionPixelSize(R.dimen.channelview_font_size)
    private val fontWidth: Int
    private val insPaint: Paint
    private val keyRow = IntArray(Xmp.MAX_CHANNELS)
    private val meterPaint: Paint
    private val numPaint: Paint
    private val rect = Rect()
    private val scopeHeight: Int
    private val scopeLeft: Int
    private val scopeLinePaint: Paint
    private val scopeMutePaint: Paint
    private val scopeMuteTextPaint: Paint
    private val scopePaint: Paint
    private val scopeWidth: Int
    private val volLeft: Int
    private var cols = 1
    private var insName = mutableListOf<String>()
    private var panLeft = 0
    private var panWidth = 0
    private var volWidth = 0

    // Better waveform
    private val useNewWaveform = PrefManager.useNewWaveform
    private val waveformPath = Path()

    // Draw Loop Variables
    private var numChannels: Int = 0
    private var numInstruments: Int = 0
    private var row: Int = 0
    private var num: Int = 0
    private var icol: Int = 0
    private var drawX: Int = 0
    private var drawY: Int = 0
    private var ins: Int = 0
    private var vol: Int = 0
    private var finalVol: Int = 0
    private var pan: Int = 0
    private var key: Int = 0
    private var period: Int = 0
    private var h: Int = 0
    private var volX: Int = 0
    private var volY1: Int = 0
    private var volY2: Int = 0
    private var panX: Int = 0

    init {
        val font2Size = resources.getDimensionPixelSize(R.dimen.channelview_channel_font_size)

        // Scope and Meter Background Paint
        scopePaint = Paint().apply {
            setARGB(255, 40, 40, 40)
        }

        // Scope Waveform Paint
        scopeLinePaint = Paint().apply {
            setARGB(255, 80, 160, 80)
            strokeWidth = 2.0f // Thicken the scope line up.
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        // Mute Background Paint
        scopeMutePaint = Paint().apply {
            setARGB(255, 60, 0, 0)
        }

        // Mute Text Paint
        scopeMuteTextPaint = Paint().apply {
            setARGB(255, 220, 220, 220)
            typeface = Typeface.MONOSPACE
            textSize = fontSize.toFloat()
            isAntiAlias = true
            isLinearText = true
        }

        // Meter Paint
        meterPaint = Paint().apply {
            setARGB(255, 40, 80, 160)
        }

        // Channel Instrument Paint
        insPaint = Paint().apply {
            setARGB(255, 200, 200, 200)
            typeface = Typeface.MONOSPACE
            textSize = fontSize.toFloat()
            isAntiAlias = true
            isLinearText = true
        }

        // Row Number Paint
        numPaint = Paint().apply {
            setARGB(255, 200, 200, 200)
            typeface = Typeface.MONOSPACE
            textSize = font2Size.toFloat()
            isAntiAlias = true
            isLinearText = true
        }

        fontWidth = insPaint.measureText("X").toInt()
        fontHeight = fontSize * 12 / 10
        font2Width = numPaint.measureText("X").toInt()
        font2Height = font2Size * 12 / 10
        scopeWidth = 8 * fontWidth
        scopeHeight = 3 * fontHeight
        scopeLeft = 2 * font2Width + 2 * fontWidth
        volLeft = scopeLeft + scopeWidth + fontWidth * 2
        buffer = Array(Xmp.MAX_CHANNELS) { ByteArray(scopeWidth) }
        bufferXY = FloatArray(scopeWidth * 2)
    }

    override fun setup(modPlayer: PlayerService, modVars: IntArray) {
        super.setup(modPlayer, modVars)

        val chn = modVars[3]
        val ins = modVars[4]

        this.modPlayer = modPlayer
        try {
            insName = modPlayer.getInstruments().toMutableList()
        } catch (e: RemoteException) {
            logE("Can't get instrument name")
        }

        if (insName.isNullOrEmpty()) {
            for (i in 0 until ins) {
                insName.add("")
            }
        }

        holdKey = IntArray(chn)
        channelNumber = arrayOfNulls(chn)

        // This is much faster than String.format
        val c = CharArray(2)
        for (i in 0 until chn) {
            Util.to2d(c, i + 1)
            channelNumber[i] = String(c)
        }
    }

    override fun update(info: Info?, paused: Boolean) {
        super.update(info, paused)

        requestCanvasLock { canvas ->
            doDraw(canvas, modPlayer, info, paused)
        }
    }

    private fun findScope(x: Int, y: Int): Int {
        val chn = modVars[3]
        val scopeWidth = 8 * fontWidth
        var scopeLeft = 2 * font2Width + 2 * fontWidth

        if (x >= scopeLeft && x <= scopeLeft + scopeWidth) {
            var scopeNum = (y + posY.toInt() - fontHeight) / (4 * fontHeight)
            if (cols > 1) {
                if (scopeNum >= (chn + 1) / cols) {
                    scopeNum = -1
                }
            } else {
                if (scopeNum >= chn) {
                    scopeNum = -1
                }
            }
            return scopeNum
        } else if (cols <= 1) {
            return -1
        }

        // Two column layout
        scopeLeft += canvasWidth / cols
        return if (x >= scopeLeft && x <= scopeLeft + scopeWidth) {
            var scopeNum = (y + posY.toInt() - fontHeight) / (4 * fontHeight) + (chn + 1) / cols
            if (scopeNum >= chn) {
                scopeNum = -1
            }
            scopeNum
        } else {
            -1
        }
    }

    public override fun onClick(x: Int, y: Int) {
        // Check if clicked on scopes
        val n = findScope(x, y)
        if (n >= 0) {
            try {
                modPlayer.mute(n, if (isMuted[n]) 0 else 1)
                isMuted[n] = isMuted[n] xor true
            } catch (e: RemoteException) {
                logE("Can't mute channel $n")
            }
        } else {
            super.onClick(x, y)
        }
    }

    public override fun onLongClick(x: Int, y: Int) {
        val chn = modVars[3]

        // Check if clicked on scopes
        val n = findScope(x, y)

        // If the channel is solo, a long press unmute all channels,
        // otherwise solo this channel
        if (n >= 0) {
            var count = 0
            for (i in 0 until chn) {
                if (!isMuted[i]) {
                    count++
                }
            }
            if (count == 1 && !isMuted[n]) {
                try {
                    for (i in 0 until chn) {
                        modPlayer.mute(i, 0)
                        isMuted[i] = false
                    }
                } catch (e: RemoteException) {
                    logE("Can't mute channels")
                }
            } else {
                try {
                    for (i in 0 until chn) {
                        modPlayer.mute(i, if (i != n) 1 else 0)
                        isMuted[i] = i != n
                    }
                } catch (e: RemoteException) {
                    logE("Can't unmute channel $n")
                }
            }
        } else {
            super.onLongClick(x, y)
        }
    }

    override fun setRotation(value: Int) {
        super.setRotation(value)

        // Should use canvasWidth but it's not updated yet
        val width = context.resources.displayMetrics.widthPixels
        cols = when (screenSize) {
            Configuration.SCREENLAYOUT_SIZE_NORMAL ->
                if (width < 800) 1
                else if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) 1
                else 2
            Configuration.SCREENLAYOUT_SIZE_LARGE ->
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) 1
                else 2
            Configuration.SCREENLAYOUT_SIZE_XLARGE -> 2
            else -> 1
        }

        val chn = modVars[3]
        if (cols == 1) {
            setMaxY((chn * 4 + 1) * fontHeight)
        } else {
            setMaxY(((chn + 1) / cols * 4 + 1) * fontHeight)
        }

        volWidth = (width / cols - 5 * fontWidth - volLeft) / 2
        panLeft = volLeft + volWidth + 3 * fontWidth
        panWidth = volWidth
        val textWidth = 2 * volWidth / fontWidth + 3
        val num = insName.size
        insNameTrim = arrayOfNulls(num)

        for (i in 0 until num) {
            if (insName[i].length > textWidth) {
                insNameTrim[i] = insName[i].substring(0, textWidth)
            } else {
                insNameTrim[i] = insName[i]
            }
        }
    }

    private fun doDraw(canvas: Canvas, modPlayer: PlayerService, info: Info?, paused: Boolean) {
        numChannels = modVars[3]
        numInstruments = modVars[4]
        row = info!!.values[2]

        // Clear screen
        canvas.drawColor(bgColor)

        for (chn in 0 until numChannels) {
            num = (numChannels + 1) / cols
            icol = chn % num
            drawX = chn / num * canvasWidth / 2
            drawY = (icol * 4 + 1) * fontHeight - posY.toInt()
            ins = if (isMuted[chn]) -1 else info.instruments[chn]
            vol = if (isMuted[chn]) 0 else info.volumes[chn]
            finalVol = info.finalVols[chn]
            pan = info.pans[chn]
            key = info.keys[chn]
            period = info.periods[chn]

            if (key >= 0) {
                holdKey[chn] = key
                if (keyRow[chn] == row) {
                    key = -1
                } else {
                    keyRow[chn] = row
                }
            }

            // Don't draw if not visible
            if (drawY < -scopeHeight || drawY > canvasHeight) {
                continue
            }

            // Draw channel number
            canvas.drawText(
                channelNumber[chn]!!,
                drawX.toFloat(),
                drawY + (scopeHeight + font2Height) / 2.toFloat(),
                numPaint
            )

            // Draw scopes
            rect[drawX + scopeLeft, drawY + 1, drawX + scopeLeft + scopeWidth] = drawY + scopeHeight
            if (isMuted[chn]) {
                canvas.drawRect(rect, scopeMutePaint)
                canvas.drawText(
                    "MUTE",
                    (drawX + scopeLeft + 2 * fontWidth).toFloat(),
                    (drawY + fontHeight + fontSize).toFloat(),
                    scopeMuteTextPaint
                )
            } else {
                canvas.drawRect(rect, scopePaint)
                if (!paused) {
                    try {

                        // Be very careful here!
                        // Our variables are latency-compensated but sample data is current
                        // so caution is needed to avoid retrieving data using old variables
                        // from a module with sample data from a newly loaded one.
                        modPlayer.getSampleData(
                            key >= 0,
                            ins,
                            holdKey[chn],
                            period,
                            chn,
                            scopeWidth,
                            buffer[chn]
                        )
                    } catch (e: RemoteException) {
                        // fail silently
                    }
                }

                h = scopeHeight / 2

                if (useNewWaveform) {
                    // New scope lines.
                    var isFirst = true
                    for (j in 0 until scopeWidth) {
                        if (isFirst) {
                            isFirst = false
                            waveformPath.moveTo(
                                (drawX + scopeLeft + j).toFloat(),
                                (drawY + h + buffer[chn][j] * h * finalVol / (48 * 180)).toFloat()
                            )
                        } else {
                            waveformPath.lineTo(
                                (drawX + scopeLeft + j).toFloat(),
                                (drawY + h + buffer[chn][j] * h * finalVol / (48 * 180)).toFloat()
                            )
                        }
                    }
                    canvas.drawPath(waveformPath, scopeLinePaint)
                    waveformPath.reset()
                } else {
                    // Claudio's OG scope lines.
                    for (j in 0 until scopeWidth) {
                        bufferXY[j * 2] = (drawX + scopeLeft + j).toFloat()
                        bufferXY[j * 2 + 1] =
                            (drawY + h + buffer[chn][j] * h * finalVol / (48 * 180)).toFloat()
                    }

                    // [Old] Doubled
                    // canvas.drawLines(bufferXY, 0, scopeWidth shl 1, scopeLinePaint)
                    // canvas.drawLines(bufferXY, 2, (scopeWidth shl 1) - 2, scopeLinePaint)

                    // Using drawPoints() instead of drawing each point saves a lot of CPU
                    canvas.drawPoints(bufferXY, 0, scopeWidth shl 1, scopeLinePaint)
                }
            }

            // Draw instrument name
            if (ins in 0 until numInstruments) {
                canvas.drawText(
                    insNameTrim[ins]!!,
                    drawX + volLeft.toFloat(),
                    drawY + fontHeight.toFloat(),
                    insPaint
                )
            }

            // Draw volumes
            volX = volLeft + vol * volWidth / 0x40
            volY1 = drawY + 2 * fontHeight
            volY2 = drawY + 2 * fontHeight + fontHeight / 3
            rect[drawX + volLeft, volY1, drawX + volX] = volY2
            canvas.drawRect(rect, meterPaint)
            rect[drawX + volX + 1, volY1, drawX + volLeft + volWidth] = volY2
            canvas.drawRect(rect, scopePaint)

            // Draw pan
            if (ins < 0) {
                pan = 0x80
            }

            panX = panLeft + pan * panWidth / 0x100
            rect[drawX + panLeft, volY1, drawX + panLeft + panWidth] = volY2
            canvas.drawRect(rect, scopePaint)
            rect[drawX + panX, volY1, drawX + panX + fontWidth / 2] = volY2
            canvas.drawRect(rect, meterPaint)
        }
    }
}
