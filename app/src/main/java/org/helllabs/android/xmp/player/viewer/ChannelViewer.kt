package org.helllabs.android.xmp.player.viewer

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.os.RemoteException
import android.view.Surface
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.player.Util
import org.helllabs.android.xmp.service.ModInterface
import org.helllabs.android.xmp.util.logE

class ChannelViewer(context: Context) : Viewer(context) {

    private val scopePaint: Paint
    private val scopeLinePaint: Paint
    private val insPaint: Paint
    private val meterPaint: Paint
    private val numPaint: Paint
    private val scopeMutePaint: Paint
    private val fontSize: Int = resources.getDimensionPixelSize(R.dimen.channelview_font_size)
    private val fontHeight: Int
    private val fontWidth: Int
    private val font2Height: Int
    private val font2Width: Int
    private var insName: Array<String?>? = null
    private lateinit var insNameTrim: Array<String?>
    private val rect = Rect()
    private val buffer: Array<ByteArray> // keep several buffers to hold data in pause
    private val bufferXY: FloatArray
    private lateinit var holdKey: IntArray
    private lateinit var channelNumber: Array<String?>
    private var cols = 1
    private val scopeWidth: Int
    private val scopeHeight: Int
    private val scopeLeft: Int
    private val volLeft: Int
    private var volWidth = 0
    private var panLeft = 0
    private var panWidth = 0
    private val keyRow = IntArray(Xmp.MAX_CHANNELS)

    init {
        val font2Size = resources.getDimensionPixelSize(R.dimen.channelview_channel_font_size)
        scopePaint = Paint()
        scopePaint.setARGB(255, 40, 40, 40)
        scopeLinePaint = Paint()
        scopeLinePaint.setARGB(255, 80, 160, 80)
        scopeLinePaint.strokeWidth = 0f
        scopeLinePaint.isAntiAlias = false
        scopeMutePaint = Paint()
        scopeMutePaint.setARGB(255, 60, 0, 0)
        meterPaint = Paint()
        meterPaint.setARGB(255, 40, 80, 160)
        insPaint = Paint()
        insPaint.setARGB(255, 140, 140, 160)
        insPaint.typeface = Typeface.MONOSPACE
        insPaint.textSize = fontSize.toFloat()
        insPaint.isAntiAlias = true
        numPaint = Paint()
        numPaint.setARGB(255, 220, 220, 220)
        numPaint.typeface = Typeface.MONOSPACE
        numPaint.textSize = font2Size.toFloat()
        numPaint.isAntiAlias = true
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

    override fun setup(modPlayer: ModInterface, modVars: IntArray) {
        super.setup(modPlayer, modVars)
        val chn = modVars[3]
        val ins = modVars[4]
        this.modPlayer = modPlayer
        try {
            insName = modPlayer.instruments
        } catch (e: RemoteException) {
            logE("Can't get instrument name")
        }
        if (insName == null) {
            insName = arrayOfNulls(ins)
            for (i in 0 until ins) {
                insName!![i] = ""
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
        var canvas: Canvas? = null
        try {
            canvas = surfaceHolder.lockCanvas(null)
            if (canvas != null) {
                synchronized(surfaceHolder) { doDraw(canvas, modPlayer!!, info, paused) }
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
                modPlayer!!.mute(n, if (isMuted[n]) 0 else 1)
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
                        modPlayer!!.mute(i, 0)
                        isMuted[i] = false
                    }
                } catch (e: RemoteException) {
                    logE("Can't mute channels")
                }
            } else {
                try {
                    for (i in 0 until chn) {
                        modPlayer!!.mute(i, if (i != n) 1 else 0)
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
        when (screenSize) {
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> {
                if (width < 800) {
                    cols = 1
                }
                cols = if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                    1
                } else {
                    2
                }
            }
            Configuration.SCREENLAYOUT_SIZE_LARGE ->
                cols = if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                    1
                } else {
                    2
                }
            Configuration.SCREENLAYOUT_SIZE_XLARGE -> cols = 2
            else -> cols = 1
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
        val num = insName!!.size
        insNameTrim = arrayOfNulls(num)
        for (i in 0 until num) {
            if (insName!![i]!!.length > textWidth) {
                insNameTrim[i] = insName!![i]!!.substring(0, textWidth)
            } else {
                insNameTrim[i] = insName!![i]
            }
        }
    }

    private fun doDraw(canvas: Canvas, modPlayer: ModInterface, info: Info?, paused: Boolean) {
        val numChannels = modVars[3]
        val numInstruments = modVars[4]
        val row = info!!.values[2]

        // Clear screen
        canvas.drawColor(Color.BLACK)
        for (chn in 0 until numChannels) {
            val num = (numChannels + 1) / cols
            val icol = chn % num
            val x = chn / num * canvasWidth / 2
            val y = (icol * 4 + 1) * fontHeight - posY.toInt()
            val ins = if (isMuted[chn]) -1 else info.instruments[chn]
            val vol = if (isMuted[chn]) 0 else info.volumes[chn]
            val finalvol = info.finalvols[chn]
            var pan = info.pans[chn]
            var key = info.keys[chn]
            val period = info.periods[chn]
            if (key >= 0) {
                holdKey[chn] = key
                if (keyRow[chn] == row) {
                    key = -1
                } else {
                    keyRow[chn] = row
                }
            }

            // Don't draw if not visible
            if (y < -scopeHeight || y > canvasHeight) {
                continue
            }

            // Draw channel number
            canvas.drawText(
                channelNumber[chn]!!,
                x.toFloat(),
                (y + (scopeHeight + font2Height) / 2).toFloat(),
                numPaint
            )

            // Draw scopes
            rect[x + scopeLeft, y + 1, x + scopeLeft + scopeWidth] = y + scopeHeight
            if (isMuted[chn]) {
                canvas.drawRect(rect, scopeMutePaint)
                canvas.drawText(
                    "MUTE",
                    (x + scopeLeft + 2 * fontWidth).toFloat(),
                    (y + fontHeight + fontSize).toFloat(),
                    insPaint
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
                val h = scopeHeight / 2
                for (j in 0 until scopeWidth) {
                    bufferXY[j * 2] =
                        (x + scopeLeft + j).toFloat()
                    bufferXY[j * 2 + 1] =
                        (y + h + buffer[chn][j] * h * finalvol / (64 * 180)).toFloat()
                }

                // Using drawPoints() instead of drawing each point saves a lot of CPU
                canvas.drawPoints(bufferXY, 0, scopeWidth shl 1, scopeLinePaint)
            }

            // Draw instrument name
            if (ins in 0 until numInstruments) {
                canvas.drawText(
                    insNameTrim[ins]!!,
                    (x + volLeft).toFloat(),
                    (y + fontHeight).toFloat(),
                    insPaint
                )
            }

            // Draw volumes
            val volX = volLeft + vol * volWidth / 0x40
            val volY1 = y + 2 * fontHeight
            val volY2 = y + 2 * fontHeight + fontHeight / 3
            rect[x + volLeft, volY1, x + volX] = volY2
            canvas.drawRect(rect, meterPaint)
            rect[x + volX + 1, volY1, x + volLeft + volWidth] = volY2
            canvas.drawRect(rect, scopePaint)

            // Draw pan
            if (ins < 0) {
                pan = 0x80
            }
            val panX = panLeft + pan * panWidth / 0x100
            rect[x + panLeft, volY1, x + panLeft + panWidth] = volY2
            canvas.drawRect(rect, scopePaint)
            rect[x + panX, volY1, x + panX + fontWidth / 2] = volY2
            canvas.drawRect(rect, meterPaint)
        }
    }
}
