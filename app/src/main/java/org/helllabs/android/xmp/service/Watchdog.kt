package org.helllabs.android.xmp.service

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class Watchdog(private val timeoutSeconds: Int) {
    private var executor: ScheduledExecutorService? = null
    private var listener: OnTimeoutListener? = null
    private var scheduledTask: ScheduledFuture<*>? = null

    fun interface OnTimeoutListener {
        fun onTimeout()
    }

    fun setOnTimeoutListener(listener: OnTimeoutListener?) {
        this.listener = listener
    }

    fun start() {
        refresh()
    }

    fun stop() {
        scheduledTask?.cancel(false)
        scheduledTask = null
        executor?.shutdown()
        executor = null
    }

    fun refresh() {
        scheduledTask?.cancel(false)

        if (executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor()
        }

        scheduledTask = executor?.schedule({
            listener?.onTimeout()
            stop()
        }, timeoutSeconds.toLong(), TimeUnit.SECONDS)
    }
}
