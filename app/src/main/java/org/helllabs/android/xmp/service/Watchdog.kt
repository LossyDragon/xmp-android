package org.helllabs.android.xmp.service

import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*

class Watchdog(private val timeoutSeconds: Long, private val onTimeout: () -> Unit) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val currentJob = AtomicReference<Job?>(null)

    fun start() {
        stop()
        currentJob.set(
            scope.launch {
                delay(timeoutSeconds * 1000)
                onTimeout()
            }
        )
    }

    fun stop() {
        currentJob.getAndSet(null)?.cancel()
    }

    fun refresh() {
        if (currentJob.get() != null) {
            start()
        }
    }
}
