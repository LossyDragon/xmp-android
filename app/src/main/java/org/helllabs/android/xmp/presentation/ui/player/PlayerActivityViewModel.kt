package org.helllabs.android.xmp.presentation.ui.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlayerActivityViewModel : ViewModel() {

    private val _infoSpeed = MutableLiveData<String>()
    val infoSpeed: LiveData<String> = _infoSpeed
    fun setInfoSpeed(value: String) {
        _infoSpeed.value = value
    }

    private val _infoBpm = MutableLiveData<String>()
    val infoBpm: LiveData<String> = _infoBpm
    fun setInfoBpm(value: String) {
        _infoBpm.value = value
    }

    private val _infoPos = MutableLiveData<String>()
    val infoPos: LiveData<String> = _infoPos
    fun setInfoPos(value: String) {
        _infoPos.value = value
    }

    private val _infoPat = MutableLiveData<String>()
    val infoPat: LiveData<String> = _infoPat
    fun setInfoPat(value: String) {
        _infoPat.value = value
    }

    private val _timeNow = MutableLiveData<String>()
    val timeNow: LiveData<String> = _timeNow
    fun setTimeNow(value: String) {
        _timeNow.value = value
    }

    private val _timeTotal = MutableLiveData<String>()
    val timeTotal: LiveData<String> = _timeTotal
    fun setTimeTotal(value: String) {
        _timeTotal.value = value
    }

    private val _seekMax = MutableLiveData<Float>()
    val seekMax: LiveData<Float> = _seekMax
    fun setSeekMax(value: Float) {
        _seekMax.value = value
    }

    private val _seekPos = MutableLiveData<Float>()
    val seekPos: LiveData<Float> = _seekPos
    fun setSeekPos(value: Float) {
        _seekPos.value = value
    }

    private val _setPlaying = MutableLiveData<Boolean>()
    val setPlaying: LiveData<Boolean> = _setPlaying
    fun setPlaying(value: Boolean) {
        _setPlaying.value = value
    }

    private val _setRepeat = MutableLiveData<Boolean>()
    val setRepeat: LiveData<Boolean> = _setRepeat
    fun setRepeat(value: Boolean) {
        _setRepeat.value = value
    }

    private val _setDetails = MutableLiveData(listOf(0, 0, 0, 0))
    val setDetails: LiveData<List<Int>> = _setDetails
    fun setDetails(pat: Int, ins: Int, smp: Int, chn: Int) {
        _setDetails.value = listOf(pat, ins, smp, chn)
    }

    private val _setAllSequences = MutableLiveData(false)
    val setAllSequences: LiveData<Boolean> = _setAllSequences
    fun setAllSequences(value: Boolean) {
        _setAllSequences.value = value
    }

    private val _numOfSequences = MutableLiveData(listOf(1))
    val numOfSequences: LiveData<List<Int>> = _numOfSequences
    fun numOfSequences(value: List<Int>) {
        _numOfSequences.value = value
    }

    private val _currentSequence = MutableLiveData(0)
    val currentSequence: LiveData<Int> = _currentSequence
    fun currentSequence(value: Int) {
        _currentSequence.value = value
    }
}
