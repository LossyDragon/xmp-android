package org.helllabs.android.xmp.ui

import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.navigation.navigation
import com.google.accompanist.insets.navigationBarsPadding
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.ui.explorer.ExplorerScreen
import org.helllabs.android.xmp.ui.playlist_list.PlaylistScreen
import org.helllabs.android.xmp.ui.playlist_selected.SelectedPlaylist
import org.helllabs.android.xmp.ui.preferences.*
import org.helllabs.android.xmp.ui.search.SearchErrorScreen
import org.helllabs.android.xmp.ui.search.SearchHistoryScreen
import org.helllabs.android.xmp.ui.search.SearchScreen
import org.helllabs.android.xmp.ui.search.result.ArtistResultScreen
import org.helllabs.android.xmp.ui.search.result.ModuleResultScreen
import org.helllabs.android.xmp.ui.search.result.SearchListResult

private const val NAV_ROOT = "nav_root"
private const val NAV_PLAYLIST_ROOT = "nav_playlist_root"
private const val NAV_SEARCH_ROOT = "nav_search_root"
private const val NAV_SETTINGS_ROOT = "nav_settings_root"

sealed class NavScreens(val route: String) {
    object Playlists : NavScreens("playlists")
    object PlaylistSelected : NavScreens("playlist_selected")
    object Explorer : NavScreens("explorer")
}

sealed class NavScreensSearch(val route: String) {
    object Search : NavScreens("search")
    object Error : NavScreens("search_error")
    object History : NavScreens("search_history")
    object ArtistResult : NavScreens("search_artist_result")
    object ModuleResult : NavScreens("search_module_result")
    object ListResult : NavScreens("search_list_result")
}

sealed class NavScreensSettings(val route: String) {
    object Settings : NavScreens("settings")
    object Playlist : NavScreens("settings_playlist")
    object Sound : NavScreens("settings_sound")
    object Interface : NavScreens("settings_interface")
    object Download : NavScreens("settings_download")
    object About : NavScreens("settings_about")
    object Formats : NavScreens("settings_formats")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    onBackPressedCallback: OnBackPressedDispatcher,
    bindService: () -> Unit,
) {
    val navController = rememberNavController()

    val navigationItems: List<Triple<NavScreens, Int, ImageVector>> = listOf(
        Triple(NavScreens.Playlists, R.string.nav_title_playlists, Icons.Filled.PlaylistPlay),
        Triple(NavScreens.Explorer, R.string.nav_title_explorer, Icons.Filled.FolderOpen),
        Triple(NavScreensSearch.Search, R.string.nav_title_search, Icons.Filled.Search),
        Triple(NavScreensSettings.Settings, R.string.nav_title_settings, Icons.Filled.Settings)
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController, navigationItems)
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NAV_PLAYLIST_ROOT,
            modifier = Modifier.padding(innerPadding),
            route = NAV_ROOT
        ) {
            navigation(
                startDestination = NavScreens.Playlists.route,
                route = NAV_PLAYLIST_ROOT
            ) {
                composable(NavScreens.Playlists.route) {
                    PlaylistScreen(navController = navController)
                }
                composable(
                    route = NavScreens.PlaylistSelected.route + "?plistName={plistName}",
                    arguments = listOf(
                        navArgument(name = "plistName") {
                            type = NavType.StringType
                            nullable = false
                        }
                    )
                ) {
                    SelectedPlaylist(
                        navController = navController,
                        bindService = bindService,
                    )
                }
            }
            composable(NavScreens.Explorer.route) {
                ExplorerScreen(
                    navController = navController,
                    onBackPressedCallback = onBackPressedCallback,
                    bindService = bindService,
                )
            }
            navigation(
                startDestination = NavScreensSearch.Search.route,
                route = NAV_SEARCH_ROOT
            ) {
                composable(NavScreensSearch.Search.route) {
                    SearchScreen(navController = navController, innerPadding)
                }
                composable(NavScreensSearch.History.route) {
                    SearchHistoryScreen(navController = navController)
                }
                composable(
                    route = NavScreensSearch.Error.route + "?errorMsg={errorMsg}",
                    arguments = listOf(
                        navArgument(name = "errorMsg") {
                            type = NavType.StringType
                            nullable = true
                        }
                    )
                ) {
                    val arg = it.arguments?.getString("errorMsg")
                    SearchErrorScreen(
                        navController = navController,
                        onBackPressedCallback = onBackPressedCallback,
                        errorMessage = arg
                    )
                }
                composable(
                    route = NavScreensSearch.ArtistResult.route + "?artistQuery={query}",
                    arguments = listOf(
                        navArgument(name = "query") {
                            type = NavType.StringType
                            nullable = false
                        }
                    )
                ) {
                    val arg = it.arguments!!.getString("query")!!
                    ArtistResultScreen(navController = navController, artistQuery = arg)
                }
                composable(
                    route = NavScreensSearch.ModuleResult.route + "?moduleId={moduleId}",
                    arguments = listOf(
                        navArgument(name = "moduleId") {
                            type = NavType.IntType
                            nullable = false
                        }
                    )
                ) {
                    val arg = it.arguments!!.getInt("moduleId")
                    ModuleResultScreen(navController = navController, moduleId = arg)
                }
                val navArgs = "?querySearch={querySearch}&queryArtist={queryArtist}"
                composable(
                    route = NavScreensSearch.ListResult.route + navArgs,
                    arguments = listOf(
                        navArgument(name = "querySearch") {
                            type = NavType.StringType
                            nullable = true
                        },
                        navArgument(name = "queryArtist") {
                            type = NavType.StringType
                            nullable = true
                        }
                    )
                ) {
                    SearchListResult(
                        navController = navController,
                        querySearch = it.arguments?.getString("querySearch"),
                        queryArtist = it.arguments?.getString("queryArtist")
                    )
                }
            }
            navigation(
                startDestination = NavScreensSettings.Settings.route,
                route = NAV_SETTINGS_ROOT
            ) {
                composable(NavScreensSettings.Settings.route) {
                    PreferencesScreen(
                        navController = navController,
                        onBackPressedCallback = onBackPressedCallback
                    )
                }
                composable(NavScreensSettings.Playlist.route) {
                    PreferencesPlaylistScreen(
                        navController = navController,
                        onBackPressedCallback = onBackPressedCallback
                    )
                }
                composable(NavScreensSettings.Sound.route) {
                    PreferencesSoundScreen(
                        navController = navController,
                        onBackPressedCallback = onBackPressedCallback
                    )
                }
                composable(NavScreensSettings.Interface.route) {
                    PreferencesInterfaceScreen(
                        navController = navController,
                        onBackPressedCallback = onBackPressedCallback
                    )
                }
                composable(NavScreensSettings.Download.route) {
                    PreferencesDownloadScreen(
                        navController = navController,
                        onBackPressedCallback = onBackPressedCallback
                    )
                }
                composable(NavScreensSettings.About.route) {
                    AboutScreen(
                        navController = navController,
                        onBackPressedCallback = onBackPressedCallback
                    )
                }
                composable(NavScreensSettings.Formats.route) {
                    FormatsScreen(
                        navController = navController,
                        onBackPressedCallback = onBackPressedCallback
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    navController: NavHostController,
    navigationItems: List<Triple<NavScreens, Int, ImageVector>>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    if (canShowBottomBar(navBackStackEntry)) {
        NavigationBar(modifier = Modifier.navigationBarsPadding()) {
            navigationItems.forEach { screen ->
                NavigationBarItem(
                    icon = { Icon(screen.third, null) },
                    label = { Text(stringResource(id = screen.second)) },
                    selected = currentDestination?.hierarchy?.any {
                        it.route == screen.first.route
                    } == true,
                    onClick = {
                        navController.navigate(screen.first.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }

//    AnimatedVisibility(
//        visible = canShowBottomBar(navBackStackEntry),
//        enter = slideInVertically(initialOffsetY =  { it / 2 }),
//        exit = slideOutVertically(targetOffsetY = { it / 2 })
//    ) {
//    }
}

private fun canShowBottomBar(navBackStackEntry: NavBackStackEntry?): Boolean {
    val currentDestination = navBackStackEntry?.destination?.route

    currentDestination?.let {
        if (it == NavScreens.Playlists.route ||
            it == NavScreens.Explorer.route ||
            it == NavScreensSearch.Search.route ||
            it == NavScreensSettings.Settings.route
        ) return true
    }

    return false
}
