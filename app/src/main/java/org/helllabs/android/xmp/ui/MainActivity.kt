@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class
)

package org.helllabs.android.xmp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import org.helllabs.android.xmp.ui.components.PlayerBottomBar
import org.helllabs.android.xmp.ui.destinations.*
import org.helllabs.android.xmp.ui.theme.XmpAndroidTheme
import timber.log.Timber

enum class BottomBarDestination(
    val direction: DirectionDestinationSpec,
    val icon: ImageVector,
    val string: String
) {
    Playlists(PlaylistScreenDestination, Icons.Default.PlaylistPlay, "Playlists"),
    Explorer(ExplorerScreenDestination, Icons.Default.Folder, "Explorer"),
    Search(SearchScreenDestination, Icons.Default.Search, "Search"),
    Settings(SettingsScreenDestination, Icons.Default.Settings, "Settings"),
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        Timber.i("onCreate")
        setContent {
            val windowInset = Modifier
                .statusBarsPadding()
                .windowInsetsPadding(
                    WindowInsets
                        .navigationBars
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                )

            XmpAndroidTheme {
                MainActivityUI(modifier = windowInset)
            }
        }
    }
}

@Composable
private fun MainActivityUI(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState

    val navController = rememberNavController()
    val bottomBarState = rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(true) {
        // If the bottom bar is pressed, go to the player screen
        viewModel.navigateToMusicScreen.collectLatest { value ->
            if (value) {
                // TODO nav to player screen
            }
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Column {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val swipeToDismissState = rememberDismissState { dismissValue ->
                        if (dismissValue == DismissValue.DismissedToEnd ||
                            dismissValue == DismissValue.DismissedToStart
                        )
                            viewModel.onPlayerBarDismissed()
                        true
                    }

                    LaunchedEffect(viewModel.uiState.value.currentModule) {
                        swipeToDismissState.reset()
                    }

                    SwipeToDismiss(
                        state = swipeToDismissState,
                        background = {},
                        modifier = Modifier.fillMaxWidth(),
                        dismissThresholds = { FractionalThreshold(0.7f) }
                    ) {
                        AnimatedVisibility(
                            visible = uiState.isPlayerBarVisible,
                            enter = scaleIn(),
                            exit = ExitTransition.None,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp)
                        ) {
                            PlayerBottomBar(
                                modifier = Modifier.fillMaxWidth(),
                                isPlaying = uiState.isMusicPlaying,
                                name = uiState.currentModule?.title ?: "N/A",
                                type = uiState.currentModule?.type ?: "N/A",
                                onBarClick = { viewModel.onPlayerBarPressed() },
                                onPrevious = { viewModel.onPrevious() },
                                onPlayPause = {
                                    viewModel.onPlayPause(uiState.currentModule!!)
                                },
                                onForward = { viewModel.onForward() }
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = bottomBarState.value,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    content = {
                        BottomBar(
                            modifier = Modifier.navigationBarsPadding(),
                            navController = navController
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            // Show or Hide Bottom Nav Bar
            val currentDestination = navController.currentBackStackEntryAsState().value
            LaunchedEffect(currentDestination) {
                bottomBarState.value = when (currentDestination?.destination?.route) {
                    BottomBarDestination.Playlists.direction.route,
                    BottomBarDestination.Explorer.direction.route,
                    BottomBarDestination.Search.direction.route,
                    BottomBarDestination.Settings.direction.route -> true
                    else -> false
                }
            }

            DestinationsNavHost(
                modifier = Modifier,
                navController = navController,
                navGraph = NavGraphs.root
            )
        }
    }
}

@Composable
fun BottomBar(
    modifier: Modifier,
    navController: NavController
) {
    val currentDestination: Destination? =
        navController.currentBackStackEntryAsState().value?.appDestination()

    NavigationBar(modifier = modifier) {
        BottomBarDestination.values().forEach { destination ->
            NavigationBarItem(
                modifier = Modifier.clip(RoundedCornerShape(24.dp)),
                selected = currentDestination == destination.direction,
                onClick = {
                    navController.navigate(destination.direction) {
                        launchSingleTop = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.string) },
                label = { Text(destination.string) },
            )
        }
    }
}

// @Preview(name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
// @Preview(name = "Light Theme")
// @Composable
// private fun MainActivityUI_Preview() {
//     XmpAndroidTheme {
//         MainActivityUI()
//     }
// }
