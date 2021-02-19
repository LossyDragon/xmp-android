package org.helllabs.android.xmp.service.utils

class Watchdog(private val timeout: Int) : Runnable {
    private var timer = 0
    private var running = false
    private var thread: Thread? = null
    private var listener: OnTimeoutListener? = null

    interface OnTimeoutListener {
        fun onTimeout()
    }

    fun setOnTimeoutListener(listener: OnTimeoutListener?) {
        this.listener = listener
    }

    override fun run() {
        while (running) {
            if (--timer <= 0) {
                listener!!.onTimeout()
                break
            }
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                /* no-op */
            }
        }
    }

    fun start() {
        running = true
        refresh()
        thread = Thread(this)
        thread!!.start()
    }

    fun stop() {
        running = false
        try {
            thread!!.join()
        } catch (e: InterruptedException) {
            /* no-op */
        }
    }

    fun refresh() {
        timer = timeout
    }
}