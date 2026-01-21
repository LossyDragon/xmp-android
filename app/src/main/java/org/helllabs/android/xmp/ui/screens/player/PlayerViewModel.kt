package org.helllabs.android.xmp.ui.screens.player

import android.net.Uri
import android.support.v4.media.session.MediaControllerCompat
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazygeniouz.dfc.file.DocumentFileCompat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.model.ChannelInfo
import org.helllabs.android.xmp.model.FrameInfo
import org.helllabs.android.xmp.model.ModVars
import org.helllabs.android.xmp.model.SequenceVars
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.ui.screens.player.components.PlayerControlsEvent
import org.helllabs.android.xmp.ui.screens.player.components.SeekEvent
import timber.log.Timber

// region State Classes

enum class RepeatMode { OFF, REPEAT, REPEAT_ONE }

@Immutable
data class PlayerState(
    val currentMessage: String = "",
    val currentViewer: Int = 0,
    val infoTitle: String = "",
    val infoType: String = "",
    val screenOn: Boolean = true,
    val serviceConnected: Boolean = false,
    val showInfoDialog: Boolean = false,
    val showMessageDialog: Boolean = false,
    val skipToPrevious: Boolean = false
)

@Immutable
data class PlayerInfoState(
    val infoSpeed: String = "00",
    val infoBpm: String = "00",
    val infoPos: String = "00",
    val infoPat: String = "00",
    val isVisible: Boolean = true
)

@Immutable
data class PlayerButtonsState(
    val isPlaying: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
)

@Immutable
data class PlayerTimeState(
    val timeNow: String = "-:--",
    val timeTotal: String = "-:--",
    val seekPos: Float = 0f,
    val seekMax: Float = 1f,
    val isVisible: Boolean = true,
    val isSeeking: Boolean = false
)

@Immutable
data class PlayerSheetState(
    val moduleInfo: List<Int> = listOf(0, 0, 0, 0, 0),
    val isPlayAllSequences: Boolean = false,
    val numOfSequences: List<Int> = emptyList(),
    val currentSequence: Int = 0
)

@Immutable
data class PlayerActivityState(
    val fileList: List<Uri> = emptyList(),
    val keepFirst: Boolean = false,
    val loopListMode: Boolean = false,
    val playTime: Float = 0f,
    val shuffleMode: Boolean = false,
    val start: Int = 0,
    val totalTime: Int = 0
)

@Immutable
data class ChannelMuteState(val isMuted: ImmutableList<Boolean> = persistentListOf()) {
    operator fun get(index: Int) = isMuted.getOrElse(index) { false } // Safe access
    fun count(predicate: (Boolean) -> Boolean) = isMuted.count(predicate)
    val size: Int get() = isMuted.size
}

@Immutable
data class SampleDataState(val buffers: ImmutableList<ByteArray> = persistentListOf())

@Immutable
data class PatternRowData(
    val notes: ByteArray,
    val instruments: ByteArray,
    val fxType: ByteArray,
    val fxParm: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PatternRowData) return false
        return notes.contentEquals(other.notes) &&
            instruments.contentEquals(other.instruments) &&
            fxType.contentEquals(other.fxType) &&
            fxParm.contentEquals(other.fxParm)
    }

    override fun hashCode(): Int {
        var result = notes.contentHashCode()
        result = 31 * result + instruments.contentHashCode()
        result = 31 * result + fxType.contentHashCode()
        result = 31 * result + fxParm.contentHashCode()
        return result
    }

    companion object {
        fun create(size: Int = 64) = PatternRowData(
            notes = ByteArray(size),
            instruments = ByteArray(size),
            fxType = ByteArray(size),
            fxParm = ByteArray(size)
        )
    }
}

@Immutable
data class PatternDataState(
    val rows: Map<Int, PatternRowData> = emptyMap(),
    val currentPattern: Int = -1
) {
    fun getRow(row: Int): PatternRowData? = rows[row]
}

/**
 * Consolidated screen state - reduces 12+ StateFlow collections to 1
 */
@Immutable
data class PlayerScreenState(
    val ui: PlayerState = PlayerState(),
    val info: PlayerInfoState = PlayerInfoState(),
    val buttons: PlayerButtonsState = PlayerButtonsState(),
    val time: PlayerTimeState = PlayerTimeState(),
    val drawer: PlayerSheetState = PlayerSheetState(),
    val modVars: ModVars = ModVars(),
    val instrumentNames: ImmutableList<String> = persistentListOf(),
    val isMuted: ChannelMuteState = ChannelMuteState(),
    val channelInfo: ChannelInfo = ChannelInfo(),
    val frameInfo: FrameInfo = FrameInfo(),
    val sampleData: SampleDataState = SampleDataState(),
    val patternData: PatternDataState = PatternDataState()
)

// endregion

class PlayerViewModel(prefManager: PrefManager) : ViewModel() {

    // region Private State Flows

    val uiState: StateFlow<PlayerState>
        field = MutableStateFlow(PlayerState())

    val infoState: StateFlow<PlayerInfoState>
        field = MutableStateFlow(PlayerInfoState())

    val buttonState: StateFlow<PlayerButtonsState>
        field = MutableStateFlow(PlayerButtonsState())

    val timeState: StateFlow<PlayerTimeState>
        field = MutableStateFlow(PlayerTimeState())

    val drawerState: StateFlow<PlayerSheetState>
        field = MutableStateFlow(PlayerSheetState())

    val modVars: StateFlow<ModVars>
        field = MutableStateFlow(ModVars())

    val insName: StateFlow<ImmutableList<String>>
        field = MutableStateFlow(persistentListOf())

    val isMuted: StateFlow<ChannelMuteState>
        field = MutableStateFlow(ChannelMuteState())

    val channelInfo: StateFlow<ChannelInfo>
        field = MutableStateFlow(ChannelInfo())

    val frameInfo: StateFlow<FrameInfo>
        field = MutableStateFlow(FrameInfo())

    val sampleData: StateFlow<SampleDataState>
        field = MutableStateFlow(SampleDataState())

    val patternData: StateFlow<PatternDataState>
        field = MutableStateFlow(PatternDataState())

    val activityState: StateFlow<PlayerActivityState>
        field = MutableStateFlow(PlayerActivityState())

    val playlistChoice: StateFlow<DocumentFileCompat?>
        field = MutableStateFlow(null)

    private val _softError = Channel<String>(Channel.BUFFERED)
    val softError = _softError.receiveAsFlow()

    // endregion

    // region Consolidated Screen State

    /**
     * Single combined StateFlow for all screen state.
     * Collectors only recompose when relevant data changes.
     */
    val screenState: StateFlow<PlayerScreenState> = combine(
        combine(
            flow = uiState,
            flow2 = infoState,
            flow3 = buttonState,
            flow4 = timeState,
            flow5 = drawerState
        ) { ui, info, buttons, time, drawer ->
            ScreenStatePart1(ui, info, buttons, time, drawer)
        },
        combine(
            flow = modVars,
            flow2 = insName,
            flow3 = isMuted,
            flow4 = channelInfo,
            flow5 = frameInfo
        ) { modVars, insName, muted, channel, frame ->
            ScreenStatePart2(modVars, insName, muted, channel, frame)
        },
        combine(
            flow = sampleData,
            flow2 = patternData
        ) { sample, pattern ->
            ScreenStatePart3(sample, pattern)
        }
    ) { part1, part2, part3 ->
        PlayerScreenState(
            ui = part1.ui,
            info = part1.info,
            buttons = part1.buttons,
            time = part1.time,
            drawer = part1.drawer,
            modVars = part2.modVars,
            instrumentNames = part2.insName,
            isMuted = part2.muted,
            channelInfo = part2.channel,
            frameInfo = part2.frame,
            sampleData = part3.sample,
            patternData = part3.pattern
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerScreenState()
    )

    // Helper data classes for combine (max 5 params)
    private data class ScreenStatePart1(
        val ui: PlayerState,
        val info: PlayerInfoState,
        val buttons: PlayerButtonsState,
        val time: PlayerTimeState,
        val drawer: PlayerSheetState
    )

    private data class ScreenStatePart2(
        val modVars: ModVars,
        val insName: ImmutableList<String>,
        val muted: ChannelMuteState,
        val channel: ChannelInfo,
        val frame: FrameInfo
    )

    private data class ScreenStatePart3(val sample: SampleDataState, val pattern: PatternDataState)

    // endregion

    // region Update Loop

    private var updateJob: Job? = null
    private var mediaController: MediaControllerCompat? = null

    private val seqVars = MutableStateFlow(SequenceVars())

    private val showHex = prefManager.showHexFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Sample data buffers - reused to avoid allocations
    private var sampleBuffers: Array<ByteArray>? = null
    private var holdKey = IntArray(Xmp.MAX_CHANNELS)
    private var keyRow = IntArray(Xmp.MAX_CHANNELS)

    // Pattern data cache
    private val cachedPatternRows = mutableMapOf<Int, PatternRowData>()
    private var cachedPattern = -1
    private var currentVisibleRowRange: IntRange = IntRange.EMPTY

    // Reusable temp buffers for pattern fetching
    private val tempRowNotes = ByteArray(64)
    private val tempRowInstruments = ByteArray(64)
    private val tempRowFxType = ByteArray(64)
    private val tempRowFxParm = ByteArray(64)

    /**
     * Starts the main update loop. Called from Activity when service connects.
     * Runs on Default dispatcher to avoid blocking UI.
     */
    fun startUpdateLoop(modPlayer: PlayerService?) {
        Timber.d("startUpdateLoop called, modPlayer=$modPlayer")

        updateJob?.cancel()

        if (modPlayer == null) {
            Timber.e("modPlayer is null, aborting update loop")
            return
        }

        mediaController = modPlayer.mediaController
        Timber.d("mediaController set: $mediaController")

        updateJob = viewModelScope.launch(Dispatchers.Default) {
            Timber.d("Start update loop")

            while (isActive && uiState.value.serviceConnected) {
                val currentState = uiState.value

                if (!currentState.screenOn) {
                    delay(500.milliseconds)
                    continue
                }

                // Update all viewer data
                updateViewInfo()
                updateSampleData()

                // Update time and seekbar
                val time = Xmp.time().div(100f)
                setPlayTime(time)
                updateSeekBar()
                updateInfoTime()
                updateInfoState()

                delay(33.milliseconds)
            }

            Timber.i("Update loop ended")
        }
    }

    // endregion

    // region Event Handlers (moved from Activity)

    fun handleControlsEvent(event: PlayerControlsEvent) {
        val controls = mediaController ?: run {
            Timber.e("mediaController is null!")
            return
        }

        Timber.d("handleControlsEvent: $event")
        when (event) {
            PlayerControlsEvent.OnNext -> {
                controls.transportControls.skipToNext()
                isPlaying(true)
            }

            PlayerControlsEvent.OnPlay -> {
                if (PlayerService.isPlaying.value) {
                    controls.transportControls.pause()
                } else {
                    controls.transportControls.play()
                }
                isPlaying(PlayerService.isPlaying.value)
            }

            PlayerControlsEvent.OnPrev -> {
                controls.transportControls.skipToPrevious()
                isPlaying(PlayerService.isPlaying.value)
            }

            PlayerControlsEvent.OnStop -> {
                controls.transportControls.stop()
            }

            is PlayerControlsEvent.OnRepeat -> {
                // Note: modPlayer.toggleLoop needs to be called from Activity
                // since we don't hold modPlayer reference here
                toggleLoop(event.value)
            }
        }
    }

    fun handleSeekEvent(event: SeekEvent) {
        when (event) {
            is SeekEvent.OnSeek -> {
                if (event.isSeeking) {
                    isSeeking(true)
                } else {
                    mediaController?.transportControls?.seekTo(event.value.toLong() * 100)
                    isSeeking(false)
                    setPlayTime(Xmp.time().div(100f))
                }
            }
        }
    }

    // endregion

    // region Public State Updates

    val isPlaying: Boolean
        get() = buttonState.value.isPlaying

    fun onConnected(value: Boolean) {
        uiState.update { it.copy(serviceConnected = value) }
    }

    fun toggleLoop(value: RepeatMode) {
        buttonState.update { it.copy(repeatMode = value) }
    }

    fun isPlaying(value: Boolean) {
        buttonState.update { it.copy(isPlaying = value) }
    }

    fun screenOn(value: Boolean) {
        uiState.update { it.copy(screenOn = value) }
    }

    fun showInfoLine(value: Boolean) {
        timeState.update { it.copy(isVisible = value) }
        infoState.update { it.copy(isVisible = value) }
    }

    fun isSeeking(value: Boolean) {
        timeState.update { it.copy(isSeeking = value) }
    }

    fun onAllSequence(value: Boolean) {
        drawerState.update { it.copy(isPlayAllSequences = value) }
    }

    fun onSequence(value: Int) {
        drawerState.update { it.copy(currentSequence = value) }
    }

    fun changeViewer() {
        val current = (uiState.value.currentViewer + 1) % 3
        uiState.update { it.copy(currentViewer = current) }
    }

    fun showSheet(value: Boolean) {
        uiState.update { it.copy(showInfoDialog = value) }
    }

    fun closeMessage() = showMessage(false, "")

    fun showMessage(value: Boolean, message: String) {
        uiState.update { it.copy(showMessageDialog = value, currentMessage = message) }
    }

    private var currentBufferedRange: IntRange = IntRange.EMPTY
    fun setVisibleRowRange(range: IntRange) {
        val buffer = 10
        val bufferedRange = (range.first - buffer).coerceAtLeast(0)..(range.last + buffer)

        if (bufferedRange != currentBufferedRange) {
            currentBufferedRange = bufferedRange
            updatePatternData(bufferedRange)
        }
    }

    // endregion

    // region Activity State

    fun setActivityState(
        fileList: List<Uri>,
        shuffleMode: Boolean,
        loopListMode: Boolean,
        keepFirst: Boolean,
        start: Int
    ) {
        activityState.update {
            it.copy(
                fileList = fileList,
                shuffleMode = shuffleMode,
                loopListMode = loopListMode,
                keepFirst = keepFirst,
                start = start
            )
        }
    }

    fun playNewMod(modPlayer: PlayerService, fileList: List<Uri>, start: Int) {
        val state = activityState.value
        modPlayer.play(
            fileList = fileList,
            start = start,
            shuffle = state.shuffleMode,
            loopList = state.loopListMode,
            keepFirst = state.keepFirst
        )
    }

    // endregion

    // region Module Display

    fun showNewMod(modPlayer: PlayerService, skipToPrevious: Boolean) {
        Timber.i("Show new module | Previous: $skipToPrevious")

        resetSampleData()
        resetPatternData()

        val mVars = ModVars()
        Xmp.getModVars(mVars)
        modVars.value = mVars

        val sequence = Xmp.getSeqVars().asList().toImmutableList()
        seqVars.value = SequenceVars(sequence)

        drawerState.update {
            it.copy(
                moduleInfo = listOf(
                    mVars.numPatterns,
                    mVars.numInstruments,
                    mVars.numSamples,
                    mVars.numChannels,
                    mVars.lengthInPatterns
                ),
                isPlayAllSequences = modPlayer.playAllSequences,
                currentSequence = 0,
                numOfSequences = sequence.toList()
            )
        }

        activityState.update {
            it.copy(
                playTime = mVars.seqDuration.div(100f),
                totalTime = mVars.seqDuration / 1000
            )
        }

        timeState.update {
            it.copy(
                seekPos = activityState.value.playTime,
                seekMax = mVars.seqDuration.div(100f)
            )
        }

        val mode = when {
            modPlayer.isLoopPlaylist -> RepeatMode.REPEAT
            modPlayer.isRepeating -> RepeatMode.REPEAT_ONE
            else -> RepeatMode.OFF
        }
        toggleLoop(mode)

        val name = Xmp.getModName().trim().ifEmpty { modPlayer.getFileName() }
        val type = Xmp.getModType()
        uiState.update {
            it.copy(
                infoTitle = name,
                infoType = type,
                skipToPrevious = skipToPrevious
            )
        }

        if (uiState.value.serviceConnected) {
            Xmp.getModVars(modVars.value)

            val seq = Xmp.getSeqVars().asList().toImmutableList()
            seqVars.update { SequenceVars(seq) }

            insName.value = Xmp.getInstruments().toPersistentList()

            isMuted.value = ChannelMuteState(
                isMuted = List(modVars.value.numChannels) { i ->
                    Xmp.mute(i, -1) == 1
                }.toPersistentList()
            )
        }
    }

    fun showNewSequence(showSnack: (Int) -> Unit) {
        val time = modVars.value.seqDuration

        activityState.update { it.copy(totalTime = time / 1000) }
        timeState.update { it.copy(seekPos = 0f, seekMax = time.div(100f)) }
        drawerState.update { it.copy(currentSequence = modVars.value.currentSequence) }

        showSnack(time)
    }

    fun updateModVars() {
        val modVars = ModVars()
        Xmp.getModVars(modVars)
        this.modVars.value = modVars
    }

    // endregion

    // region Private Update Methods

    private fun resetPlayTime() {
        activityState.update { it.copy(playTime = 0f) }
    }

    private fun setPlayTime(time: Float) {
        activityState.update { it.copy(playTime = time) }
    }

    private fun updateSeekBar() {
        if (!timeState.value.isSeeking && activityState.value.playTime >= 0) {
            timeState.update { it.copy(seekPos = activityState.value.playTime) }
        }
    }

    private fun updateInfoTime() {
        val time = Xmp.time() / 1000
        timeState.update {
            it.copy(
                timeNow = Util.updateTime(time),
                timeTotal = Util.updateTime(activityState.value.totalTime)
            )
        }
    }

    private fun updateInfoState() {
        val fi = frameInfo.value
        val hex = showHex.value
        infoState.update {
            it.copy(
                infoPat = Util.updateFrameInfo(hex, fi.pattern),
                infoPos = Util.updateFrameInfo(hex, fi.pos),
                infoBpm = Util.updateFrameInfo(hex, fi.bpm),
                infoSpeed = Util.updateFrameInfo(hex, fi.speed)
            )
        }
    }

    private fun updateViewInfo() {
        val ci = ChannelInfo()
        Xmp.getChannelData(ci)
        channelInfo.value = ci

        val fi = FrameInfo()
        Xmp.getInfo(fi)
        frameInfo.value = fi

        val numChannels = modVars.value.numChannels
        isMuted.value = ChannelMuteState(
            isMuted = List(numChannels) { i -> Xmp.mute(i, -1) == 1 }.toPersistentList()
        )
    }

    // endregion

    // region Sample Data (optimized)

    private fun updateSampleData() {
        if (!buttonState.value.isPlaying) return

        val numChannels = modVars.value.numChannels
        if (numChannels == 0) return

        // Initialize or resize buffers only when needed
        if (sampleBuffers == null || sampleBuffers!!.size != numChannels) {
            sampleBuffers = Array(numChannels) { ByteArray(Xmp.MAX_BUFFERS) }
            holdKey = IntArray(numChannels)
            keyRow = IntArray(numChannels)
        }

        val ci = channelInfo.value
        val fi = frameInfo.value
        val buffers = sampleBuffers!!

        for (chn in 0 until numChannels) {
            val ins = ci.instruments[chn]
            val period = ci.periods[chn]
            val row = fi.row
            var key = ci.keys[chn]

            if (key >= 0) {
                holdKey[chn] = key
                if (keyRow[chn] == row) {
                    key = -1
                } else {
                    keyRow[chn] = row
                }
            }

            Xmp.getSampleData(
                key >= 0,
                ins,
                holdKey[chn],
                period,
                chn,
                Xmp.MAX_BUFFERS,
                buffers[chn]
            )
        }

        // Only copy buffers when emitting new state
        sampleData.value = SampleDataState(
            buffers = buffers.map { it.copyOf() }.toPersistentList()
        )
    }

    private fun resetSampleData() {
        holdKey = IntArray(Xmp.MAX_CHANNELS)
        keyRow = IntArray(Xmp.MAX_CHANNELS)
        sampleBuffers = null
        sampleData.value = SampleDataState()
    }

    // endregion

    // region Pattern Data (optimized)

    private fun updatePatternData(range: IntRange) {
        if (range.isEmpty()) return

        val fi = frameInfo.value
        if (fi.numRows == 0) return

        if (cachedPattern != fi.pattern) {
            cachedPatternRows.clear()
            cachedPattern = fi.pattern
        }

        var hasNewData = false

        for (row in range) {
            if (row < 0 || row >= fi.numRows) continue
            if (cachedPatternRows.containsKey(row)) continue

            Xmp.getPatternRow(
                pat = fi.pattern,
                row = row,
                rowNotes = tempRowNotes,
                rowInstruments = tempRowInstruments,
                rowFxType = tempRowFxType,
                rowFxParm = tempRowFxParm
            )

            cachedPatternRows[row] = PatternRowData(
                notes = tempRowNotes.copyOf(),
                instruments = tempRowInstruments.copyOf(),
                fxType = tempRowFxType.copyOf(),
                fxParm = tempRowFxParm.copyOf()
            )

            hasNewData = true
        }

        if (hasNewData || patternData.value.currentPattern != fi.pattern) {
            patternData.value = PatternDataState(
                rows = cachedPatternRows.toMap(),
                currentPattern = fi.pattern
            )
        }
    }

    fun resetPatternData() {
        cachedPatternRows.clear()
        cachedPattern = -1
        patternData.value = PatternDataState()
    }

    // endregion

    // region Playlist - TODO

    fun onAddToPlaylist(uri: Uri) {
        Timber.w("TODO: onAddToPlaylist")
    }

    fun clearPlaylist() {
        playlistChoice.value = null
    }

    fun addToPlaylist(index: Int) {
        Timber.w("TODO: addToPlaylist")
    }

    // endregion

    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
    }
}
