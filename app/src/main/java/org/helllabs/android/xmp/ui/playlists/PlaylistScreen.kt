package org.helllabs.android.xmp.ui.playlists

import android.support.v4.media.MediaBrowserCompat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.components.AppBarText
import org.helllabs.android.xmp.ui.components.ProgressbarIndicator
import org.helllabs.android.xmp.ui.components.TopAppBar
import org.helllabs.android.xmp.ui.components.themedText
import org.helllabs.android.xmp.ui.destinations.SelectedScreenDestination
import timber.log.Timber

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@RootNavGraph(start = true)
@Destination
@Composable
fun PlaylistScreen(
    navigator: DestinationsNavigator,
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState

    val scrollState = rememberLazyListState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    val oldName = remember { mutableStateOf("") }
    val oldComment = remember { mutableStateOf("") }
    val removePlaylist = remember { mutableStateOf("") }

    /* Delete Playlist */
    val deletePlaylist = rememberMaterialDialogState()
    DeletePlaylistDialog(
        isShowing = deletePlaylist,
        name = removePlaylist.value,
        onDelete = {
            viewModel.deletePlaylist(removePlaylist.value)
            removePlaylist.value = ""
        }
    )

    /* New Playlist */
    val newPlaylist = rememberMaterialDialogState()
    NewPlaylistDialog(
        isShowing = newPlaylist,
        onConfirm = { name, comment ->
            viewModel.addPlaylist(name, comment)
        }
    )

    /* Edit Playlist */
    val editPlaylist = rememberMaterialDialogState()
    EditPlaylistDialog(
        isShowing = editPlaylist,
        oldName = oldName.value,
        oldComment = oldComment.value,
        onConfirm = { name, comment ->
            viewModel.editPlaylist(oldName.value, name, comment)
            oldName.value = ""
            oldComment.value = ""
        },
        onDelete = { name ->
            removePlaylist.value = name
            deletePlaylist.show()
        }
    )

    /* Click Listeners */
    val onClick: (item: MediaBrowserCompat.MediaItem) -> Unit = {
        Timber.d("onClick: $it")

        val title = it.description.title.toString()
        val comment = it.description.title.toString()
        navigator.navigate(SelectedScreenDestination(title, comment))
    }
    val onOverflowClick: (item: MediaBrowserCompat.MediaItem) -> Unit = {
        Timber.d("onOverflowClick: $it")

        val item = it.description
        oldName.value = item.title.toString()
        oldComment.value = item.description.toString()

        editPlaylist.show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    AppBarText(themedText(R.string.app_name))
                }
            )
        },
        floatingActionButton = {
            PlaylistsFab(
                extended = scrollState.firstVisibleItemIndex == 0,
                onFabClicked = {
                    newPlaylist.show()
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = scrollState,
                contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp),
            ) {
                items(uiState.playlists, key = { it.description.mediaId.toString() }) { item ->
                    val currentItem = item.description
                    PlaylistItemCard(
                        modifier = Modifier.animateItemPlacement(),
                        primaryText = currentItem.title.toString(),
                        secondaryText = currentItem.description.toString()
                            .ifEmpty { "** No Comment **" },
                        onClick = { onClick(item) },
                        onOverflowClick = { onOverflowClick(item) }
                    )
                }
            }

            if (!uiState.isLoading && uiState.playlists.isEmpty()) {
                OutlinedCard {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        fontSize = 18.sp,
                        text = "Empty playlist!"
                    )
                }
            }

            ProgressbarIndicator(uiState.isLoading)
        }
    }
}
