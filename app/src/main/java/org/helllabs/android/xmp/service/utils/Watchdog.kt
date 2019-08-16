package org.helllabs.android.xmp.service.utils

class Watchdog(private val timeout: Int) : Runnable {
    private var timer: Int = 0
    private var running: Boolean = false
    private var thread: Thread? = null
    private var listener: OnTimeoutListener? = null

    interface OnTimeoutListener {
        fun onTimeout()
    }

    fun setOnTimeoutListener(listener: OnTimeoutListener) {
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
        }

    }

    fun refresh() {
        timer = timeout
    }
}
