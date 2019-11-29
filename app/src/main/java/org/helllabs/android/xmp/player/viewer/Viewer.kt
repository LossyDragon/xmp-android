package org.helllabs.android.xmp.player.viewer

import org.helllabs.android.xmp.service.ModInterface
import org.helllabs.android.xmp.util.Log

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.RemoteException
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import org.helllabs.android.xmp.player.getScreenSize
import kotlin.math.abs


abstract class Viewer(context: Context) : SurfaceView(context), SurfaceHolder.Callback, View.OnClickListener {
    protected lateinit var surfaceHolder: SurfaceHolder
    protected var canvasHeight: Int = 0
    protected var canvasWidth: Int = 0
    protected lateinit var modVars: IntArray
    protected open var modPlayer: ModInterface? = null
    protected lateinit var isMuted: BooleanArray
    protected var viewerRotation: Int = 0
    protected val screenSize: Int
    private val gestureDetector: GestureDetector

    // Touch tracking
    protected var posX: Float
    protected var posY: Float
    protected var velX: Float = 0.0f
    protected var velY: Float = 0.0f
    private var isDown: Boolean = false
    private var maxX: Int = 0
    private var maxY: Int = 0

    class Info {
        var time: Int = 0
        val values = IntArray(7)    // order pattern row num_rows frame speed bpm
        val volumes = IntArray(64)
        val finalVols = IntArray(64)
        val pans = IntArray(64)
        val instruments = IntArray(64)
        val keys = IntArray(64)
        val periods = IntArray(64)
    }

    private fun limitPosition() {
        if (posX > maxX - canvasWidth) {
            posX = (maxX - canvasWidth).toFloat()
        }
        if (posX < 0) {
            posX = 0f
        }

        if (posY > maxY - canvasHeight) {
            posY = (maxY - canvasHeight).toFloat()
        }
        if (posY < 0) {
            posY = 0f
        }
    }

    private inner class MyGestureDetector : SimpleOnGestureListener() {

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            synchronized(this) {
                posX = posX.plus(distanceX)
                posY = posY.plus(distanceY)

                limitPosition()

                velY = 0f
                velX = velY
            }
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            //This is very wonky.
            //velX = velocityX / 25
            //velY = velocityY / 25
            return true
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
            velX = velY        // stop fling
            return true
        }
    }

    // Hmpf, reinventing the wheel instead of using Scroller
    private fun updateScroll() {

        posX -= velX
        posY -= velY

        limitPosition()

        velX = velX.times(0.9f)
        if (abs(velX) < 0.5) {
            velX = 0f
        }

        velY = velX.times(0.9f)
        if (abs(velY) < 0.5) {
            velY = 0f
        }
    }

    init {

        // register our interest in hearing preferences_about changes to our surface
        setupHolder()

        posY = 0f
        posX = posY
        isDown = false

        // Gesture detection
        gestureDetector = GestureDetector(context, MyGestureDetector())

        setupTouch()

        screenSize = context.getScreenSize()
    }

    private fun setupHolder() {
        val holder = holder
        holder.addCallback(this)

        surfaceHolder = holder
    }

    private fun setupTouch() {
        val gestureListener = OnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }

        setOnClickListener(this@Viewer)
        setOnTouchListener(gestureListener)
    }

    override fun onClick(view: View) {
        // do nothing
    }

    protected open fun onClick(x: Int, y: Int) {
        val parent = parent as View
        parent.performClick()
    }

    protected open fun onLongClick(x: Int, y: Int) {
        // do nothing
    }

    open fun setRotation(value: Int) {
        viewerRotation = value
    }

    open fun update(info: Info, paused: Boolean) {
        updateScroll()
    }

    open fun setup(modPlayer: ModInterface, modVars: IntArray) {
        val chn = modVars[3]
        this.modVars = modVars
        this.modPlayer = modPlayer

        isMuted = BooleanArray(chn)
        for (i in 0 until chn) {
            try {
                isMuted[i] = modPlayer.mute(i, -1) == 1
            } catch (e: RemoteException) {
                Log.e(TAG, "Can't read channel mute status")
            }

        }

        posY = 0f
        posX = posY
    }

    fun setMaxX(x: Int) {
        synchronized(this) {
            maxX = x
        }
    }

    fun setMaxY(y: Int) {
        synchronized(this) {
            maxY = y
        }
    }

    /* Callback invoked when the surface dimensions change. */
    private fun setSurfaceSize(width: Int, height: Int) {
        // synchronized to make sure these all change atomically
        synchronized(surfaceHolder) {
            canvasWidth = width
            canvasHeight = height
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        setSurfaceSize(width, height)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceHolder = holder
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // do nothing
    }


    fun getBackgroundColor(): Int {
        //Detect if dark mode is enabled or not
        return when (resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> Color.BLACK
            Configuration.UI_MODE_NIGHT_NO -> Color.WHITE
            else -> Color.BLACK
        }
    }

    companion object {
        private val TAG = Viewer::class.java.simpleName
    }
}
