package org.helllabs.android.xmp.presentation.ui.playlistMenu

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.toPaddingValues
import java.io.File
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.presentation.components.AppBar
import org.helllabs.android.xmp.presentation.components.ExtendedFab
import org.helllabs.android.xmp.presentation.components.ItemPlaylistCard
import org.helllabs.android.xmp.presentation.components.MainMenuItems
import org.helllabs.android.xmp.presentation.theme.AppTheme
import org.helllabs.android.xmp.presentation.theme.systemDarkTheme
import org.helllabs.android.xmp.presentation.theme.themedText
import org.helllabs.android.xmp.presentation.ui.fileBrowser.FilelistActivity
import org.helllabs.android.xmp.presentation.ui.player.PlayerActivity
import org.helllabs.android.xmp.presentation.ui.playlistDetail.PlaylistActivity
import org.helllabs.android.xmp.presentation.ui.playlistEdit.PlaylistEdit
import org.helllabs.android.xmp.presentation.ui.preferences.Preferences
import org.helllabs.android.xmp.presentation.ui.search.Search
import org.helllabs.android.xmp.presentation.utils.playlist.Playlist
import org.helllabs.android.xmp.presentation.utils.playlist.PlaylistUtils.createEmptyPlaylist
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.*

class PlaylistMenu : ComponentActivity() {

    private val viewModel: PlaylistMenuViewModel by viewModels()

    private val mediaPath: String
        get() = PrefManager.mediaPath

    private val resultRefresh = registerForActivityResult(StartActivityForResult()) {
        logD("Activity Result Refresh")
        viewModel.getPlaylists()
    }
    private val resultManageStorage = registerForActivityResult(StartActivityForResult()) {
        logD("Activity Result Manage Storage")
        if (isAtLeastR) {
            if (Environment.isExternalStorageManager()) {
                setupDataDir()
                viewModel.getPlaylists()
            } else {
                toast(R.string.api_30_perms_denied)
            }
        }
    }
    private val resultAdd = registerForActivityResult(StartActivityForResult()) {
        logD("Activity Result Add")
        addPlaylist(it.data)
    }
    private val resultEdit = registerForActivityResult(StartActivityForResult()) {
        logD("Activity Result Edit")
        editPlaylist(it.data)
    }
    private val permissions = registerForActivityResult(RequestMultiplePermissions()) { perm ->
        var read = false
        var write = false

        perm.entries.forEach {
            logD("${it.key} = ${it.value}")
            if (it.key == READ_EXTERNAL_STORAGE && it.value == true) read = true
            if (it.key == WRITE_EXTERNAL_STORAGE && it.value == true) write = true
        }

        if (read && write) {
            showChangeLog(this) {
                if (isAtLeastR && !Environment.isExternalStorageManager()) {
                    askForApi30Permissions()
                } else {
                    setupDataDir()
                    viewModel.getPlaylists()
                }
            }
        } else {
            errorDialog(this, getString(R.string.permission_denied), R.string.exit) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        checkPermissions()

        logI("Start application")
        setContent {
            val context = LocalContext.current
            val list = viewModel.playlists.observeAsState(mutableListOf())

            if (!Preferences.checkStorage()) {
                context.errorDialog(
                    owner = this,
                    message = stringResource(id = R.string.error_storage),
                    onConfirm = { finish() }
                )
            }

            PlaylistMenuLayout(
                menuDownloadClick = {
                    resultRefresh.launch(Intent(this, Search::class.java))
                },
                menuSettingsClick = {
                    resultRefresh.launch(Intent(this, Preferences::class.java))
                },
                fabClick = {
                    resultAdd.launch(Intent(this, PlaylistEdit::class.java))
                },
                titleClick = { startPlayerActivity() },
                mediaPath = mediaPath,
                playlistItems = list.value,
                playlistClick = { index, item ->
                    val intent: Intent
                    if (index == 0) {
                        intent = Intent(this, FilelistActivity::class.java)
                    } else {
                        intent = Intent(this, PlaylistActivity::class.java)
                        intent.putExtra("name", item.name)
                    }
                    resultRefresh.launch(intent)
                },
                playlistLongClick = { index, item ->
                    if (index == 0) {
                        toast("Long hold a crumb in File Browser to choose a default directory. ")
                    } else {
                        val intent = Intent(this, PlaylistEdit::class.java).apply {
                            putExtra(PlaylistEdit.EXTRA_ID, item.id)
                            putExtra(PlaylistEdit.EXTRA_NAME, item.name)
                            putExtra(PlaylistEdit.EXTRA_COMMENT, item.comment)
                            putExtra(PlaylistEdit.EXTRA_TYPE, item.type)
                        }
                        resultEdit.launch(intent)
                    }
                }
            )
        }
    }

    private fun checkPermissions() {
        permissions.launch(arrayOf(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE))
    }

    // Create application directory and populate with empty playlist
    private fun setupDataDir() {
        if (!Preferences.DATA_DIR.isDirectory) {
            if (Preferences.DATA_DIR.mkdirs()) {
                val name = getString(R.string.empty_playlist)
                val comment = getString(R.string.empty_comment)
                if (!createEmptyPlaylist(name, comment)) {
                    errorDialog(this, getString(R.string.error_create_playlist)) {}
                }
            } else {
                errorDialog(this, getString(R.string.error_datadir)) {
                    finish()
                }
            }
        }
    }

    private fun startPlayerActivity() {
        if (PrefManager.startOnPlayer) {
            if (PlayerService.isPlayerAlive.value == true) {
                val playerIntent = Intent(this, PlayerActivity::class.java)
                startActivity(playerIntent)
            } else {
                toast("Player Service is not running.")
            }
        }
    }

    private fun addPlaylist(data: Intent?) {
        if (data == null) {
            toast("Couldn't add playlist")
            return
        }

        val name = data.getStringExtra(PlaylistEdit.EXTRA_NAME)!!
        val comment = data.getStringExtra(PlaylistEdit.EXTRA_COMMENT)!!
        if (!createEmptyPlaylist(name, comment)) {
            errorDialog(this, getString(R.string.error_create_playlist)) {}
        }

        viewModel.getPlaylists()
    }

    private fun editPlaylist(data: Intent?) {

        if (data == null) {
            toast(R.string.msg_edit_playlist_failed)
            return
        }

        val id = data.getIntExtra(PlaylistEdit.EXTRA_ID, -1)
        val name = data.getStringExtra(PlaylistEdit.EXTRA_NAME)!!
        val comment = data.getStringExtra(PlaylistEdit.EXTRA_COMMENT)!!
        val oldName = data.getStringExtra(PlaylistEdit.EXTRA_OLD_NAME)

        when (id) {
            PlaylistEdit.RESULT_DELETE_PLAYLIST -> Playlist.delete(name)
            PlaylistEdit.RESULT_EDIT_PLAYLIST -> {
                if (!Playlist.rename(oldName!!, name)) {
                    errorDialog(this, getString(R.string.error_rename_playlist)) {}
                    return // Don't attempt to edit comment if failed.
                }

                val file = File(Preferences.DATA_DIR, name + Playlist.COMMENT_SUFFIX)
                if (!Playlist.editComment(file, comment)) {
                    errorDialog(this, getString(R.string.error_edit_comment)) {}
                }
            }
            else -> throw IllegalArgumentException("Edit playlist id was not correct: $id")
        }

        viewModel.getPlaylists()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun askForApi30Permissions() {
        //https://support.google.com/googleplay/android-developer/answer/10467955
        // XMP-Android may fall under the category "Document management apps" or "Exceptions"
        // IMPORTANT: This needs to be approved by google for Play Store submission.
        yesNoDialog(
            owner = this,
            title = R.string.dialog_api_30_title,
            message = getString(R.string.dialog_api_30_message),
            confirmText = R.string.grant,
            dismissText = R.string.cancel,
            onConfirm = {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                resultManageStorage.launch(intent)
            },
        )
    }
}

@Composable
fun PlaylistMenuLayout(
    isDarkTheme: Boolean = systemDarkTheme(),
    menuDownloadClick: () -> Unit,
    menuSettingsClick: () -> Unit,
    fabClick: () -> Unit,
    titleClick: () -> Unit,
    mediaPath: String,
    playlistItems: MutableList<PlaylistItem>,
    playlistClick: (index: Int, item: PlaylistItem) -> Unit,
    playlistLongClick: (index: Int, item: PlaylistItem) -> Unit,
) {

    AppTheme(
        isDarkTheme = isDarkTheme
    ) {
        Scaffold(
            topBar = {
                AppBar(
                    annotatedTitle = themedText(res = R.string.app_name),
                    menuActions = {
                        MainMenuItems(
                            downloadClick = { menuDownloadClick() },
                            settingsClick = { menuSettingsClick() }
                        )
                    },
                    titleClick = { titleClick() }
                )
            },
            floatingActionButton = { ExtendedFab(onClick = { fabClick() }) }
        ) {
            // We're injecting the 'Special' card here because has string resources.
            val special = playlistItems.find { it.type == PlaylistItem.TYPE_SPECIAL }
            if (special != null) {
                special.name = stringResource(id = R.string.playlist_special_title)
                special.comment = stringResource(id = R.string.playlist_special_comment, mediaPath)
            }

            // Handle null or empty comments
            playlistItems.forEach {
                if (it.comment.isNullOrEmpty()) {
                    it.comment = stringResource(id = R.string.no_comment)
                }
            }

            LazyColumn(
                contentPadding = LocalWindowInsets.current.systemBars.toPaddingValues(
                    top = false,
                    bottom = true,
                    additionalTop = 10.dp,
                    additionalBottom = 80.dp
                )
            ) {
                itemsIndexed(items = playlistItems) { index, item ->
                    ItemPlaylistCard(
                        playlist = item,
                        onClick = { playlistClick(index, item) },
                        onLongClick = { playlistLongClick(index, item) }
                    )
                }
            }
        }
    }
}

/************
 * Previews *
 ************/

@Preview
@Composable
fun PlayListMenuPreview() {
    val list = mutableListOf(
        PlaylistItem(PlaylistItem.TYPE_SPECIAL, "File Explorer", "Explorer Comment"),
        PlaylistItem(PlaylistItem.TYPE_PLAYLIST, "Playlist", "Playlist Comment"),
    )
    PlaylistMenuLayout(
        isDarkTheme = false,
        menuDownloadClick = {},
        menuSettingsClick = {},
        fabClick = {},
        titleClick = {},
        mediaPath = "sdcard/mod/",
        playlistItems = list,
        playlistClick = { _, _ -> },
        playlistLongClick = { _, _ -> }
    )
}

@Preview
@Composable
fun PlayListMenuPreviewDark() {
    val list = mutableListOf(
        PlaylistItem(PlaylistItem.TYPE_SPECIAL, "File Explorer", "Explorer Comment"),
        PlaylistItem(PlaylistItem.TYPE_PLAYLIST, "Playlist", "Playlist Comment"),
    )
    PlaylistMenuLayout(
        isDarkTheme = true,
        menuDownloadClick = {},
        menuSettingsClick = {},
        fabClick = {},
        titleClick = {},
        mediaPath = "sdcard/mod/",
        playlistItems = list,
        playlistClick = { _, _ -> },
        playlistLongClick = { _, _ -> }
    )
}
