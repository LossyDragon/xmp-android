package org.helllabs.android.xmp.ui.playlistMenu

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.*
import android.view.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import java.util.*
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.filelist.FilelistActivity
import org.helllabs.android.xmp.ui.modarchive.Search
import org.helllabs.android.xmp.ui.player.PlayerActivity
import org.helllabs.android.xmp.ui.playlist_detail.PlaylistActivity
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.preferences.Preferences
import org.helllabs.android.xmp.ui.theme.XmpTheme
import org.helllabs.android.xmp.ui.theme.themedText
import org.helllabs.android.xmp.ui.util.dialogMessage
import org.helllabs.android.xmp.ui.util.showChangeLog
import org.helllabs.android.xmp.ui.util.toast
import org.helllabs.android.xmp.util.*

class PlaylistMenu : ComponentActivity() {

    val viewModel: PlaylistMenuViewModel by viewModels()

    private var resultPermissions = registerForActivityResult(RequestMultiplePermissions()) {
        if (it[WRITE_EXTERNAL_STORAGE] == true && it[READ_EXTERNAL_STORAGE] == true) {
            logD("Perms Granted: ${it.entries}")
            showChangeLog(this) {
                val name = getString(R.string.empty_playlist)
                val comment = getString(R.string.empty_comment)
                when (viewModel.setupDataDir(name, comment)) {
                    0 -> Unit // Success
                    -1 -> dialogMessage(
                        lifecycleOwner = this,
                        message = getString(R.string.error_create_playlist),
                    )
                    -2 -> dialogMessage(
                        lifecycleOwner = this,
                        title = R.string.error,
                        message = getString(R.string.error_datadir),
                        positiveButtonText = R.string.exit,
                        block = { finish() }
                    )
                }
            }
        } else {
            logW("Perms Not-Granted: ${it.entries}")
            dialogMessage(
                lifecycleOwner = this,
                message = "Permissions Not Granted...",
                block = { finish() }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val permissions = arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)
        resultPermissions.launch(permissions)

        logI("Start application")
        setContent {
            val playlistState = viewModel.playlistState.collectAsState()

            PlaylistMenuScreen(
                viewModel = viewModel,
                playlistState = playlistState,
            )
        }
    }

    public override fun onResume() {
        super.onResume()
        if (!Preferences.checkStorage()) {
            dialogMessage(
                lifecycleOwner = this,
                message = getString(R.string.error_storage),
                block = { finish() }
            )
        }

        // Refresh for any external changes, like setting a new default path.
        viewModel.updateList()
    }
}

@SuppressLint("CheckResult")
@Composable
private fun PlaylistMenuScreen(
    viewModel: PlaylistMenuViewModel,
    playlistState: State<PlaylistMenuViewModel.PlaylistMenuState>,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val resultAdd = rememberLauncherForActivityResult(StartActivityForResult()) {
        if (it.resultCode == AppCompatActivity.RESULT_OK) {
            if (it.data == null) {
                context.toast("Couldn't add playlist")
                return@rememberLauncherForActivityResult
            }

            val data = it.data!!
            val name = data.getStringExtra(PLAYLIST_EDIT_NAME)!!
            val comment = data.getStringExtra(PLAYLIST_EDIT_COMMENT)!!
            if (!viewModel.addPlaylist(name, comment)) {
                context.dialogMessage(
                    lifecycleOwner = lifecycleOwner,
                    message = context.getString(R.string.error_create_playlist),
                )
            }
        }
    }
    val resultEdit = rememberLauncherForActivityResult(StartActivityForResult()) {
        if (it.resultCode == ComponentActivity.RESULT_OK) {
            if (it.data == null) {
                context.toast(R.string.msg_edit_playlist_failed)
                return@rememberLauncherForActivityResult
            }
            val data = it.data!!

            val id = data.getIntExtra(PLAYLIST_EDIT_ID, -1)
            val name = data.getStringExtra(PLAYLIST_EDIT_NAME)!!
            val comment = data.getStringExtra(PLAYLIST_EDIT_COMMENT)!!
            val oldName = data.getStringExtra(PLAYLIST_EDIT_OLD_NAME)

            when (viewModel.editPlaylist(id, name, comment, oldName)) {
                0 -> Unit // Success
                -1 -> context.dialogMessage(
                    lifecycleOwner = lifecycleOwner,
                    message = context.getString(R.string.error_rename_playlist),
                )
                -2 -> context.dialogMessage(
                    lifecycleOwner = lifecycleOwner,
                    message = context.getString(R.string.error_edit_comment),
                )
            }
        }
    }
    val changeDir = {
        val mediaPath = PrefManager.mediaPath
        MaterialDialog(context).show {
            lifecycleOwner(lifecycleOwner)
            title(R.string.dialog_change_dir_title)
            message(R.string.dialog_change_dir_msg)
            input(
                prefill = mediaPath,
                waitForPositiveButton = true,
                allowEmpty = false
            ) { _, text ->
                if (text != mediaPath) {
                    PrefManager.mediaPath = text.toString()
                    viewModel.updateList()
                }
            }
            negativeButton(R.string.cancel)
        }
    }
    val onClick: (item: PlaylistItem, index: Int) -> Unit = { item, index ->
        val intent: Intent =
            if (index == 0) {
                Intent(context, FilelistActivity::class.java)
            } else {
                Intent(context, PlaylistActivity::class.java).apply {
                    putExtra("name", item.name)
                }
            }
        context.startActivity(intent)
        (context as Activity).overridePendingTransition(
            R.anim.slide_in_right,
            R.anim.slide_out_left
        )
    }
    val onLongClick: (item: PlaylistItem, index: Int) -> Unit = { item, index ->
        if (index == 0) {
            changeDir()
        } else {
            val intent = Intent(context, PlaylistEdit::class.java).apply {
                putExtra(PLAYLIST_EDIT_ID, item.id)
                putExtra(PLAYLIST_EDIT_NAME, item.name)
                putExtra(PLAYLIST_EDIT_COMMENT, item.comment)
            }
            resultEdit.launch(intent)
            (context as Activity).overridePendingTransition(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        }
    }

    PlaylistMenuContent(
        playlistState = playlistState.value,
        onClick = { item, index -> onClick(item, index) },
        onLongClick = { item, index -> onLongClick(item, index) },
        onFabClick = {
            val intent = Intent(context, PlaylistEdit::class.java)
            resultAdd.launch(intent)
            (context as Activity).overridePendingTransition(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        }
    )
}

@Composable
private fun PlaylistMenuContent(
    playlistState: PlaylistMenuViewModel.PlaylistMenuState,
    onClick: (item: PlaylistItem, index: Int) -> Unit,
    onLongClick: (item: PlaylistItem, index: Int) -> Unit,
    onFabClick: () -> Unit,
) {
    val context = LocalContext.current
    XmpTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { PlaylistMenuAppBar() },
            floatingActionButton = {
                ExtendedFab(onClick = { onFabClick() })
            }
        ) {
            val list = remember { mutableStateOf(listOf<PlaylistItem>()) }

            // State Flow
            when (playlistState) {
                PlaylistMenuViewModel.PlaylistMenuState.None -> Unit
                PlaylistMenuViewModel.PlaylistMenuState.Load -> {
                    ProgressbarIndicator()
                    list.value = listOf()
                }
                is PlaylistMenuViewModel.PlaylistMenuState.Loaded -> {
                    context.logD("Loaded")
                    list.value = playlistState.list
                }
            }

            LazyColumn(
                contentPadding = rememberInsetsPaddingValues(
                    insets = LocalWindowInsets.current.systemBars,
                    applyTop = false,
                    applyBottom = true,
                    additionalTop = 10.dp,
                    additionalBottom = 80.dp
                )
            ) {
                itemsIndexed(list.value) { index, item ->
                    ItemPlaylistCard(
                        playlist = item,
                        onClick = { onClick(item, index) },
                        onLongClick = { onLongClick(item, index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistMenuAppBar() {
    val context = LocalContext.current
    AppBar(
        annotatedTitle = themedText(res = R.string.app_name),
        menuActions = {
            PlaylistMenuItems(
                downloadClick = {
                    val intent = Intent(context, Search::class.java)
                    context.startActivity(intent)
                    (context as Activity).overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                },
                settingsClick = {
                    val intent = Intent(context, Preferences::class.java)
                    context.startActivity(intent)
                    (context as Activity).overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                }
            )
        },
        titleClick = { startPlayerActivity(context) }
    )
}

private fun startPlayerActivity(context: Context) {
    if (PrefManager.startOnPlayer) {
        if (PlayerService.isPlayerAlive.value == true) {
            val playerIntent = Intent(context, PlayerActivity::class.java)
            context.startActivity(playerIntent)
            (context as Activity).overridePendingTransition(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        } else {
            context.toast(R.string.msg_service_not_alive)
        }
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PlaylistMenuPreview() {
    val state = PlaylistMenuViewModel.PlaylistMenuState.Loaded(fakeDataPlaylistMenu())
    PlaylistMenuContent(
        playlistState = state,
        onClick = { _, _ -> },
        onLongClick = { _, _ -> },
        onFabClick = {},
    )
}
