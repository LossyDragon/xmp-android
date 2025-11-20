package org.helllabs.android.xmp.compose.ui.player

import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import java.util.Collections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.model.ChannelInfo
import org.helllabs.android.xmp.model.FrameInfo
import org.helllabs.android.xmp.model.ModVars
import org.helllabs.android.xmp.model.SequenceVars
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

enum class RepeatMode {
    OFF,
    REPEAT_ALL,
    REPEAT_ONE
}

@Stable
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

@Stable
data class PlayerInfoState(
    val infoSpeed: String = "00",
    val infoBpm: String = "00",
    val infoPos: String = "00",
    val infoPat: String = "00",
    val isVisible: Boolean = true
)

@Stable
data class PlayerButtonsState(
    val isPlaying: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
)

@Stable
data class PlayerTimeState(
    val timeNow: String = "-:--",
    val timeTotal: String = "-:--",
    val seekPos: Float = 0f,
    val seekMax: Float = 1f,
    val isVisible: Boolean = true,
    val isSeeking: Boolean = false
)

@Stable
data class PlayerSheetState(
    val moduleInfo: List<Int> = listOf(0, 0, 0, 0, 0),
    val isPlayAllSequences: Boolean = false,
    val numOfSequences: List<Int> = listOf(),
    val currentSequence: Int = 0
)

@Stable
data class PlayerActivityState(
    val fileList: List<Uri> = listOf(),
    val keepFirst: Boolean = false,
    val loopListMode: Boolean = false,
    val playTime: Float = 0F,
    val shuffleMode: Boolean = false,
    val start: Int = 0,
    val totalTime: Int = 0
)

@Stable
data class ChannelMuteState(val isMuted: BooleanArray = BooleanArray(Xmp.MAX_CHANNELS)) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChannelMuteState

        return isMuted.contentEquals(other.isMuted)
    }

    override fun hashCode(): Int = isMuted.contentHashCode()
}

@Stable
class PlayerViewModel : ViewModel() {

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

    val insName = MutableStateFlow(arrayOf(""))

    private val _isMuted = MutableStateFlow(ChannelMuteState())
    val isMuted = _isMuted.asStateFlow()

    val modVars = MutableStateFlow(ModVars())

    val frameInfo = MutableStateFlow(FrameInfo())

    val channelInfo = MutableStateFlow(ChannelInfo())

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
        modVars.update { mVars }

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

            insName.update {
                Xmp.getInstruments()
                    ?: Collections.nCopies(modVars.value.numInstruments, "").toTypedArray()
            }

            val muteArray = BooleanArray(modVars.value.numChannels) { i ->
                Xmp.mute(i, -1) == 1
            }
            _isMuted.update { it.copy(isMuted = muteArray) }
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
        _infoState.update {
            it.copy(
                infoPat = Util.updateFrameInfo(value = frameInfo.value.pattern),
                infoPos = Util.updateFrameInfo(value = frameInfo.value.pos),
                infoBpm = Util.updateFrameInfo(value = frameInfo.value.bpm),
                infoSpeed = Util.updateFrameInfo(value = frameInfo.value.speed),
            )
        }
    }

    private val lock = Any() // Meh
    fun updateViewInfo() {
        synchronized(lock) {
            val ci = ChannelInfo()
            Xmp.getChannelData(ci)
            channelInfo.update {
                ci
            }

            val fi = FrameInfo()
            Xmp.getInfo(fi)
            frameInfo.update {
                fi
            }

            val muteArray = BooleanArray(modVars.value.numChannels) {
                Xmp.mute(it, -1) == 1
            }
            _isMuted.update { it.copy(isMuted = muteArray) }
        }
    }
}
