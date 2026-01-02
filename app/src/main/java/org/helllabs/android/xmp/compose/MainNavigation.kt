package org.helllabs.android.xmp.compose

import android.app.Activity.RESULT_OK
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.*
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.PlayerActivityLauncher
import org.helllabs.android.xmp.compose.components.KoinPreview
import org.helllabs.android.xmp.compose.navkey.NavKeyMain
import org.helllabs.android.xmp.compose.ui.explorer.ExplorerScreen
import org.helllabs.android.xmp.compose.ui.explorer.ExplorerViewModel
import org.helllabs.android.xmp.compose.ui.playlist.NavPlaylists
import org.helllabs.android.xmp.compose.ui.search.NavSearch
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

val bottomBarItems = persistentListOf(
    NavKeyMain.Playlists,
    NavKeyMain.Explorer,
    NavKeyMain.Downloads
)

val BottomBarScreenSaver = Saver<NavKeyMain, String>(
    save = { it::class.simpleName ?: "Unknown" },
    restore = {
        when (it) {
            NavKeyMain.Playlists::class.simpleName -> NavKeyMain.Playlists
            NavKeyMain.Explorer::class.simpleName -> NavKeyMain.Explorer
            NavKeyMain.Downloads::class.simpleName -> NavKeyMain.Downloads
            else -> NavKeyMain.Playlists
        }
    }
)

@Composable
fun MainNavigation(
    snackBarHostState: SnackbarHostState,
    onSettings: () -> Unit,
    onPlayAll: (List<Uri>, Boolean, Boolean, PlayerActivityLauncher) -> Unit,
    onAddQueue: (List<Uri>, Boolean, Boolean, PlayerActivityLauncher) -> Unit,
    onPlayModule: (List<Uri>, Int, Boolean, Boolean, Boolean, PlayerActivityLauncher) -> Unit,
    onItemClick: (List<Uri>, Int, Boolean, Boolean, PlayerActivityLauncher) -> Unit
) {
    val scope = rememberCoroutineScope()
    val mainBackStack = rememberNavBackStack(NavKeyMain.Playlists)
    var currentBottomBarScreen by rememberSaveable(
        stateSaver = BottomBarScreenSaver,
        init = { mutableStateOf(NavKeyMain.Playlists) }
    )

    BackHandler(enabled = currentBottomBarScreen != NavKeyMain.Playlists) {
        if (mainBackStack.lastOrNull() in bottomBarItems) {
            mainBackStack.removeAt(mainBackStack.lastIndex)
        }
        mainBackStack.add(NavKeyMain.Playlists)
        currentBottomBarScreen = NavKeyMain.Playlists
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        bottomBar = {
            ShortNavigationBar {
                bottomBarItems.forEach { destination ->
                    ShortNavigationBarItem(
                        selected = currentBottomBarScreen == destination,
                        label = { Text(text = destination.title) },
                        icon = {
                            if (currentBottomBarScreen == destination) {
                                Icon(
                                    imageVector = destination.selectedIcon!!,
                                    contentDescription = null
                                )
                            } else {
                                Icon(
                                    imageVector = destination.unSelectedIcon!!,
                                    contentDescription = null
                                )
                            }
                        },
                        onClick = {
                            if (mainBackStack.lastOrNull() != destination) {
                                if (mainBackStack.lastOrNull() in bottomBarItems) {
                                    mainBackStack.removeAt(mainBackStack.lastIndex)
                                }
                                mainBackStack.add(destination)
                                currentBottomBarScreen = destination
                            }
                        }
                    )
                }
            }
        },
        content = { paddingValues ->
            NavDisplay(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                backStack = mainBackStack,
                onBack = { mainBackStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                entryProvider = entryProvider {
                    entry<NavKeyMain.Playlists> {
                        NavPlaylists(
                            modifier = Modifier.consumeWindowInsets(paddingValues),
                            snackbarHostState = snackBarHostState,
                            onSettings = onSettings,
                            onPlayAll = onPlayAll,
                            onAddQueue = onAddQueue,
                            onPlayModule = onPlayModule,
                            onItemClick = onItemClick,
                        )
                    }
                    entry<NavKeyMain.Explorer> {
                        val viewModel = koinViewModel<ExplorerViewModel>()
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
                                        snackBarHostState.showSnackbar(msg)
                                    }
                                }

                                2 -> viewModel.onRefresh()
                            }
                        }

                        ExplorerScreen(
                            modifier = Modifier.consumeWindowInsets(paddingValues),
                            viewModel = viewModel,
                            snackBarHostState = snackBarHostState,
                            onBack = {
                                if (mainBackStack.lastOrNull() in bottomBarItems) {
                                    mainBackStack.removeAt(mainBackStack.lastIndex)
                                }
                                mainBackStack.add(NavKeyMain.Playlists)
                                currentBottomBarScreen = NavKeyMain.Playlists
                            },
                            onPlayAll = { modList, isShuffle, isLoop ->
                                onPlayAll(modList, isShuffle, isLoop, result)
                            },
                            onAddQueue = { list, isShuffle, isLoop ->
                                onAddQueue(list, isShuffle, isLoop, result)
                            },
                            onPlayModule = { modList, start, keepFirst, isShuffle, isLoop ->
                                onPlayModule(modList, start, keepFirst, isShuffle, isLoop, result)
                            },
                            onItemClick = { items, position, isShuffle, isLoop ->
                                onItemClick(items, position, isShuffle, isLoop, result)
                            }
                        )
                    }
                    entry<NavKeyMain.Downloads> {
                        NavSearch(
                            modifier = Modifier.consumeWindowInsets(paddingValues),
                            snackbarHostState = snackBarHostState,
                            onBack = {
                                if (mainBackStack.lastOrNull() in bottomBarItems) {
                                    mainBackStack.removeAt(mainBackStack.lastIndex)
                                }
                                mainBackStack.add(NavKeyMain.Playlists)
                                currentBottomBarScreen = NavKeyMain.Playlists
                            }
                        )
                    }
                }
            )
        }
    )
}

@Preview
@Composable
private fun Preview() {
    KoinPreview {
        MainNavigation(
            snackBarHostState = SnackbarHostState(),
            onSettings = { },
            onPlayAll = { _, _, _, _ -> },
            onAddQueue = { _, _, _, _ -> },
            onPlayModule = { _, _, _, _, _, _ -> },
            onItemClick = { _, _, _, _, _ -> },
        )
    }
}
