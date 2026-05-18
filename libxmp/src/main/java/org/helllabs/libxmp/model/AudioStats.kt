package org.helllabs.libxmp.model

import androidx.compose.runtime.Immutable

/**
 * Real-time audio statistics from the Oboe audio engine.
 *
 * Provides performance metrics and configuration details about the active audio stream.
 * All values represent the current state at the time [org.helllabs.libxmp.Xmp.getAudioStats] was called.
 *
 * @property xrunCount Total number of buffer underruns/overruns detected by the audio system.
 *                     Higher values indicate audio glitches. Only available on devices that
 *                     support XRun counting (typically Android 8.1+).
 * @property underrunCount Number of buffer underruns detected by the app (when playback buffer
 *                         runs dry). Each underrun causes audio gaps/silence.
 * @property framesPerBurst Number of audio frames processed per callback burst. Lower values
 *                          mean lower latency but higher CPU usage.
 * @property bufferCapacity Maximum buffer size in frames. Represents the total capacity
 *                          available for buffering audio data.
 * @property bufferSize Current buffer size in frames. May be adjusted dynamically by the system
 *                      to balance latency and stability.
 * @property sampleRate Sample rate in Hz (e.g., 44100, 48000). The actual rate Oboe opened with,
 *                      which may differ from the requested rate.
 * @property audioApi The audio API being used: "AAudio" (modern, lower latency) or
 *                    "OpenSL ES" (legacy, broader compatibility).
 * @property sharingMode Stream sharing mode: "Exclusive" (dedicated audio path, lowest latency)
 *                       or "Shared" (mixed with other apps, more compatible).
 */
@Immutable
data class AudioStats(
    val xrunCount: Int = 0,
    val underrunCount: Int = 0,
    val framesPerBurst: Int = 0,
    val bufferCapacity: Int = 0,
    val bufferSize: Int = 0,
    val sampleRate: Int = 0,
    val audioApi: String = "",
    val sharingMode: String = ""
)
