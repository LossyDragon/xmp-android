package org.helllabs.android.xmp.compose.ui.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import org.helllabs.android.xmp.compose.navkey.NavKeyPlaylists
import org.helllabs.android.xmp.compose.ui.playlist.screen.PlaylistsScreen
import org.helllabs.android.xmp.compose.ui.playlist.screen.SelectedPlaylistScreen
import org.helllabs.android.xmp.compose.ui.playlist.viewmodel.PlaylistsViewModel
import org.helllabs.android.xmp.compose.ui.playlist.viewmodel.SelectedPlaylistViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun NavPlaylists(
    modifier: Modifier,
    snackbarHostState: SnackbarHostState,
    onSettings: () -> Unit
) {
    val playlistBackStack = rememberNavBackStack(NavKeyPlaylists.Playlists)
    val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }

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
            val viewmodel = koinInject<PlaylistsViewModel>()
            entry<NavKeyPlaylists.Playlists> {
                PlaylistsScreen(
                    modifier = modifier,
                    viewModel = viewmodel,
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
                SelectedPlaylistScreen(
                    modifier = modifier,
                    viewModel = viewModel,
                    snackBarHostState = snackbarHostState,
                    onBack = { playlistBackStack.removeLastOrNull() },
                    onPlayAll = { _, _, _ ->
                        // TODO
                    },
                    onAddQueue = { _, _, _ ->
                        // TODO
                    },
                    onPlayModule = { _, _, _, _, _ ->
                        // TODO
                    },
                    onItemClick = { _, _, _, _ ->
                        // TODO
                    },
                )
            }
            entry<NavKeyPlaylists.Edit>(
                metadata = DialogSceneStrategy.dialog()
            ) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .clip(RoundedCornerShape(48.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Hellooooo!")
                    TextButton(
                        onClick = { playlistBackStack.removeLastOrNull() },
                        content = {
                            Text(text = "Cancel")
                        }
                    )
                }
            }
        }
    )
}
