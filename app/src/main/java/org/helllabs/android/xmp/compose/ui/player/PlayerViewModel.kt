package org.helllabs.android.xmp.compose.ui.player

import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazygeniouz.dfc.file.DocumentFileCompat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.model.ChannelInfo
import org.helllabs.android.xmp.model.FrameInfo
import org.helllabs.android.xmp.model.ModVars
import org.helllabs.android.xmp.model.Playlist
import org.helllabs.android.xmp.model.SequenceVars
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

enum class RepeatMode {
    OFF,
    REPEAT_ALL,
    REPEAT_ONE
}

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
    val numOfSequences: List<Int> = listOf(),
    val currentSequence: Int = 0
)

@Immutable
data class PlayerActivityState(
    val fileList: List<Uri> = listOf(),
    val keepFirst: Boolean = false,
    val loopListMode: Boolean = false,
    val playTime: Float = 0F,
    val shuffleMode: Boolean = false,
    val start: Int = 0,
    val totalTime: Int = 0
)

@Immutable
data class ChannelMuteState(val isMuted: ImmutableList<Boolean> = persistentListOf()) {
    operator fun get(index: Int) = isMuted[index]
    fun count(predicate: (Boolean) -> Boolean) = isMuted.count(predicate)
}

class PlayerViewModel(prefManager: PrefManager) : ViewModel() {

    private val _activityState = MutableStateFlow(PlayerActivityState())
    val activityState = _activityState.asStateFlow()

    /** Player Variables **/
    private val _uiState = MutableStateFlow(PlayerState())
    val uiState = _uiState.asStateFlow()

    private val _infoState = MutableStateFlow(PlayerInfoState())
    val infoState = _infoState.asStateFlow()

    private val _buttonState = MutableStateFlow(PlayerButtonsState())
    val buttonState = _buttonState.asStateFlow()

    private val _timeState = MutableStateFlow(PlayerTimeState())
    val timeState = _timeState.asStateFlow()

    private val _drawerState = MutableStateFlow(PlayerSheetState())
    val drawerState = _drawerState.asStateFlow()

    val isPlaying: Boolean
        get() = _buttonState.value.isPlaying

    /** Viewer Variables **/
    private val seqVars = MutableStateFlow(SequenceVars())

    private val _insName = MutableStateFlow(persistentListOf(""))
    val insName = _insName.asStateFlow()

    private val _modVars = MutableStateFlow(ModVars())
    val modVars = _modVars.asStateFlow()

    private val _frameInfo = MutableStateFlow(FrameInfo())
    val frameInfo = _frameInfo.asStateFlow()

    private val _channelInfo = MutableStateFlow(ChannelInfo())
    val channelInfo = _channelInfo.asStateFlow()

    private val _playlistChoice = MutableStateFlow<DocumentFileCompat?>(null)
    val playlistChoice = _playlistChoice.asStateFlow()

    private val _isMuted = MutableStateFlow(ChannelMuteState())
    val isMuted = _isMuted.asStateFlow()

    private val _softError = MutableSharedFlow<String>()
    val softError = _softError.asSharedFlow()

    private val showHex = prefManager.showHexFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /** Player Functions **/

    fun onConnected(value: Boolean) {
        _uiState.update { it.copy(serviceConnected = value) }
    }

    fun toggleLoop(value: RepeatMode) {
        _buttonState.update { it.copy(repeatMode = value) }
    }

    fun isPlaying(value: Boolean) {
        _buttonState.update { it.copy(isPlaying = value) }
    }

    fun screenOn(value: Boolean) {
        _uiState.update { it.copy(screenOn = value) }
    }

    fun showInfoLine(value: Boolean) {
        _timeState.update { it.copy(isVisible = value) }
        _infoState.update { it.copy(isVisible = value) }
    }

    fun isSeeking(value: Boolean) {
        Timber.w("Trying to seek: $value")
        _timeState.update { it.copy(isSeeking = value) }
    }

    fun onAllSequence(value: Boolean) {
        _drawerState.update { it.copy(isPlayAllSequences = value) }
    }

    fun onSequence(value: Int) {
        _drawerState.update { it.copy(currentSequence = value) }
    }

    /** Viewer Functions **/
    fun changeViewer() {
        val current = (_uiState.value.currentViewer + 1) % 3
        _uiState.update { it.copy(currentViewer = current) }
    }

    fun updateSeekBar() {
        if (!timeState.value.isSeeking && activityState.value.playTime >= 0) {
            _timeState.update {
                it.copy(seekPos = activityState.value.playTime)
            }
        }
    }

    fun showNewSequence(showSnack: (Int) -> Unit) {
        val time = modVars.value.seqDuration

        _activityState.update {
            it.copy(totalTime = time / 1000)
        }

        _timeState.update {
            it.copy(seekPos = 0f, seekMax = time.div(100f))
        }

        _drawerState.update {
            it.copy(currentSequence = modVars.value.currentSequence)
        }

        showSnack(time)
    }

    fun showNewMod(modPlayer: PlayerService, skipToPrevious: Boolean) {
        Timber.i("Show new module | Previous: $skipToPrevious")

        val mVars = ModVars()
        Xmp.getModVars(mVars)
        _modVars.update { mVars }

        val sVars = SequenceVars()
        Xmp.getSeqVars(sVars)
        seqVars.update { sVars }

        _drawerState.update {
            it.copy(
                moduleInfo = listOf(
                    modVars.value.numPatterns,
                    modVars.value.numInstruments,
                    modVars.value.numSamples,
                    modVars.value.numChannels,
                    modVars.value.lengthInPatterns
                ),
                isPlayAllSequences = modPlayer.playAllSequences,
                currentSequence = 0,
                numOfSequences = seqVars.value.sequence.toList()
            )
        }

        _activityState.update {
            it.copy(
                playTime = mVars.seqDuration.div(100F),
                totalTime = mVars.seqDuration / 1000
            )
        }

        _timeState.update {
            it.copy(
                seekPos = _activityState.value.playTime,
                seekMax = mVars.seqDuration.div(100F)
            )
        }

        val mode = if (modPlayer.isLoopPlaylist) {
            RepeatMode.REPEAT_ALL
        } else if (modPlayer.isRepeating) {
            RepeatMode.REPEAT_ONE
        } else {
            RepeatMode.OFF
        }
        toggleLoop(mode)

        val name: String = Xmp.getModName().trim().ifEmpty { modPlayer.getFileName() }
        val type: String = Xmp.getModType()
        _uiState.update {
            it.copy(
                infoTitle = name,
                infoType = type,
                skipToPrevious = skipToPrevious
            )
        }

        if (_uiState.value.serviceConnected) {
            Xmp.getModVars(modVars.value)
            Xmp.getSeqVars(seqVars.value)

            _insName.update {
                val instruments = Xmp.getInstruments() ?: Array(modVars.value.numInstruments) { "" }
                instruments.toPersistentList()
            }

            _isMuted.update {
                ChannelMuteState(
                    isMuted = List(modVars.value.numChannels) { i ->
                        Xmp.mute(i, -1) == 1
                    }.toPersistentList()
                )
            }
        }
    }

    fun resetPlayTime() {
        _activityState.update {
            it.copy(playTime = 0F)
        }
    }

    fun setActivityState(
        fileList: List<Uri>,
        shuffleMode: Boolean,
        loopListMode: Boolean,
        keepFirst: Boolean,
        start: Int
    ) {
        _activityState.update {
            it.copy(
                fileList = fileList,
                shuffleMode = shuffleMode,
                loopListMode = loopListMode,
                keepFirst = keepFirst,
                start = start,
            )
        }
    }

    fun setPlayTime(time: Float) {
        _activityState.update {
            it.copy(playTime = time)
        }
    }

    fun showSheet(value: Boolean) {
        _uiState.update {
            it.copy(showInfoDialog = value)
        }
    }

    fun closeMessage() {
        showMessage(false, "")
    }

    fun showMessage(value: Boolean, message: String) {
        _uiState.update {
            it.copy(showMessageDialog = value, currentMessage = message)
        }
    }

    fun updateInfoTime() {
        val time = Xmp.time() / 1000

        _timeState.update {
            it.copy(
                timeNow = Util.updateTime(time),
                timeTotal = Util.updateTime(activityState.value.totalTime)
            )
        }
    }

    fun updateInfoState() {
        val fi = frameInfo.value
        _infoState.update {
            it.copy(
                infoPat = Util.updateFrameInfo(showHex.value, fi.pattern),
                infoPos = Util.updateFrameInfo(showHex.value, fi.pos),
                infoBpm = Util.updateFrameInfo(showHex.value, fi.bpm),
                infoSpeed = Util.updateFrameInfo(showHex.value, fi.speed)
            )
        }
    }

    fun updateModVars() {
        val modVars = ModVars()
        Xmp.getModVars(modVars)
        _modVars.update { modVars }
    }

    fun updateViewInfo() {
        val ci = ChannelInfo()
        Xmp.getChannelData(ci)
        _channelInfo.update { ci }

        val fi = FrameInfo()
        Xmp.getInfo(fi)
        _frameInfo.update { fi }

        _isMuted.update {
            ChannelMuteState(
                isMuted = List(modVars.value.numChannels) { i ->
                    Xmp.mute(i, -1) == 1
                }.toPersistentList()
            )
        }
    }

    fun onAddToPlaylist(uri: Uri) {
        // TODO
        Timber.w("TODO")
    }

    fun clearPlaylist() {
        _playlistChoice.value = null
    }

    fun addToPlaylist(index: Int) {
        // TODO
        Timber.w("TODO")
    }
}
