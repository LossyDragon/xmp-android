package org.helllabs.android.xmp.player.viewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.os.RemoteException
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import kotlin.math.abs
import org.helllabs.android.xmp.service.ModInterface
import org.helllabs.android.xmp.util.*

// http://developer.android.com/guide/topics/graphics/2d-graphics.html
// http://www.independent-software.com/android-speeding-up-canvas-drawbitmap.html
// https://androidpedia.net/en/tutorial/3754/canvas-drawing-using-surfaceview
// https://stackoverflow.com/a/8289516
abstract class Viewer(context: Context) :
    SurfaceView(context),
    SurfaceHolder.Callback,
    View.OnClickListener {

    private var canvas: Canvas? = null
    private lateinit var gestureDetector: GestureDetector

    internal var surfaceHolder: SurfaceHolder
    internal var rotation: Int = 0
    internal var modPlayer: ModInterface? = null
    internal var canvasWidth = 0
    internal var canvasHeight = 0
    internal val screenSize: Int
    internal lateinit var modVars: IntArray
    internal lateinit var isMuted: BooleanArray

    // Touch tracking
    internal var posX: Float
    internal var posY: Float
    internal var velX = 0f
    internal var velY = 0f
    private var maxX = 0
    private var maxY = 0

    class Info {
        var time = 0
        val values = IntArray(7) // order pattern row num_rows frame speed bpm
        val volumes = IntArray(64)
        val finalvols = IntArray(64)
        val pans = IntArray(64)
        val instruments = IntArray(64)
        val keys = IntArray(64)
        val periods = IntArray(64)
    }

    init {
        // register our interest in hearing about changes to our surface
        @Suppress("LeakingThis") // Its because it's in the init block.
        holder.addCallback(this)

        surfaceHolder = holder

        posY = 0f
        posX = posY

        initGestureListener()
        screenSize = resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initGestureListener() {
        // Gesture detection
        gestureDetector = GestureDetector(context, MyGestureDetector())
        val gestureListener = OnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
        setOnClickListener(this@Viewer)
        setOnTouchListener(gestureListener)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        setSurfaceSize(width, height)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        logD("Creating Surface")
        surfaceHolder = holder
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        logD("Destroying Surface")
        surfaceHolder.surface.release()
    }

    override fun onClick(view: View) {
        // do nothing
    }

    protected open fun onClick(x: Int, y: Int) {
        val parent: View = parent as View
        parent.performClick()
    }

    protected open fun onLongClick(x: Int, y: Int) {
        // do nothing until overridden
    }

    open fun setRotation(value: Int) {
        rotation = value
    }

    open fun update(info: Info?, paused: Boolean) {
        updateScroll()
    }

    open fun setup(modPlayer: ModInterface, modVars: IntArray) {
        logI("Viewer setup")
        val chn = modVars[3]
        this.modVars = modVars
        this.modPlayer = modPlayer
        isMuted = BooleanArray(chn)

        for (i in 0 until chn) {
            try {
                isMuted[i] = modPlayer.mute(i, -1) == 1
            } catch (e: RemoteException) {
                logE("Can't read channel mute status")
            }
        }

        posY = 0f
        posX = posY
    }

    private fun limitPosition() {
        if (posX > maxX - canvasWidth)
            posX = (maxX - canvasWidth).toFloat()

        if (posX < 0)
            posX = 0f

        if (posY > maxY - canvasHeight)
            posY = (maxY - canvasHeight).toFloat()

        if (posY < 0)
            posY = 0f
    }

    // Hmpf, reinventing the wheel instead of using Scroller
    private fun updateScroll() {
        posX -= velX
        posY -= velY
        limitPosition()

        velX *= 0.9f
        if (abs(velX) < 0.5)
            velX = 0f

        velY *= 0.9f
        if (abs(velY) < 0.5)
            velY = 0f
    }

    /* Callback invoked when the surface dimensions change. */
    private fun setSurfaceSize(width: Int, height: Int) {
        // synchronized to make sure these all change atomically
        synchronized(surfaceHolder) {
            canvasWidth = width
            canvasHeight = height
        }
    }

    fun setMaxX(x: Int) {
        synchronized(this) { maxX = x }
    }

    fun setMaxY(y: Int) {
        synchronized(this) { maxY = y }
    }

    fun requestCanvasLock(draw: (canvas: Canvas) -> Unit) {
        // Check if the surface is valid. Stops it from being silly.
        if (surfaceHolder.surface.isValid) {
            try {
                canvas = if (isAtLeastO) {
                    try {
                        // Try and use hardware-accelerated canvas
                        surfaceHolder.lockHardwareCanvas()
                    } catch (e: IllegalStateException) {
                        // The device must not support hardware-accelerated canvas.
                        surfaceHolder.lockCanvas()
                    }
                } else {
                    surfaceHolder.lockCanvas()
                }

                draw(canvas!!)
            } catch (e: Exception) {
                logE("Canvas error: $e")
            } finally {
                // do this in a finally so that if an exception is thrown
                // during the above, we don't leave the Surface in an
                // inconsistent state
                if (canvas != null)
                    surfaceHolder.unlockCanvasAndPost(canvas)
            }
        } else {
            logW("Surface not valid")
        }
    }

    private inner class MyGestureDetector : SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            synchronized(this) {
                posX += distanceX
                posY += distanceY
                limitPosition()
                velY = 0f
                velX = velY
            }
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            // Register a fling after a certain velocity.
            if (velocityX >= FLING || velocityY >= FLING) {
                synchronized(this) {
                    velX = velocityX / 25
                    velY = velocityY / 25
                }
                return true
            }
            return false
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClick(e.x.toInt(), e.y.toInt())
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            onLongClick(e.x.toInt(), e.y.toInt())
        }

        override fun onDown(e: MotionEvent): Boolean {
            velY = 0f
            velX = velY // stop fling
            return true
        }
    }

    companion object {
        private const val FLING = 1000 // arbitrary
    }
}
