package org.helllabs.android.xmp.presentation.ui.playlists

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set this for all Compose activities.
        WindowCompat.setDecorFitsSystemWindows(window, false)

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

            // Show Changelog
            context.showChangeLog(this)

            PlaylistMenuLayout(
                menuDownloadClick = {
                    Intent(this, Search::class.java).also {
                        startActivityForResult(it, REFRESH_REQUEST)
                    }
                },
                menuSettingsClick = {
                    Intent(this, Preferences::class.java).also {
                        startActivityForResult(it, REFRESH_REQUEST)
                    }
                },
                fabClick = {
                    Intent(this, PlaylistEdit::class.java).also {
                        startActivityForResult(it, MOD_ADD_REQUEST)
                    }
                },
                titleClick = { startPlayerActivity() },
                playlistItems = list.value,
                playlistClick = { index, item ->
                    val intent: Intent
                    if (index == 0) {
                        intent = Intent(this, FilelistActivity::class.java)
                    } else {
                        intent = Intent(this, PlaylistActivity::class.java)
                        intent.putExtra("name", item.name)
                    }
                    startActivityForResult(intent, REFRESH_REQUEST)
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
                        startActivityForResult(intent, MOD_EDIT_REQUEST)
                    }
                }
            )
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            setupDataDir()
            viewModel.getPlaylists()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_STORAGE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REFRESH_REQUEST -> viewModel.getPlaylists()
                MOD_ADD_REQUEST -> addPlaylist(data)
                MOD_EDIT_REQUEST -> editPlaylist(data)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getPlaylists()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                setupDataDir()
                viewModel.getPlaylists()
            }
        }
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

    companion object {
        private const val MOD_ADD_REQUEST = 1
        private const val MOD_EDIT_REQUEST = 2
        private const val REFRESH_REQUEST = 3
        private const val REQUEST_WRITE_STORAGE = 112
    }
}

@Composable
fun PlaylistMenuLayout(
    isDarkTheme: Boolean = systemDarkTheme(),
    menuDownloadClick: () -> Unit,
    menuSettingsClick: () -> Unit,
    fabClick: () -> Unit,
    titleClick: () -> Unit,
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
                val path = PrefManager.mediaPath
                special.name = stringResource(id = R.string.playlist_special_title)
                special.comment = stringResource(id = R.string.playlist_special_comment, path)
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
        playlistItems = list,
        playlistClick = { _, _ -> },
        playlistLongClick = { _, _ -> }
    )
}
