package org.helllabs.android.xmp.compose.ui.playlist

import android.app.Activity.RESULT_OK
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.PlayerActivityLauncher
import org.helllabs.android.xmp.compose.navkey.NavKeyPlaylists
import org.helllabs.android.xmp.compose.ui.playlist.screen.PlaylistEditScreen
import org.helllabs.android.xmp.compose.ui.playlist.screen.PlaylistsScreen
import org.helllabs.android.xmp.compose.ui.playlist.screen.SelectedPlaylistScreen
import org.helllabs.android.xmp.compose.ui.playlist.viewmodel.PlaylistsViewModel
import org.helllabs.android.xmp.compose.ui.playlist.viewmodel.SelectedPlaylistViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

@Composable
fun NavPlaylists(
    modifier: Modifier,
    snackbarHostState: SnackbarHostState,
    onSettings: () -> Unit,
    onPlayAll: (List<Uri>, Boolean, Boolean, PlayerActivityLauncher) -> Unit,
    onAddQueue: (List<Uri>, Boolean, Boolean, PlayerActivityLauncher) -> Unit,
    onPlayModule: (List<Uri>, Int, Boolean, Boolean, Boolean, PlayerActivityLauncher) -> Unit,
    onItemClick: (List<Uri>, Int, Boolean, Boolean, PlayerActivityLauncher) -> Unit
) {
    val scope = rememberCoroutineScope()
    val playlistBackStack = rememberNavBackStack(NavKeyPlaylists.Playlists)
    val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }

    val playlistsViewModel = koinInject<PlaylistsViewModel>()

    BackHandler(enabled = playlistBackStack.size > 1) {
        playlistBackStack.removeLastOrNull()
    }

    NavDisplay(
        backStack = playlistBackStack,
        onBack = { playlistBackStack.removeLastOrNull() },
        sceneStrategy = dialogStrategy,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<NavKeyPlaylists.Playlists> {
                PlaylistsScreen(
                    modifier = modifier,
                    viewModel = playlistsViewModel,
                    snackBarHostState = snackbarHostState,
                    onSettings = onSettings,
                    onNavPlaylist = {
                        val screen = NavKeyPlaylists.Selected(uri = it)
                        playlistBackStack.add(screen)
                    },
                    onEditPlaylist = {
                        val screen = NavKeyPlaylists.Edit(uri = it?.uri)
                        playlistBackStack.add(screen)
                    }
                )
            }
            entry<NavKeyPlaylists.Selected> {
                val viewModel = koinViewModel<SelectedPlaylistViewModel>(
                    parameters = { parametersOf(it.uri) }
                )
                val result = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    when (result.resultCode) {
                        RESULT_OK -> result.data?.getStringExtra("message")?.let { msg ->
                            if (msg.isBlank()) {
                                return@let
                            }
                            Timber.w("Result with error: $msg")
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }

                        2 -> viewModel.onRefresh()
                    }
                }

                SelectedPlaylistScreen(
                    modifier = modifier,
                    viewModel = viewModel,
                    snackBarHostState = snackbarHostState,
                    onBack = { playlistBackStack.removeLastOrNull() },
                    onPlayAll = { modList, isShuffle, isLoop ->
                        onPlayAll(modList, isShuffle, isLoop, result)
                    },
                    onAddQueue = { list, isShuffleMode, isLoopMode ->
                        onAddQueue(list, isShuffleMode, isLoopMode, result)
                    },
                    onPlayModule = { modList, start, keepFirst, isShuffle, isLoop ->
                        onPlayModule(modList, start, keepFirst, isShuffle, isLoop, result)
                    },
                    onItemClick = { items, position, isShuffle, isLoop ->
                        onItemClick(items, position, isShuffle, isLoop, result)
                    },
                )
            }
            entry<NavKeyPlaylists.Edit>(
                metadata = DialogSceneStrategy.dialog()
            ) {
                PlaylistEditScreen(
                    uri = it.uri,
                    onBack = { result ->
                        Timber.d("Back from Playlist Edit: $result")
                        playlistBackStack.removeLastOrNull()
                        if (result) {
                            scope.launch {
                                playlistsViewModel.refreshPlaylistItems()
                            }
                        }
                    },
                    onDeleted = { result ->
                        Timber.d("Deleting Playlist: $result")
                        playlistBackStack.removeLastOrNull()
                        scope.launch {
                            playlistsViewModel.refreshPlaylistItems()
                            snackbarHostState.showSnackbar(
                                message = if (result) {
                                    "Playlist deleted"
                                } else {
                                    "Error deleting playlist"
                                },
                                actionLabel = if (result) null else "Dismiss"
                            )
                        }
                    }
                )
            }
        }
    )
}
