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
import org.helllabs.android.xmp.ui.preferences.PreferencesScreen
import org.helllabs.android.xmp.ui.search.SearchErrorScreen
import org.helllabs.android.xmp.ui.search.SearchHistoryScreen
import org.helllabs.android.xmp.ui.search.SearchScreen
import org.helllabs.android.xmp.ui.search.result.ArtistResultScreen
import org.helllabs.android.xmp.ui.search.result.ModuleResultScreen
import org.helllabs.android.xmp.ui.search.result.SearchListResult
import org.helllabs.android.xmp.ui.theme.sectionBackground

private const val NAV_ROOT = "nav_root"
private const val NAV_PLAYLIST_ROOT = "nav_playlist_root"
private const val NAV_SEARCH_ROOT = "nav_search_root"

sealed class NavScreens(val route: String) {
    object Playlists : NavScreens("playlists")
    object PlaylistSelected : NavScreens("playlist_selected")
    object Explorer : NavScreens("explorer")
    object Search : NavScreens("search")
    object SearchError : NavScreens("search_error")
    object SearchHistory : NavScreens("search_history")
    object SearchArtistResult : NavScreens("search_artist_result")
    object SearchModuleResult : NavScreens("search_module_result")
    object SearchListResult : NavScreens("search_list_result")
    object Settings : NavScreens("settings")
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
        Triple(NavScreens.Search, R.string.nav_title_search, Icons.Filled.Search),
        Triple(NavScreens.Settings, R.string.nav_title_settings, Icons.Filled.Settings)
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
                startDestination = NavScreens.Search.route,
                route = NAV_SEARCH_ROOT
            ) {
                composable(NavScreens.Search.route) {
                    SearchScreen(navController = navController, innerPadding)
                }
                composable(NavScreens.SearchHistory.route) {
                    SearchHistoryScreen(navController = navController)
                }
                composable(
                    route = NavScreens.SearchError.route + "?errorMsg={errorMsg}",
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
                    route = NavScreens.SearchArtistResult.route + "?artistQuery={query}",
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
                    route = NavScreens.SearchModuleResult.route + "?moduleId={moduleId}",
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
                    route = NavScreens.SearchListResult.route + navArgs,
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
            composable(NavScreens.Settings.route) {
                PreferencesScreen(
                    navController = navController,
                    onBackPressedCallback = onBackPressedCallback
                )
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
        // TODO theme Nav bar
        NavigationBar(
            modifier = Modifier.navigationBarsPadding(),
            containerColor = sectionBackground
        ) {
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
            it == NavScreens.Search.route ||
            it == NavScreens.Settings.route
        ) return true
    }

    return false
}
