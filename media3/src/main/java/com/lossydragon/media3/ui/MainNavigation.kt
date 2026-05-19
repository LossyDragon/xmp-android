package com.lossydragon.media3.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.*
import androidx.compose.ui.*
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.lossydragon.media3.ui.browser.FileBrowserScreenRoute
import com.lossydragon.media3.ui.downloads.NavDownloads
import com.lossydragon.media3.ui.player.PlayerScreen
import kotlinx.collections.immutable.persistentListOf

private val bottomBarItems = persistentListOf(
    NavKeyMain.Browser,
    NavKeyMain.Playlists,
    NavKeyMain.Downloads,
)

@Composable
fun MainNavigation(
    onNavigateToPlayer: () -> Unit,
    onBack: () -> Unit
) {
    val mainBackStack = rememberNavBackStack(NavKeyMain.Browser)
    var currentTab by remember { mutableStateOf<NavKeyMain>(NavKeyMain.Browser) }

    val snackBarHostState = retain { SnackbarHostState() }

    BackHandler(enabled = currentTab != NavKeyMain.Browser) {
        mainBackStack.removeAt(mainBackStack.lastIndex)
        mainBackStack.add(NavKeyMain.Browser)
        currentTab = NavKeyMain.Browser
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomBarItems.forEach { destination ->
                    NavigationBarItem(
                        selected = currentTab == destination,
                        label = { Text(destination.title) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == destination) {
                                    destination.selectedIcon
                                } else {
                                    destination.unselectedIcon
                                },
                                contentDescription = destination.title,
                            )
                        },
                        onClick = {
                            if (currentTab != destination) {
                                mainBackStack.removeAt(mainBackStack.lastIndex)
                                mainBackStack.add(destination)
                                currentTab = destination
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavDisplay(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            backStack = mainBackStack,
            onBack = { mainBackStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<NavKeyMain.Browser> {
                    FileBrowserScreenRoute(
                        modifier = Modifier.consumeWindowInsets(padding),
                        onNavigateToPlayer = onNavigateToPlayer,
                        onBack = onBack,
                    )
                }
                entry<NavKeyMain.Playlists> {
                    // TODO: SearchScreen()
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                        content = { Text("Search — coming soon") }
                    )
                }
                entry<NavKeyMain.Downloads> {
                    NavDownloads(
                        modifier = Modifier.consumeWindowInsets(padding),
                        snackbarHostState = snackBarHostState,
                        onNavigateToPlayer = onNavigateToPlayer,
                    )
                }
                entry<NavKeyMain.NowPlaying> {
                    PlayerScreen(
                        modifier = Modifier.consumeWindowInsets(padding),
                        onBack = { /* no back from now playing tab */ },
                    )
                }
            }
        )
    }
}
