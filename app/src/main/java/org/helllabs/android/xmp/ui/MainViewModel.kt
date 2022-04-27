package org.helllabs.android.xmp.ui

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.model.Module
import org.helllabs.android.xmp.service.*
import timber.log.Timber

data class MainScreenState(
    val currentModule: Module? = null,
    val playingState: PlayingState = PlayingState.NONE
) {
    private var state = playingState == PlayingState.PLAYING || playingState == PlayingState.PAUSED

    val isPlayerBarVisible = currentModule != null && state

    val isMusicPlaying = playingState == PlayingState.PLAYING
}

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val useCase: PlayerUseCase,
) : AndroidViewModel(application) {

    private val _uiState = mutableStateOf(MainScreenState())
    val uiState: State<MainScreenState> = _uiState

    private val _navigateToMusicScreen = MutableSharedFlow<Boolean>()
    val navigateToMusicScreen = _navigateToMusicScreen.asSharedFlow()

    private val currentSong = useCase.currentSong
    private val playBackState = useCase.playbackState

    init {
        collectCurrentSong()
        collectPlayBackState()
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared")
        useCase.unsubscribeToService(PLAYLIST_ROOT_ID)
    }

    private fun collectCurrentSong() = viewModelScope.launch {
        currentSong.collectLatest {
            val module = it?.getModule()
            _uiState.value = uiState.value.copy(currentModule = module)
        }
    }

    private fun collectPlayBackState() = viewModelScope.launch {
        playBackState.collectLatest { playback ->
            val musicState = playback?.getMusicState() ?: PlayingState.NONE
            _uiState.value = uiState.value.copy(playingState = musicState)
        }
    }

    /* Player Bar Actions */

    fun onPlayPause(module: Module) = viewModelScope.launch {
        useCase.playPause(module.id, true)
    }

    fun onForward() = viewModelScope.launch {
        useCase.skipToNextTrack()
    }

    fun onPrevious() = viewModelScope.launch {
        useCase.skipToPrevTrack()
    }

    fun onPlayerBarPressed() = viewModelScope.launch {
        _navigateToMusicScreen.emit(true)
    }

    fun onPlayerBarDismissed() = viewModelScope.launch {
        useCase.stopPlaying()
    }
}
