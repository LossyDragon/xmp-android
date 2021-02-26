package org.helllabs.android.xmp.service

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.*
import androidx.lifecycle.MutableLiveData
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.preferences.PrefManager
import org.helllabs.android.xmp.service.notifier.LegacyNotifier
import org.helllabs.android.xmp.service.notifier.LollipopNotifier
import org.helllabs.android.xmp.service.notifier.Notifier
import org.helllabs.android.xmp.service.notifier.OreoNotifier
import org.helllabs.android.xmp.service.utils.*
import org.helllabs.android.xmp.util.FileUtils.basename
import org.helllabs.android.xmp.util.InfoCache.delete
import org.helllabs.android.xmp.util.InfoCache.testModule
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.logE
import org.helllabs.android.xmp.util.logI
import org.helllabs.android.xmp.util.logW

class PlayerService : Service(), OnAudioFocusChangeListener {

    private var audioManager: AudioManager? = null
    private var remoteControl: RemoteControl? = null
    private var hasAudioFocus = false
    private var ducking = false
    private var audioInitialized = false
    private var playThread: Thread? = null
    private var watchdog: Watchdog? = null
    private var sampleRate = 0
    private var volume = 0
    private var notifier: Notifier? = null
    private var cmd = 0
    private var restart = false
    private var canRelease = false
    var isPlayerPaused = false
        private set

    // save previous pause state
    private var previousPaused = false

    // don't play current buffer if changing module while paused
    private var discardBuffer = false
    private var looped = false
    private var playerAllSequences = false
    private var startIndex = 0
    private var updateData = false
    private var currentFileName: String? = null
    private var queue: QueueManager? = null
    private val callbacks = RemoteCallbackList<PlayerCallback>()
    private var sequenceNumber = 0
    private var receiverHelper: ReceiverHelper? = null

    override fun onCreate() {
        super.onCreate()
        logI("Create service")
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        remoteControl = RemoteControl(this, audioManager)
        hasAudioFocus = requestAudioFocus()
        if (!hasAudioFocus) {
            logE("Can't get audio focus")
        }
        receiverHelper = ReceiverHelper(this)
        receiverHelper!!.registerReceivers()
        var bufferMs = PrefManager.bufferMs
        if (bufferMs < MIN_BUFFER_MS) {
            bufferMs = MIN_BUFFER_MS
        } else if (bufferMs > MAX_BUFFER_MS) {
            bufferMs = MAX_BUFFER_MS
        }
        sampleRate = PrefManager.samplingRate.toInt()
        if (Xmp.init(sampleRate, bufferMs)) {
            audioInitialized = true
        } else {
            logE("error initializing audio")
        }
        volume = Xmp.getVolume()
        isPlayerAlive.postValue(false)
        isLoaded = false
        isPlayerPaused = false
        playerAllSequences = PrefManager.allSequences

        // session = new MediaSessionCompat(this, getPackageName());
        // session.setActive(true);

        notifier = when {
            Build.VERSION.SDK_INT >= 26 -> OreoNotifier(this)
            Build.VERSION.SDK_INT >= 21 -> LollipopNotifier(this)
            else -> LegacyNotifier(this)
        }

        watchdog = Watchdog(10)
        watchdog!!.setOnTimeoutListener(
            object : Watchdog.OnTimeoutListener {
                override fun onTimeout() {
                    stopSelf()
                }
            }
        )
        watchdog!!.start()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        receiverHelper!!.unregisterReceivers()
        watchdog!!.stop()
        notifier!!.cancel()

        // session.setActive(false);
        if (audioInitialized) {
            end(if (hasAudioFocus) RESULT_OK else RESULT_NO_AUDIO_FOCUS)
        } else {
            end(RESULT_CANT_OPEN_AUDIO)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun requestAudioFocus(): Boolean {
        return audioManager!!.requestAudioFocus(
            this,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun updateNotification() {
        // It seems that queue can be null if we're called from PhoneStateListener
        if (queue != null) {
            var name = Xmp.getModName()
            if (name.isEmpty()) {
                name = basename(queue!!.filename)
            }

            val ticker = if (isPlayerPaused) Notifier.TYPE_PAUSE else 0
            notifier!!.notify(name, Xmp.getModType(), queue!!.index, ticker)
        }
    }

    private fun doPauseAndNotify() {
        isPlayerPaused = isPlayerPaused xor true
        updateNotification()
        if (isPlayerPaused) {
            Xmp.stopAudio()
            remoteControl!!.setStatePaused()
        } else {
            remoteControl!!.setStatePlaying()
            Xmp.restartAudio()
        }
    }

    fun actionStop() {
        Xmp.stopModule()
        if (isPlayerPaused) {
            doPauseAndNotify()
        }
        cmd = CMD_STOP
    }

    fun actionPlayPause() {
        doPauseAndNotify()

        // Notify clients that we paused
        val numClients = callbacks.beginBroadcast()
        for (i in 0 until numClients) {
            try {
                callbacks.getBroadcastItem(i).pauseCallback()
            } catch (e: RemoteException) {
                logE("Error notifying pause to client")
            }
        }
        callbacks.finishBroadcast()
    }

    fun actionPrev() {
        if (Xmp.time() > 2000) {
            Xmp.seek(0)
        } else {
            Xmp.stopModule()
            cmd = CMD_PREV
        }
        if (isPlayerPaused) {
            doPauseAndNotify()
            discardBuffer = true
        }
    }

    fun actionNext() {
        Xmp.stopModule()
        if (isPlayerPaused) {
            doPauseAndNotify()
            discardBuffer = true
        }
        cmd = CMD_NEXT
    }

    // private int playFrame() {
    // 	// Synchronize frame play with data gathering so we don't change playing variables
    // 	// in the middle of e.g. sample data reading, which results in a segfault in C code
    //
    // 	synchronized (playThread) {
    // 		return Xmp.playBuffer();
    // 	}
    // }

    private fun notifyNewSequence() {
        val numClients = callbacks.beginBroadcast()
        for (j in 0 until numClients) {
            try {
                callbacks.getBroadcastItem(j).newSequenceCallback()
            } catch (e: RemoteException) {
                logE("Error notifying end of module to client")
            }
        }
        callbacks.finishBroadcast()
    }

    private inner class PlayRunnable : Runnable {
        override fun run() {
            cmd = CMD_NONE
            val vars = IntArray(8)
            remoteControl!!.setStatePlaying()
            var lastRecognized = 0
            do {
                currentFileName = queue!!.filename // Used in reconnection

                // If this file is unrecognized, and we're going backwards, go to previous
                // If we're at the start of the list, go to the last recognized file
                if (currentFileName == null || !testModule(currentFileName!!)) {
                    logW("$currentFileName: unrecognized format")
                    if (cmd == CMD_PREV) {
                        if (queue!!.index <= 0) {
                            // -1 because we have queue.next() in the while condition
                            queue!!.index = lastRecognized - 1
                            continue
                        }
                        queue!!.previous()
                    }
                    continue
                }

                // Set default pan before we load the module
                val defpan = PrefManager.defaultPan
                logI("Set default pan to $defpan")
                Xmp.setPlayer(Xmp.PLAYER_DEFPAN, defpan)

                // Ditto if we can't load the module
                logI("Load $currentFileName")
                if (Xmp.loadModule(currentFileName!!) < 0) {
                    logE("Error loading $currentFileName")
                    if (cmd == CMD_PREV) {
                        if (queue!!.index <= 0) {
                            queue!!.index = lastRecognized - 1
                            continue
                        }
                        queue!!.previous()
                    }
                    continue
                }
                lastRecognized = queue!!.index
                cmd = CMD_NONE
                var name = Xmp.getModName()
                if (name.isEmpty() && currentFileName != null) {
                    name = basename(currentFileName!!)
                }
                notifier!!.notify(name, Xmp.getModType(), queue!!.index, Notifier.TYPE_TICKER)
                isLoaded = true
                val volBoost = PrefManager.volumeBoost
                val interpTypes = intArrayOf(
                    Xmp.INTERP_NEAREST,
                    Xmp.INTERP_LINEAR,
                    Xmp.INTERP_SPLINE
                )
                val temp = PrefManager.interpType.toInt()
                var interpType: Int
                interpType = if (temp in 1..2) {
                    interpTypes[temp]
                } else {
                    Xmp.INTERP_LINEAR
                }
                if (!PrefManager.interpolate) {
                    interpType = Xmp.INTERP_NEAREST
                }
                Xmp.startPlayer(sampleRate)
                synchronized(audioManager!!) {
                    if (ducking) {
                        Xmp.setPlayer(Xmp.PLAYER_VOLUME, DUCK_VOLUME)
                    }
                }

                // Unmute all channels
                for (i in 0..63) {
                    Xmp.mute(i, 0)
                }
                var numClients = callbacks.beginBroadcast()
                for (j in 0 until numClients) {
                    try {
                        callbacks.getBroadcastItem(j).newModCallback()
                    } catch (e: RemoteException) {
                        logE("Error notifying new module to client")
                    }
                }
                callbacks.finishBroadcast()
                Xmp.setPlayer(Xmp.PLAYER_AMP, volBoost.toInt())
                Xmp.setPlayer(Xmp.PLAYER_MIX, PrefManager.stereoMix)
                Xmp.setPlayer(Xmp.PLAYER_INTERP, interpType)
                Xmp.setPlayer(Xmp.PLAYER_DSP, Xmp.DSP_LOWPASS)
                var flags = Xmp.getPlayer(Xmp.PLAYER_CFLAGS)
                flags = if (PrefManager.amigaMixer) {
                    flags or Xmp.FLAGS_A500
                } else {
                    flags and Xmp.FLAGS_A500.inv()
                }
                Xmp.setPlayer(Xmp.PLAYER_CFLAGS, flags)
                updateData = true
                sequenceNumber = 0
                var playNewSequence: Boolean
                Xmp.setSequence(sequenceNumber)
                Xmp.playAudio()
                logI("Enter play loop")
                do {
                    Xmp.getModVars(vars)
                    remoteControl!!.setMetadata(
                        Xmp.getModName(),
                        Xmp.getModType(),
                        vars[0].toLong()
                    )
                    while (cmd == CMD_NONE) {
                        discardBuffer = false

                        // Wait if paused
                        while (isPlayerPaused) {
                            try {
                                Thread.sleep(100)
                            } catch (e: InterruptedException) {
                                break
                            }
                            watchdog!!.refresh()
                            receiverHelper!!.checkReceivers()
                        }
                        if (discardBuffer) {
                            logD("discard buffer")
                            Xmp.dropAudio()
                            break
                        }

                        // Wait if no buffers available
                        while (!Xmp.hasFreeBuffer() && !isPlayerPaused && cmd == CMD_NONE) {
                            try {
                                Thread.sleep(40)
                            } catch (e: InterruptedException) {
                                /* no-op */
                            }
                        }

                        // Fill a new buffer
                        if (Xmp.fillBuffer(looped) < 0) {
                            break
                        }
                        watchdog!!.refresh()
                        receiverHelper!!.checkReceivers()
                    }

                    // Subsong explorer
                    // Do all this if we've exited normally and explorer is active
                    playNewSequence = false
                    if (playerAllSequences && cmd == CMD_NONE) {
                        sequenceNumber++
                        logI("Play sequence $sequenceNumber")
                        if (Xmp.setSequence(sequenceNumber)) {
                            playNewSequence = true
                            notifyNewSequence()
                        }
                    }
                } while (playNewSequence)
                Xmp.endPlayer()
                isLoaded = false

                // notify end of module to our clients
                numClients = callbacks.beginBroadcast()
                if (numClients > 0) {
                    canRelease = false
                    for (j in 0 until numClients) {
                        try {
                            logI("Call end of module callback")
                            callbacks.getBroadcastItem(j).endModCallback()
                        } catch (e: RemoteException) {
                            logE("Error notifying end of module to client")
                        }
                    }
                    callbacks.finishBroadcast()

                    // if we have clients, make sure we can release module
                    var timeout = 0
                    try {
                        while (!canRelease && timeout < 20) {
                            Thread.sleep(100)
                            timeout++
                        }
                    } catch (e: InterruptedException) {
                        logE("Sleep interrupted: $e")
                    }
                } else {
                    callbacks.finishBroadcast()
                }
                logI("Release module")
                Xmp.releaseModule()

                // audio.stop();

                // Used when current files are replaced by a new set
                if (restart) {
                    logI("Restart")
                    queue!!.index = startIndex - 1
                    cmd = CMD_NONE
                    restart = false
                } else if (cmd == CMD_PREV) {
                    queue!!.previous()
                    // returnToPrev = false;
                }
            } while (cmd != CMD_STOP && queue!!.next())
            synchronized(playThread!!) {
                updateData = false // stop getChannelData update
            }
            watchdog!!.stop()
            notifier!!.cancel()
            remoteControl!!.setStateStopped()
            audioManager!!.abandonAudioFocus(this@PlayerService)

            logI("Stop service")
            stopSelf()
        }
    }

    private fun end(result: Int) {
        logI("End service")
        val numClients = callbacks.beginBroadcast()
        for (i in 0 until numClients) {
            try {
                callbacks.getBroadcastItem(i).endPlayCallback(result)
            } catch (e: RemoteException) {
                logE("Error notifying end of play to client")
            }
        }
        callbacks.finishBroadcast()
        isPlayerAlive.postValue(false)
        Xmp.stopModule()
        if (isPlayerPaused) {
            doPauseAndNotify()
        }
        Xmp.deinit()
        // audio.release();
    }

    private val binder = object : ModInterface.Stub() {
        override fun play(
            fileList: MutableList<String>,
            start: Int,
            shuffle: Boolean,
            loopList: Boolean,
            keepFirst: Boolean
        ) {
            if (!audioInitialized || !hasAudioFocus) {
                stopSelf()
                return
            }
            queue = QueueManager(fileList, start, shuffle, loopList, keepFirst)
            notifier!!.setQueue(queue)
            // notifier.clean();
            cmd = CMD_NONE
            if (isPlayerPaused) {
                doPauseAndNotify()
            }
            if (isPlayerAlive.value == true) {
                logI("Use existing player thread")
                restart = true
                startIndex = if (keepFirst) 0 else start
                nextSong()
            } else {
                logI("Start player thread")
                playThread = Thread(PlayRunnable())
                playThread!!.start()
            }
            isPlayerAlive.postValue(true)
        }

        override fun add(fileList: List<String>) {
            queue!!.add(fileList)
            updateNotification()
            // notifier.notification("Added to play queue");
        }

        override fun stop() {
            actionStop()
        }

        override fun pause() {
            doPauseAndNotify()
            receiverHelper!!.isHeadsetPaused = false
        }

        override fun getInfo(values: IntArray) {
            Xmp.getInfo(values)
        }

        override fun seek(seconds: Int) {
            Xmp.seek(seconds)
        }

        override fun time(): Int {
            return Xmp.time()
        }

        override fun getModVars(vars: IntArray) {
            Xmp.getModVars(vars)
        }

        override fun getModName(): String {
            return Xmp.getModName()
        }

        override fun getModType(): String {
            return Xmp.getModType()
        }

        override fun getChannelData(
            volumes: IntArray,
            finalvols: IntArray,
            pans: IntArray,
            instruments: IntArray,
            keys: IntArray,
            periods: IntArray
        ) {
            if (updateData) {
                synchronized(playThread!!) {
                    Xmp.getChannelData(volumes, finalvols, pans, instruments, keys, periods)
                }
            }
        }

        override fun getSampleData(
            trigger: Boolean,
            ins: Int,
            key: Int,
            period: Int,
            chn: Int,
            width: Int,
            buffer: ByteArray
        ) {
            if (updateData) {
                synchronized(playThread!!) {
                    Xmp.getSampleData(trigger, ins, key, period, chn, width, buffer)
                }
            }
        }

        override fun nextSong() {
            Xmp.stopModule()
            cmd = CMD_NEXT
            if (isPlayerPaused) {
                doPauseAndNotify()
            }
            discardBuffer = true
        }

        override fun prevSong() {
            Xmp.stopModule()
            cmd = CMD_PREV
            if (isPlayerPaused) {
                doPauseAndNotify()
            }
            discardBuffer = true
        }

        @Throws(RemoteException::class)
        override fun toggleLoop(): Boolean {
            looped = looped xor true
            return looped
        }

        @Throws(RemoteException::class)
        override fun toggleAllSequences(): Boolean {
            playerAllSequences = playerAllSequences xor true
            return playerAllSequences
        }

        @Throws(RemoteException::class)
        override fun getLoop(): Boolean {
            return looped
        }

        @Throws(RemoteException::class)
        override fun getAllSequences(): Boolean {
            return playerAllSequences
        }

        override fun isPaused(): Boolean {
            return isPlayerPaused
        }

        override fun setSequence(seq: Int): Boolean {
            val ret = Xmp.setSequence(seq)
            if (ret) {
                sequenceNumber = seq
                notifyNewSequence()
            }
            return ret
        }

        override fun allowRelease() {
            canRelease = true
        }

        override fun getSeqVars(vars: IntArray) {
            Xmp.getSeqVars(vars)
        }

        // for Reconnection
        override fun getFileName(): String {
            return currentFileName!!
        }

        override fun getInstruments(): Array<String> {
            return Xmp.getInstruments()
        }

        override fun getPatternRow(
            pat: Int,
            row: Int,
            rowNotes: ByteArray,
            rowInstruments: ByteArray
        ) {
            if (isPlayerAlive.value == true) {
                Xmp.getPatternRow(pat, row, rowNotes, rowInstruments)
            }
        }

        override fun mute(chn: Int, status: Int): Int {
            return Xmp.mute(chn, status)
        }

        override fun hasComment(): Boolean {
            return !Xmp.getComment().isNullOrEmpty()
        }

        // File management
        override fun deleteFile(): Boolean {
            logI("Delete file $currentFileName")
            return delete(currentFileName!!)
        }

        // Callback
        override fun registerCallback(callback: PlayerCallback) {
            callbacks.register(callback)
        }

        override fun unregisterCallback(callback: PlayerCallback) {
            callbacks.unregister(callback)
        }
    }

    // for audio focus loss
    private fun autoPause(pause: Boolean): Boolean {
        logI("autoPause($pause), previously ${receiverHelper!!.isAutoPaused}")
        if (pause) {
            previousPaused = isPlayerPaused
            receiverHelper!!.isAutoPaused = true
            isPlayerPaused = false // set to complement, flip on doPause()
            doPauseAndNotify()
        } else {
            if (receiverHelper!!.isAutoPaused && !receiverHelper!!.isHeadsetPaused) {
                receiverHelper!!.isAutoPaused = false
                isPlayerPaused = !previousPaused // set to complement, flip on doPause()
                doPauseAndNotify()
            }
        }
        return receiverHelper!!.isAutoPaused
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                logD("AUDIOFOCUS_LOSS_TRANSIENT")
                // Pause playback
                autoPause(true)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                logD("AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK")
                // Lower volume
                synchronized(audioManager!!) {
                    volume = Xmp.getVolume()
                    Xmp.setVolume(DUCK_VOLUME)
                    ducking = true
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                logD("AUDIOFOCUS_GAIN")
                // Resume playback/raise volume
                autoPause(false)
                synchronized(audioManager!!) {
                    Xmp.setVolume(volume)
                    ducking = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                logD("AUDIOFOCUS_LOSS")
                // Stop playback
                actionStop()
            }
            else -> {
            }
        }
    }

    companion object {
        const val RESULT_OK = 0
        const val RESULT_CANT_OPEN_AUDIO = 1
        const val RESULT_NO_AUDIO_FOCUS = 2
        private const val CMD_NONE = 0
        private const val CMD_NEXT = 1
        private const val CMD_PREV = 2
        private const val CMD_STOP = 3
        private const val MIN_BUFFER_MS = 80
        private const val MAX_BUFFER_MS = 1000
        private const val DEFAULT_BUFFER_MS = 400
        private const val DUCK_VOLUME = 0x500

        @JvmField
        var isPlayerAlive: MutableLiveData<Boolean> = MutableLiveData(false)

        @JvmField
        var isLoaded = false
    }
}
