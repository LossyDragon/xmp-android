package com.lossydragon.media3.ui.screens.downloads.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lossydragon.media3.data.DownloadHistoryRepository
import com.lossydragon.media3.model.Module
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DownloadHistoryViewModel(
    private val repo: DownloadHistoryRepository
) : ViewModel() {

    val history: StateFlow<ImmutableList<Module>>
        field = MutableStateFlow(persistentListOf())

    init {
        viewModelScope.launch {
            history.value = repo.getAll().toPersistentList()
        }
    }

    fun clear() {
        viewModelScope.launch {
            repo.clear()
            history.value = persistentListOf()
        }
    }
}
