package org.helllabs.android.xmp.compose

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.compose.components.XmpCenterTopBar
import org.helllabs.android.xmp.compose.navkey.NavKeyMain
import org.helllabs.android.xmp.compose.ui.home.HomeScreen
import org.helllabs.android.xmp.compose.ui.home.PlaylistMenuViewModel
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.compose.ui.search.NavSearch
import org.helllabs.android.xmp.service.PlayerService
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
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val serviceAlive by PlayerService.isAlive.collectAsStateWithLifecycle()
    val servicePlaying by PlayerService.isPlaying.collectAsStateWithLifecycle()

    val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }

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

    val playerResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == 1) {
            result.data?.getStringExtra("error")?.let {
                Timber.w("Result with error: $it")
                scope.launch {
                    snackBarHostState.showSnackbar(message = it)
                }
            }
        }
        if (result.resultCode == 2) {
            Timber.d("Result with 2")
        }
    }

    // Long lasting VM instances.
    val playlistViewModel = koinViewModel<PlaylistMenuViewModel>()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            XmpCenterTopBar(
                onSettings = onSettings,
                onTitle = {
                    if (PlayerService.isAlive.value) {
                        Intent(context, PlayerActivity::class.java).also(playerResult::launch)
                    }
                },
                isAlive = serviceAlive,
                isPlaying = servicePlaying,
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
                sceneStrategy = dialogStrategy,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                entryProvider = entryProvider {
                    entry<NavKeyMain.Playlists> {
                        HomeScreen(
                            modifier = Modifier.consumeWindowInsets(paddingValues),
                            viewModel = playlistViewModel,
                            snackBarHostState = snackBarHostState,
                            onEditPlaylist = { item ->
                                val screen = NavKeyMain.Playlists.Edit(item)
                                mainBackStack.add(screen)
                            },
                            onNavPlaylist = {
                            },
                        )
                    }
                    entry<NavKeyMain.Playlists.Edit>(
                        metadata = DialogSceneStrategy.dialog(
                            dialogProperties = DialogProperties(
                                dismissOnClickOutside = false,
                                dismissOnBackPress = false
                            )
                        )
                    ) { key ->
                        // TODO clenaup
                        Column(
                            modifier = Modifier
                                .clip(shape = RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val item = retain(key.fileItem) { key.fileItem }
                            var name by retain(item) { mutableStateOf(item?.name) }
                            var comment by retain(item) { mutableStateOf(item?.comment) }

                            Text(
                                text = if (item == null) {
                                    "New Playlist"
                                } else {
                                    "Edit Playlist"
                                }
                            )
                            OutlinedTextField(
                                value = name ?: "",
                                onValueChange = {
                                    name = it
                                }
                            )
                            OutlinedTextField(
                                value = comment ?: "",
                                onValueChange = {
                                    comment = it
                                }
                            )
                            Button(
                                enabled = name.orEmpty().isNotEmpty(),
                                onClick = {
                                    playlistViewModel.editPlaylist(
                                        fileItem = item,
                                        name = name!!,
                                        comment = comment.orEmpty()
                                    )
                                    mainBackStack.removeLastOrNull()
                                },
                                content = { Text(text = "Save") }
                            )
                            Button(
                                onClick = {
                                    mainBackStack.removeLastOrNull()
                                },
                                content = { Text(text = "Cancel") }
                            )
                            if (item != null) {
                                Button(
                                    onClick = {
                                        playlistViewModel.deletePlaylist(item)
                                        mainBackStack.removeLastOrNull()
                                    },
                                    content = { Text(text = "Remove") }
                                )
                            }
                        }
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
