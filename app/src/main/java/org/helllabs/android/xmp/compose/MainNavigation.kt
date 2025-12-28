package org.helllabs.android.xmp.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.collections.immutable.persistentListOf
import org.helllabs.android.xmp.compose.components.XmpCenterTopBar
import org.helllabs.android.xmp.compose.navkey.NavKeyMain
import org.helllabs.android.xmp.compose.ui.home.HomeScreen
import org.helllabs.android.xmp.compose.ui.home.PlaylistMenuViewModel
import org.helllabs.android.xmp.compose.ui.search.NavSearch
import org.koin.androidx.compose.koinViewModel

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
    onTextClick: () -> Unit
) {
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
        topBar = {
            XmpCenterTopBar(
                onSettings = onSettings,
                onTextClick = onTextClick,
                isAlive = false, // TODO
                isPlaying = false, // TODO
            )
        },
        bottomBar = {
            ShortNavigationBar {
                bottomBarItems.forEach { destination ->
                    ShortNavigationBarItem(
                        selected = currentBottomBarScreen == destination,
                        label = { Text(text = destination.title) },
                        icon = {
                            if (currentBottomBarScreen == destination) {
                                Icon(
                                    imageVector = destination.selectedIcon,
                                    contentDescription = null
                                )
                            } else {
                                Icon(
                                    imageVector = destination.unSelectedIcon,
                                    contentDescription = null
                                )
                            }
                        },
                        onClick = {
                            if (mainBackStack.lastOrNull() != destination) {
                                if (mainBackStack.lastOrNull() in bottomBarItems) {
                                    mainBackStack.removeAt(
                                        mainBackStack.lastIndex
                                    )
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
                        val viewModel = koinViewModel<PlaylistMenuViewModel>()
                        HomeScreen(
                            modifier = Modifier.consumeWindowInsets(paddingValues),
                            viewModel = viewModel,
                            snackBarHostState = snackBarHostState,
                            onNavFileList = {
                            },
                            onNavPlaylist = {
                            },
                        )
                    }
                    entry<NavKeyMain.Explorer> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Gray)
                                .clip(RoundedCornerShape(48.dp)),
                            content = {
                                Text(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .align(Alignment.CenterHorizontally),
                                    fontWeight = FontWeight.Bold,
                                    text = "Explorer"
                                )
                            }
                        )
                    }
                    entry<NavKeyMain.Downloads> {
                        NavSearch(
                            modifier = Modifier.consumeWindowInsets(paddingValues),
                            snackbarHostState = snackBarHostState
                        )
                    }
                }
            )
        }
    )
}
