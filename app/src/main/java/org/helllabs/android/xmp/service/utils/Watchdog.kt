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

    // The runnable of the watchdog, if the threshold is met, it calls the interface to be executed.
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

    // Initialize the watchdog to start counting.
    fun start() {
        running = true
        refresh()
        thread = Thread(this)
        thread?.start()
    }

    // Stop and shutdown the watchdog.
    fun stop() {
        running = false
        try {
            thread?.join()
        } catch (e: InterruptedException) {
        }
    }

    // Called when everything is functioning okay. To reset the counter/
    fun refresh() {
        timer = timeout
    }
}
