package org.helllabs.android.xmp.ui.playlistDetail

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.accompanist.insets.navigationBarsPadding
import java.io.IOException
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.ui.BasePlaylistActivity
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.playlistDetail.recyclerview.OnStartDragListener
import org.helllabs.android.xmp.ui.playlistDetail.recyclerview.SimpleItemTouchHelperCallback
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.theme.XmpTheme
import org.helllabs.android.xmp.util.PlaylistUtils
import org.helllabs.android.xmp.util.logE
import org.helllabs.android.xmp.util.toList

class PlaylistActivity :
    BasePlaylistActivity(),
    OnStartDragListener {

    private lateinit var mItemTouchHelper: ItemTouchHelper
    private lateinit var mPlaylistAdapter: PlaylistAdapter
    private lateinit var mPlaylist: Playlist

    override val isShuffleMode: Boolean
        get() = mPlaylist.isShuffleMode
    override val isLoopMode: Boolean
        get() = mPlaylist.isLoopMode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val name = intent.extras?.getString("name") ?: return

        try {
            mPlaylist = Playlist(name)
        } catch (e: IOException) {
            logE("Can't read playlist $name")
            onBackPressed()
        }

        mPlaylistAdapter = PlaylistAdapter(mPlaylist.list, PrefManager.useFilename)
        with(mPlaylistAdapter) {
            onClick = { position ->
                onItemClick(
                    position,
                    mPlaylistAdapter.currentList[position].file!!.path,
                    PlaylistUtils.getDirectoryCount(mPlaylistAdapter.currentList),
                    PlaylistUtils.getFilePathList(mPlaylistAdapter.currentList)
                )
            }
            onLongClick = { position -> onItemLongClick(position) }
            dragListener = this@PlaylistActivity
        }

        val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(mPlaylistAdapter)
        mItemTouchHelper = ItemTouchHelper(callback)

        setContent {
            PlaylistActivityScreen(
                onBack = { onBackPressed() },
                playlist = mPlaylist,
                mPlaylistAdapter = mPlaylistAdapter,
                touchHelper = mItemTouchHelper,
                onPlay = { playModule(it) },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        mPlaylistAdapter.setUseFilename(PrefManager.useFilename)
        mPlaylistAdapter.update()
    }

    public override fun onPause() {
        super.onPause()
        mPlaylist.commit()
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        mItemTouchHelper.startDrag(viewHolder)
    }

    override fun onStopDrag(list: List<PlaylistItem>) {
        mPlaylist.setListChanged(true)
        mPlaylist.updateList(list)
        mPlaylist.commit()
    }

    @SuppressLint("CheckResult")
    private fun onItemLongClick(position: Int) {
        MaterialDialog(this).show {
            title(R.string.dialog_playlist_edit_title)
            listItemsSingleChoice(R.array.edit_playlist_dialog_array) { _, index, _ ->
                when (index) {
                    0 -> {
                        mPlaylist.remove(position)
                        mPlaylist.setListChanged(true)
                        mPlaylist.commit()
                        mPlaylistAdapter.update()
                    }
                    1 -> addToQueue(mPlaylistAdapter.getFilename(position).toList())
                    2 -> addToQueue(PlaylistUtils.getFilePathList(mPlaylistAdapter.currentList))
                    3 -> playModule(mPlaylistAdapter.getFilename(position).toList())
                    4 -> playModule(
                        PlaylistUtils.getFilePathList(mPlaylistAdapter.currentList),
                        position
                    )
                }
            }
            positiveButton(R.string.select)
        }
    }
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
            val list = PlaylistUtils.getFilePathList(mPlaylistAdapter.currentList)
            onPlay(list)
        },
    )
}

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
    XmpTheme(onlyStyleStatusBar = true) {
        val scaffoldState = rememberScaffoldState()
        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(id = R.string.browser_playlist_title),
                    navIconClick = { onBack() },
                )
            },
            scaffoldState = scaffoldState,
            snackbarHost = { scaffoldState.snackbarHostState },
        ) {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxHeight()
                    .navigationBarsPadding()
            ) {
                val (infoBar, customView, controls, snack) = createRefs()

                Column(
                    modifier = Modifier
                        .constrainAs(infoBar) {
                            width = Dimension.fillToConstraints
                            top.linkTo(parent.top, 6.dp)
                        }

                ) {
                    Text(
                        modifier = Modifier.padding(bottom = 3.dp, start = 6.dp, end = 6.dp),
                        text = name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        modifier = Modifier.padding(bottom = 6.dp, start = 6.dp, end = 6.dp),
                        text = if (comment.isNullOrEmpty()) stringResource(id = R.string.no_comment)
                        else comment,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Divider()
                }

                if (currentList.isEmpty()) {
                    ErrorLayout(stringResource(id = R.string.empty_playlist))
                }

                recyclerView(
                    modifier = Modifier.constrainAs(customView) {
                        height = Dimension.fillToConstraints
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(infoBar.bottom)
                        bottom.linkTo(controls.top)
                    }
                )
                Snackbar(
                    modifier = Modifier.constrainAs(snack) {
                        width = Dimension.fillToConstraints
                        bottom.linkTo(controls.top)
                    },
                    snackBarState = scaffoldState.snackbarHostState,
                    onDismiss = {
                        scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                    }
                )
                LayoutControls(
                    modifier = Modifier
                        .constrainAs(controls) {
                            width = Dimension.fillToConstraints
                            top.linkTo(customView.bottom)
                            bottom.linkTo(parent.bottom)
                        },
                    onPlay = {
                        if (currentList.isEmpty()) {
                            scope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(
                                    message = context.getString(R.string.error_no_files_to_play),
                                    actionLabel = context.getString(R.string.ok)
                                )
                            }
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
        }
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Light Theme", uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun PlaylistActivityPreview() {
    PlaylistActivityLayout(
        onBack = { },
        name = "Some Playlist Name",
        comment = "Some Playlist Comment",
        currentList = fakeDataPlaylistDetail(),
        recyclerView = {},
        isLoop = true,
        isShuffle = true,
        onPlay = { },
        onLoop = {},
        onShuffle = {}
    )
}
