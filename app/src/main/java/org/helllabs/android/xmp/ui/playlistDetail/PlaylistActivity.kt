package org.helllabs.android.xmp.ui.playlistDetail

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.accompanist.insets.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.io.IOException
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.ui.BasePlaylistActivity
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.playlistDetail.recyclerview.OnStartDragListener
import org.helllabs.android.xmp.ui.playlistDetail.recyclerview.SimpleItemTouchHelperCallback
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.ui.theme.sectionBackgroundDark
import org.helllabs.android.xmp.util.PlaylistUtils
import org.helllabs.android.xmp.util.logE
import org.helllabs.android.xmp.util.toList
import org.helllabs.android.xmp.util.toast

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
            ProvideWindowInsets {
                val uiController = rememberSystemUiController()
                SideEffect {
                    uiController.setNavigationBarColor(color = sectionBackgroundDark)
                }

                PlaylistActivityScreen(
                    onBack = { onBackPressed() },
                    playlist = mPlaylist,
                    mPlaylistAdapter = mPlaylistAdapter,
                    touchHelper = mItemTouchHelper,
                    onPlay = { playModule(it) },
                )
            }
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

    XmpTheme3 {
        Scaffold(
            topBar = {
                // Top App Bar
                val rotation = LocalConfiguration.current.orientation
                val appBarModifier =
                    if (rotation == Configuration.ORIENTATION_PORTRAIT) Modifier.statusBarsPadding()
                    else Modifier.systemBarsPadding()

                Column {
                    XmpAppBar3(
                        modifier = appBarModifier,
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
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            text = playlist,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
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
