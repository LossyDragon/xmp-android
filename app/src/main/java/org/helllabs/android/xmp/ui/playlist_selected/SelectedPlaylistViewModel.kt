package org.helllabs.android.xmp.ui.playlist_selected

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.ui.playlist_selected.recyclerview.OnStartDragListener
import org.helllabs.android.xmp.ui.playlist_selected.recyclerview.SimpleItemTouchHelperCallback
import org.helllabs.android.xmp.util.PrefManager

sealed class SelectedEvent {
    object OnStart : SelectedEvent()
    object OnStop : SelectedEvent()
}

sealed class SelectedUiEvent {
    data class OnClick(val position: Int) : SelectedUiEvent()
    data class OnLongClick(val position: Int) : SelectedUiEvent()
    object OnStartDrag : SelectedUiEvent()
    object OnStopDrag : SelectedUiEvent()
}

@HiltViewModel
class SelectedPlaylistViewModel
@Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // This is terrible, but it defeats lifecycle shit
    lateinit var mItemTouchHelper: ItemTouchHelper
    lateinit var mPlaylistAdapter: PlaylistAdapter
    lateinit var mPlaylist: Playlist

    val isShuffleMode: Boolean
        get() = mPlaylist.isShuffleMode
    val isLoopMode: Boolean
        get() = mPlaylist.isLoopMode

    private val dragListener = object : OnStartDragListener {
        override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
            viewModelScope.launch {
                mItemTouchHelper.startDrag(viewHolder)
                _uiState.emit(SelectedUiEvent.OnStartDrag)
            }
        }

        override fun onStopDrag(list: List<PlaylistItem>) {
            viewModelScope.launch {
                mPlaylist.setListChanged(true)
                mPlaylist.updateList(list)
                mPlaylist.commit()
                _uiState.emit(SelectedUiEvent.OnStopDrag)
            }
        }
    }

    private val _uiState = MutableSharedFlow<SelectedUiEvent>()
    val uiState: SharedFlow<SelectedUiEvent> = _uiState.asSharedFlow()

    init {
        savedStateHandle.get<String>("plistName")?.let { name ->
            mPlaylist = Playlist(name)

            mPlaylistAdapter = PlaylistAdapter(mPlaylist.list, PrefManager.useFilename)
            mPlaylistAdapter.dragListener = dragListener

            val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(mPlaylistAdapter)
            mItemTouchHelper = ItemTouchHelper(callback)
        }

        mPlaylistAdapter.onClick = { position ->
            viewModelScope.launch {
                _uiState.emit(SelectedUiEvent.OnClick(position))
            }
        }
        mPlaylistAdapter.onLongClick = { position ->
            viewModelScope.launch {
                _uiState.emit(SelectedUiEvent.OnLongClick(position))
            }
        }
    }

    fun onEvent(event: SelectedEvent) {
        when (event) {
            SelectedEvent.OnStart -> onStart()
            SelectedEvent.OnStop -> onStop()
        }
    }

    private fun onStart() {
        mPlaylistAdapter.setUseFilename(PrefManager.useFilename)
        mPlaylistAdapter.update()
    }

    private fun onStop() {
        mPlaylist.commit()
    }
}
