package org.helllabs.android.xmp.ui.playlist_selected

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.listItemsSingleChoice
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import kotlinx.coroutines.flow.collectLatest
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.model.PlaylistType
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.player.PlayerUtil
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.util.*

@Composable
fun SelectedPlaylist(
    navController: NavController,
    bindService: () -> Unit,
    viewModel: SelectedPlaylistViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var clickPosition by remember { mutableStateOf(-1) }

    // TODO: Test this
    val playThisModule = {
        val item = viewModel.mPlaylistAdapter.currentList[clickPosition].file!!.path
        PlayerUtil.playModule(
            context = context,
            modList = item.toList(),
            isLoop = viewModel.isLoopMode,
            isShuffle = viewModel.isShuffleMode,
            keepFirst = true
        )

        clickPosition = -1
    }

    // TODO: Test this
    val playAllStartingHere = {
        val list = viewModel.mPlaylistAdapter.currentList.map { it.file!!.path }
        PlayerUtil.playModule(
            context = context,
            modList = list,
            start = clickPosition,
            isLoop = viewModel.isLoopMode,
            isShuffle = viewModel.isShuffleMode,
            keepFirst = true
        )

        clickPosition = -1
    }

    // TODO: Test this
    val removeItemFromPlaylist = {
        viewModel.mPlaylist.remove(clickPosition)
        viewModel.mPlaylist.setListChanged(true)
        viewModel.mPlaylist.commit()
        viewModel.mPlaylistAdapter.update()
        clickPosition = -1
    }

    val longClickState = rememberMaterialDialogState()
    MaterialDialog(
        dialogState = longClickState,
        buttons = {
            positiveButton(res = R.string.select)
            negativeButton(res = R.string.cancel)
        }
    ) {
        val listItems = context.resources.getStringArray(R.array.playlist_item_array)
        title(res = R.string.dialog_playlist_edit_title)
        listItemsSingleChoice(list = listItems.toList()) { item ->
            when (item) {
                0 -> {
                    // Play this module
                    playThisModule()
                }
                1 -> {
                    // Play all, starting here
                    playAllStartingHere()
                }
                2 -> {
                    // Remove from playlist
                    removeItemFromPlaylist()
                }
            }
        }
    }

    LaunchedEffect(true) {
        viewModel.uiState.collectLatest { event ->
            when (event) {
                is SelectedUiEvent.OnClick -> {
                    clickPosition = event.position
                    playAllStartingHere()
                }
                is SelectedUiEvent.OnLongClick -> {
                    clickPosition = event.position
                    longClickState.show()
                }
                is SelectedUiEvent.OnStartDrag,
                is SelectedUiEvent.OnStopDrag -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        }
    }

    DisposableEffect(viewModel) {
        viewModel.onEvent(SelectedEvent.OnStart)
        onDispose {
            viewModel.onEvent(SelectedEvent.OnStop)
        }
    }

    PlaylistActivityScreen(
        onBack = { navController.popBackStack() },
        playlist = viewModel.mPlaylist,
        mPlaylistAdapter = viewModel.mPlaylistAdapter,
        touchHelper = viewModel.mItemTouchHelper,
        onPlay = {
            clickPosition = 0
            playAllStartingHere()
        },
    )
}

@Composable
private fun PlaylistActivityScreen(
    onBack: () -> Unit,
    playlist: Playlist,
    mPlaylistAdapter: PlaylistAdapter,
    touchHelper: ItemTouchHelper,
    onPlay: (list: List<String>) -> Unit,
) {
    var isLoop: Boolean by remember { mutableStateOf(playlist.isLoopMode) }
    var isShuffle: Boolean by remember { mutableStateOf(playlist.isShuffleMode) }

    // Because Compose has no Drag 'n Drop
    val recyclerView: @Composable (modifier: Modifier) -> Unit = { modifier ->
        AndroidView(
            modifier = modifier,
            factory = { context ->
                RecyclerView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    adapter = mPlaylistAdapter
                    layoutManager = LinearLayoutManager(context)
                    setHasFixedSize(true)
                }.also {
                    touchHelper.attachToRecyclerView(it)
                }
            },
        )
    }

//    val uiController = rememberSystemUiController()
//    SideEffect {
//        uiController.setNavigationBarColor(color = sectionBackgroundDark)
//    }

    PlaylistActivityLayout(
        onBack = { onBack() },
        name = playlist.name,
        comment = playlist.comment,
        currentList = mPlaylistAdapter.currentList,
        recyclerView = { recyclerView(it) },
        isLoop = isLoop,
        isShuffle = isShuffle,
        onLoop = {
            isLoop = it
            playlist.isLoopMode = it
        },
        onShuffle = {
            isShuffle = it
            playlist.isShuffleMode = it
        },
        onPlay = {
            val list = mPlaylistAdapter.currentList
                .filter { it.type == PlaylistType.TYPE_FILE }
                .map { it.file!!.path }

            onPlay(list)
        },
    )
}

// TODO: Composed List ordering is coming soon.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistActivityLayout(
    onBack: () -> Unit,
    name: String,
    comment: String?,
    currentList: List<PlaylistItem>,
    recyclerView: @Composable (modifier: Modifier) -> Unit,
    isLoop: Boolean,
    isShuffle: Boolean,
    onPlay: () -> Unit,
    onLoop: (value: Boolean) -> Unit,
    onShuffle: (value: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    Scaffold(
        topBar = {
            Column {
                XmpAppBar3(
                    titleText = stringResource(id = R.string.browser_playlist_title),
                    scrollBehavior = scrollBehavior,
                    onNavIconPressed = onBack,
                )
                ScrollableInfoBar(
                    playlist = name,
                    comment = comment,
                )
            }
        },
        bottomBar = {
            LayoutControls(
                modifier = Modifier.navigationBarsPadding(),
                onPlay = {
                    if (currentList.isEmpty()) {
                        context.toast(R.string.error_no_files_to_play)
                        return@LayoutControls
                    }
                    onPlay()
                },
                onLoop = { onLoop(!isLoop) },
                onShuffle = { onShuffle(!isShuffle) },
                isLoopEnabled = isLoop,
                isShuffleEnabled = isShuffle,
            )
        }
    ) { contentPadding ->
        Box {
            recyclerView(
                modifier = Modifier
                    .padding(contentPadding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            )

            if (currentList.isEmpty()) {
                ErrorLayout(
                    modifier = Modifier.padding(contentPadding),
                    message = stringResource(id = R.string.empty_playlist)
                )
            }
        }
    }
}

// TODO would like to hide/collapse this on scroll.
@Composable
fun ScrollableInfoBar(
    playlist: String,
    comment: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.waterfallPadding(),
            text = playlist,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            modifier = Modifier.waterfallPadding(),
            text = if (comment.isNullOrEmpty()) stringResource(id = R.string.no_comment)
            else comment,
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
    Divider(color = MaterialTheme.colorScheme.inverseSurface)
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Light Theme", uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun PlaylistActivityPreview() {
    XmpTheme3 {
        PlaylistActivityLayout(
            onBack = { },
            name = "Some Playlist Name",
            comment = "Some Playlist Comment",
            currentList = fakeDataPlaylistDetail(),
            recyclerView = {},
            isLoop = true,
            isShuffle = true,
            onPlay = {},
            onLoop = {},
            onShuffle = {}
        )
    }
}
