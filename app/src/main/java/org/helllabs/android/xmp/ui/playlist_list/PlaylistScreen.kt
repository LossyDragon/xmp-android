package org.helllabs.android.xmp.ui.playlist_list

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.vanpra.composematerialdialogs.*
import kotlinx.coroutines.flow.collectLatest
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.model.PlaylistItem
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.ui.MainActivity
import org.helllabs.android.xmp.ui.NavScreens
import org.helllabs.android.xmp.ui.components.*
import org.helllabs.android.xmp.ui.player.PlayerActivity
import org.helllabs.android.xmp.ui.theme.XmpTheme3
import org.helllabs.android.xmp.ui.theme.themedText
import org.helllabs.android.xmp.util.*

@Composable
fun PlaylistScreen(
    navController: NavController,
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    var isLoading by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current

    var selectedName by remember { mutableStateOf("") }
    var selectedComment by remember { mutableStateOf("") }

    val playlistCreateState = rememberMaterialDialogState()
    DialogMessage(
        dialogState = playlistCreateState,
        title = R.string.error,
        message = R.string.error_create_playlist,
        positiveButtonText = R.string.ok,
        onPositiveButton = { },
        onDismiss = { playlistCreateState.hide() }
    )

    val playlistRenameState = rememberMaterialDialogState()
    DialogMessage(
        dialogState = playlistRenameState,
        title = R.string.error,
        message = R.string.error_rename_playlist,
        positiveButtonText = R.string.ok,
        onPositiveButton = { },
        onDismiss = { playlistRenameState.hide() }
    )

    val playlistCommentState = rememberMaterialDialogState()
    DialogMessage(
        dialogState = playlistCommentState,
        title = R.string.error,
        message = R.string.error_edit_comment,
        positiveButtonText = R.string.ok,
        onPositiveButton = { },
        onDismiss = { playlistCommentState.hide() }
    )

    val changeDirState = rememberMaterialDialogState()
    ChangeDirDialog(dialogState = changeDirState) {
        context.logD("Change Dir to $it")
        PrefManager.mediaPath = it
        viewModel.onEvent(PlaylistEvent.Refresh)
    }

    val deletePlaylistState = rememberMaterialDialogState()
    DialogMessage(
        dialogState = deletePlaylistState,
        title = R.string.dialog_delete_playlist,
        messageText = stringResource(id = R.string.dialog_delete_playlist_message, selectedName),
        positiveButtonText = R.string.menu_delete,
        negativeButtonText = R.string.cancel,
        onPositiveButton = {
            viewModel.onEvent(PlaylistEvent.Delete(selectedName))
            selectedName = ""
            selectedComment = ""
        },
        onNegativeButton = { deletePlaylistState.hide() }
    )

    val newPlaylistState = rememberMaterialDialogState()
    NewPlaylistDialog(
        dialogState = newPlaylistState,
        buttons = {
            positiveButton(res = R.string.ok)
            negativeButton(res = R.string.cancel)
        }
    ) {
        var newName by remember { mutableStateOf("") }
        var newComment by remember { mutableStateOf("") }
        DialogCallback {
            viewModel.onEvent(PlaylistEvent.Add(newName, newComment))
            newName = ""
            newComment = ""
        }
        customView {
            NewPlaylistLayout(
                name = newName,
                comment = newComment,
                onName = { newName = it },
                onComment = { newComment = it }
            )
        }
    }

    val editPlaylistState = rememberMaterialDialogState()
    NewPlaylistDialog(
        dialogState = editPlaylistState,
        buttons = {
            positiveButton(res = R.string.ok)
            negativeButton(res = R.string.delete) {
                deletePlaylistState.show()
            }
            negativeButton(res = R.string.cancel)
        }
    ) {
        var editName by remember { mutableStateOf("") }
        var editComment by remember { mutableStateOf("") }
        LaunchedEffect(editPlaylistState.showing) {
            editName = selectedName
            editComment = selectedComment
        }
        DialogCallback {
            viewModel.onEvent(PlaylistEvent.Edit(editName, editComment, selectedName))
            selectedName = ""
            selectedComment = ""
            editName = ""
            editComment = ""
        }
        customView {
            NewPlaylistLayout(
                name = editName,
                comment = editComment,
                onName = { editName = it },
                onComment = { editComment = it }
            )
        }
    }

    val playlistErrorState = rememberMaterialDialogState()
    DialogMessage(
        dialogState = playlistErrorState,
        title = R.string.error,
        message = R.string.error_create_playlist,
        positiveButtonText = R.string.ok,
        onPositiveButton = { },
        onDismiss = { playlistErrorState.hide() }
    )

    val playlistDirsErrorState = rememberMaterialDialogState()
    DialogMessage(
        dialogState = playlistDirsErrorState,
        title = R.string.error,
        message = R.string.error_datadir,
        positiveButtonText = R.string.exit,
        onPositiveButton = {
            (context as Activity).finish()
        }
    )

    val storageCheckState = rememberMaterialDialogState()
    DialogMessage(
        dialogState = storageCheckState,
        title = R.string.error,
        message = R.string.error_storage,
        positiveButtonText = R.string.exit,
        onPositiveButton = { (context as Activity).finish() }
    )

    val changeLogState = rememberMaterialDialogState()
    DialogShowChangelog(changeLogState) {
        PrefManager.changelogVersion = BuildConfig.VERSION_CODE

        val name = context.getString(R.string.empty_playlist)
        val comment = context.getString(R.string.empty_comment)

        viewModel.onEvent(PlaylistEvent.Setup(name, comment))
    }

    LaunchedEffect(true) {
        if (!MainActivity.checkStorage()) {
            storageCheckState.show()
        }

        viewModel.uiState.collectLatest { event ->
            when (event) {
                is PlaylistUiEvent.CreateError -> playlistCreateState.show()
                is PlaylistUiEvent.EditCommentError -> playlistCommentState.show()
                is PlaylistUiEvent.EditRenameError -> playlistRenameState.show()
                is PlaylistUiEvent.Loading -> isLoading = event.isLoading
                is PlaylistUiEvent.SetupMkDirsError -> playlistDirsErrorState.show()
                is PlaylistUiEvent.SetupPlaylistError -> playlistErrorState.show()
            }
        }
    }

    PlaylistsContent(
        state = viewModel.state.value,
        isLoading = isLoading,
        onClick = { item ->
            navController.navigate(
                NavScreens.PlaylistSelected.route + "?plistName=${item.name}"
            )
        },
        onLongClick = { item ->
            selectedName = item.name
            selectedComment = item.comment

            editPlaylistState.show()
        },
        onFabClicked = {
            selectedName = ""
            selectedComment = ""

            newPlaylistState.show()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PlaylistsContent(
    state: PlaylistState,
    isLoading: Boolean,
    onFabClicked: () -> Unit,
    onClick: (item: PlaylistItem) -> Unit,
    onLongClick: (item: PlaylistItem) -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberLazyListState()
    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }

    Scaffold(
        topBar = {
            XmpAppBar3(
                scrollBehavior = scrollBehavior,
                title = {
                    AppBarText(
                        title = themedText(R.string.app_name),
                        titleClick = { context.startPlayerActivity() }
                    )
                },
            )
        },
        floatingActionButton = {
            PlaylistsFab(
                extended = scrollState.firstVisibleItemIndex == 0,
                onFabClicked = onFabClicked
            )
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = scrollState,
                contentPadding = rememberInsetsPaddingValues(
                    insets = LocalWindowInsets.current.navigationBars,
                    additionalTop = 10.dp,
                    additionalBottom = 80.dp
                )
            ) {
                items(state.result, key = { it.id }) { item ->
                    ItemPlaylistCard(
                        modifier = Modifier.animateItemPlacement(),
                        playlist = item,
                        onClick = { onClick(item) },
                        onLongClick = { onLongClick(item) }
                    )
                }
            }

            ProgressbarIndicator(isLoading)
        }
    }
}

/****************************
 * Utility related functions
 ****************************/
@Composable
private fun ChangeDirDialog(
    dialogState: MaterialDialogState,
    onNewPath: (value: String) -> Unit,
) {
    val buttons: @Composable MaterialDialogButtons.() -> Unit = {
        positiveButton(res = R.string.ok)
        negativeButton(res = R.string.cancel)
    }

    val currentPath = PrefManager.mediaPath
    MaterialDialog(dialogState = dialogState, buttons = buttons) {
        title(res = R.string.dialog_change_dir_title)
        message(res = R.string.dialog_change_dir_msg)
        input(label = "New Directory", prefill = currentPath.toString()) { inputString ->
            onNewPath(inputString)
        }
    }
}

@Composable
private fun NewPlaylistDialog(
    dialogState: MaterialDialogState,
    buttons: @Composable MaterialDialogButtons.() -> Unit,
    content: @Composable MaterialDialogScope.() -> Unit
) {
    MaterialDialog(
        dialogState = dialogState,
        buttons = buttons
    ) {
        content()
    }
}

private fun Context.startPlayerActivity() {
    if (PrefManager.startOnPlayer) {
        if (PlayerService.isPlayerAlive.value == true) {
            val playerIntent = Intent(this, PlayerActivity::class.java)
            launchActivity(playerIntent)
        } else {
            toast(R.string.msg_service_not_alive)
        }
    }
}

/************
 * Previews *
 ************/

@Preview(name = "Dark Theme", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PlaylistMenuPreview() {
    val state = PlaylistState(result = fakeDataPlaylistMenu())
    XmpTheme3 {
        PlaylistsContent(
            state = state,
            isLoading = true,
            onFabClicked = {},
            onClick = { },
            onLongClick = { }
        )
    }
}
