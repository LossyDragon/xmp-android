package org.helllabs.android.xmp.presentation.ui.playlistDetail

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
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
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.presentation.components.AppBar
import org.helllabs.android.xmp.presentation.components.DialogSnackbar
import org.helllabs.android.xmp.presentation.components.ErrorLayout
import org.helllabs.android.xmp.presentation.components.LayoutControls
import org.helllabs.android.xmp.presentation.theme.AppTheme
import org.helllabs.android.xmp.presentation.ui.BasePlaylistActivity
import org.helllabs.android.xmp.presentation.utils.internalTextGenerator
import org.helllabs.android.xmp.presentation.utils.playlist.Playlist
import org.helllabs.android.xmp.presentation.utils.playlist.PlaylistUtils
import org.helllabs.android.xmp.presentation.utils.recyclerview.OnStartDragListener
import org.helllabs.android.xmp.presentation.utils.recyclerview.SimpleItemTouchHelperCallback
import org.helllabs.android.xmp.util.logE
import org.helllabs.android.xmp.util.toast

class PlaylistActivity :
    BasePlaylistActivity(),
    OnStartDragListener {

    private lateinit var mItemTouchHelper: ItemTouchHelper
    private lateinit var mPlaylistAdapter: PlaylistAdapter
    private lateinit var mPlaylist: Playlist

    override var isShuffleMode: Boolean
        get() = mPlaylist.isShuffleMode
        set(value) {
            mPlaylist.isShuffleMode = value
        }
    override var isLoopMode: Boolean
        get() = mPlaylist.isLoopMode
        set(value) {
            mPlaylist.isLoopMode = value
        }
    override val allFiles: List<String>
        get() = PlaylistUtils.getFilePathList(mPlaylistAdapter.currentList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val name = intent.extras?.getString("name") ?: return

        try {
            mPlaylist = Playlist(name)
        } catch (e: IOException) {
            logE("Can't read playlist $name")
        }

        mPlaylistAdapter = PlaylistAdapter(listOf(), PrefManager.useFilename)
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
            var isLoop: Boolean by remember { mutableStateOf(isLoopMode) }
            var isShuffle: Boolean by remember { mutableStateOf(isShuffleMode) }

            PlaylistActivityScreen(
                isDarkTheme = isSystemInDarkTheme(),
                onBack = { onBackPressed() },
                name = mPlaylist.name,
                comment = mPlaylist.comment,
                isLoop = isLoop,
                isShuffle = isShuffle,
                mPlaylistAdapter = mPlaylistAdapter,
                touchHelper = mItemTouchHelper,
                onPlay = {
                    val list = allFiles
                    if (list.isEmpty()) {
                        // Sanity check
                        toast(R.string.error_no_files_to_play)
                    } else {
                        playModule(list)
                    }
                },
                onLoop = {
                    isLoop = it // Force recompose
                    isLoopMode = it
                },
                onShuffle = {
                    isShuffle = it // Force recompose
                    isShuffleMode = it
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        mPlaylistAdapter.setUseFilename(PrefManager.useFilename)
        update()
    }

    public override fun onPause() {
        super.onPause()
        mPlaylist.commit()
    }

    public override fun update() {
        mPlaylistAdapter.submitList(mPlaylist.list)
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        mItemTouchHelper.startDrag(viewHolder)
    }

    override fun onStopDrag(playlist: MutableList<PlaylistItem>) {
        mPlaylist.list.clear()
        mPlaylist.list.addAll(playlist)
        mPlaylist.setListChanged(true)
        mPlaylist.commit()
        mPlaylistAdapter.submitList(mPlaylist.list)
    }

    private fun onItemLongClick(position: Int) {
        MaterialDialog(this).show {
            title(R.string.dialog_playlist_edit_title)
            listItemsSingleChoice(R.array.edit_playlist_dialog_array) { _, index, _ ->
                when (index) {
                    0 -> {
                        mPlaylist.remove(position)
                        mPlaylist.setListChanged(true)
                        mPlaylist.commit()
                        update()
                    }
                    1 -> addToQueue(mPlaylistAdapter.getFilename(position))
                    2 -> addToQueue(allFiles)
                    3 -> playModule(mPlaylistAdapter.getFilename(position))
                    4 -> playModule(allFiles, position)
                }
            }
            positiveButton(R.string.select)
        }
    }
}

@Composable
fun PlaylistActivityScreen(
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    name: String,
    comment: String?,
    isLoop: Boolean,
    isShuffle: Boolean,
    onPlay: () -> Unit,
    onLoop: (value: Boolean) -> Unit,
    onShuffle: (value: Boolean) -> Unit,
    mPlaylistAdapter: PlaylistAdapter?, // Only nullable for previewing
    touchHelper: ItemTouchHelper?, // Only nullable for previewing
) {
    AppTheme(
        isDarkTheme = isDarkTheme,
        onlyStyleStatusBar = true,
    ) {
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

                if (mPlaylistAdapter != null && mPlaylistAdapter.currentList.isEmpty()) {
                    ErrorLayout(stringResource(id = R.string.empty_playlist))
                }

                AndroidView(
                    modifier = Modifier
                        .constrainAs(customView) {
                            height = Dimension.fillToConstraints
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            top.linkTo(infoBar.bottom)
                            bottom.linkTo(controls.top)
                        },
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
                            touchHelper?.attachToRecyclerView(it)
                        }
                    },
                )
                DialogSnackbar(
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
                        if (mPlaylistAdapter!!.currentList.isEmpty()) {
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

@Preview
@Composable
fun PlaylistActivityScreenPreview() {
    PlaylistActivityScreen(
        isDarkTheme = false,
        onBack = {},
        name = internalTextGenerator(),
        comment = internalTextGenerator(),
        isLoop = true,
        isShuffle = true,
        onPlay = {},
        onLoop = {},
        onShuffle = {},
        mPlaylistAdapter = null,
        touchHelper = null,
    )
}

@Preview
@Composable
fun PlaylistActivityScreenPreviewDark() {
    PlaylistActivityScreen(
        isDarkTheme = true,
        onBack = {},
        name = internalTextGenerator(),
        comment = internalTextGenerator(),
        isLoop = true,
        isShuffle = true,
        onPlay = {},
        onLoop = {},
        onShuffle = {},
        mPlaylistAdapter = null,
        touchHelper = null,
    )
}
