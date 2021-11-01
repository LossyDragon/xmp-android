package org.helllabs.android.xmp.ui.playlists

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.accompanist.insets.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.filelist.FilelistActivity
import org.helllabs.android.xmp.ui.modarchive.Search
import org.helllabs.android.xmp.ui.player.PlayerActivity
import org.helllabs.android.xmp.ui.playlistDetail.PlaylistActivity
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.ui.preferences.Preferences
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.ui.theme.themedText
import org.helllabs.android.xmp.util.*
import java.util.*

class PlaylistMenu : ComponentActivity() {

    val viewModel: PlaylistMenuViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        logI("Start application")
        setContent {
            val playlistState = viewModel.playlistState.collectAsState()
            val permissions = rememberMultiplePermissionsState(
                permissions = listOf(
                    READ_EXTERNAL_STORAGE,
                    WRITE_EXTERNAL_STORAGE
                )
            )

            ProvideWindowInsets(consumeWindowInsets = false) {
                PlaylistMenuScreen(
                    viewModel = viewModel,
                    playlistState = playlistState,
                    permissionsState = permissions,
                )
            }
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
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PlaylistMenuScreen(
    viewModel: PlaylistMenuViewModel,
    playlistState: State<PlaylistMenuViewModel.PlaylistMenuState>,
    permissionsState: MultiplePermissionsState,
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
        context.launchActivity(intent)
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

    when {
        permissionsState.allPermissionsGranted -> {
            // All Perms Granted

            //TODO
            DialogShowChangelog {
                println("Hello World!")
            }


//            context.showChangeLog(lifecycleOwner) {
//                val name = stringResource(R.string.empty_playlist)
//                val comment = getString(R.string.empty_comment)
//                when (viewModel.setupDataDir(name, comment)) {
//                    0 -> Unit // Success
//                    -1 -> dialogMessage(
//                        lifecycleOwner = this,
//                        message = getString(R.string.error_create_playlist),
//                    )
//                    -2 -> dialogMessage(
//                        lifecycleOwner = this,
//                        title = R.string.error,
//                        message = getString(R.string.error_datadir),
//                        positiveButtonText = R.string.exit,
//                        block = { finish() }
//                    )
//                }
//            }

            PlaylistsContent(
                playlistState = playlistState.value,
                onClick = { item, index -> onClick(item, index) },
                onLongClick = { item, index -> onLongClick(item, index) },
                onFabClicked = {
                    val intent = Intent(context, PlaylistEdit::class.java)
                    resultAdd.launch(intent)
                    (context as Activity).overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    )
                }
            )
        }
        permissionsState.shouldShowRationale ||
            !permissionsState.permissionRequested -> {
            // Need Permissions
            XmpTheme3 {
                Surface {
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp)
                            .fillMaxSize()
                            .systemBarsPadding(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val appName = stringResource(id = R.string.app_name)
                        Text("The following permissions is needed for\n$appName to run properly: \n")
                        Text(getPermissionsText(permissionsState.revokedPermissions))
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { permissionsState.launchMultiplePermissionRequest() }
                        ) {
                            Text(
                                text = "Request permission",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
        else -> {
            // Permissions most-likely permanently denied.
            XmpTheme3 {
                Surface {
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp)
                            .fillMaxSize()
                            .systemBarsPadding(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Permissions were denied.\nPlease manually grant permissions in Settings.\n")
                        Text(getPermissionsText(permissionsState.revokedPermissions))
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.fromParts("package", context.packageName, null)
                                context.startActivity(intent)
                            }
                        ) {
                            Text(
                                text = "Open Settings",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistsContent(
    playlistState: PlaylistMenuViewModel.PlaylistMenuState,
    onFabClicked: () -> Unit,
    onClick: (item: PlaylistItem, index: Int) -> Unit,
    onLongClick: (item: PlaylistItem, index: Int) -> Unit,
) {
    val scrollState = rememberLazyListState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    XmpTheme3 {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            // Top App Bar
            PlaylistMenuAppBar(
                modifier = Modifier.statusBarsPadding(),
                scrollBehavior = scrollBehavior,
            )

            // Content
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val list = remember { mutableStateOf(listOf<PlaylistItem>()) }

                Surface {
                    // State Flow
                    when (playlistState) {
                        PlaylistMenuViewModel.PlaylistMenuState.None -> Unit
                        PlaylistMenuViewModel.PlaylistMenuState.Load -> {
                            ProgressbarIndicator()
                            list.value = listOf()
                        }
                        is PlaylistMenuViewModel.PlaylistMenuState.Loaded -> {
                            list.value = playlistState.list
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = scrollState,
                        contentPadding = rememberInsetsPaddingValues(
                            insets = LocalWindowInsets.current.navigationBars,
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

                PlaylistsFab(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    extended = scrollState.firstVisibleItemIndex == 0,
                    onFabClicked = onFabClicked
                )
            }
        }
    }
}

@Composable
private fun PlaylistMenuAppBar(
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val context = LocalContext.current

    XmpAppBar3(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        title = {
            AppBarText(
                title = themedText(R.string.app_name),
                titleClick = { startPlayerActivity(context) }
            )
        },
        actions = {
            PlaylistMenuItems(
                downloadClick = {
                    val intent = Intent(context, Search::class.java)
                    context.launchActivity(intent)
                },
                settingsClick = {
                    val intent = Intent(context, Preferences::class.java)
                    context.launchActivity(intent)
                }
            )
        },
    )
}

/****************************
 * Utility related functions
 ****************************/

private fun Context.launchActivity(intent: Intent) {
    startActivity(intent)
    (this as Activity).overridePendingTransition(
        R.anim.slide_in_right,
        R.anim.slide_out_left
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


@OptIn(ExperimentalPermissionsApi::class)
private fun getPermissionsText(permissions: List<PermissionState>): AnnotatedString {
    val revokedPermissionsSize = permissions.size

    val permissionsString = buildAnnotatedString {
        if (revokedPermissionsSize == 0)
            append("")

        for (perm in permissions.indices) {
            append(permissions[perm].permission.substringAfterLast('.'))
            when (perm) {
                revokedPermissionsSize - 1 -> {
                    append(" ")
                }
                else -> {
                    append(",\n")
                }
            }
        }
    }

    return permissionsString
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PlaylistMenuPreview() {
    val state = PlaylistMenuViewModel.PlaylistMenuState.Loaded(fakeDataPlaylistMenu())
    PlaylistsContent(
        playlistState = state,
        onFabClicked = {},
        onClick = { _, _ -> },
        onLongClick = { _, _ -> }
    )
}
