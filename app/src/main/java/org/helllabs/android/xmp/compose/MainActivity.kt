package org.helllabs.android.xmp.compose

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.helllabs.android.xmp.BuildConfig
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.filelist.FileListScreenImpl
import org.helllabs.android.xmp.compose.ui.filelist.FileListViewModel
import org.helllabs.android.xmp.compose.ui.filelist.NavFileList
import org.helllabs.android.xmp.compose.ui.home.HomeScreenImpl
import org.helllabs.android.xmp.compose.ui.home.NavigationHome
import org.helllabs.android.xmp.compose.ui.home.PermissionModel
import org.helllabs.android.xmp.compose.ui.home.PermissionViewModel
import org.helllabs.android.xmp.compose.ui.home.PermissionViewModelFactory
import org.helllabs.android.xmp.compose.ui.home.PlaylistMenuViewModel
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.compose.ui.playlist.NavPlaylist
import org.helllabs.android.xmp.compose.ui.playlist.PlaylistScreenImpl
import org.helllabs.android.xmp.compose.ui.playlist.PlaylistViewModel
import org.helllabs.android.xmp.compose.ui.preferences.AboutScreen
import org.helllabs.android.xmp.compose.ui.preferences.FormatsScreen
import org.helllabs.android.xmp.compose.ui.preferences.NavPreferenceAbout
import org.helllabs.android.xmp.compose.ui.preferences.NavPreferenceFormats
import org.helllabs.android.xmp.compose.ui.preferences.NavPreferences
import org.helllabs.android.xmp.compose.ui.preferences.PreferencesScreen
import org.helllabs.android.xmp.compose.ui.search.ErrorScreen
import org.helllabs.android.xmp.compose.ui.search.NavSearch
import org.helllabs.android.xmp.compose.ui.search.NavSearchError
import org.helllabs.android.xmp.compose.ui.search.NavSearchHistory
import org.helllabs.android.xmp.compose.ui.search.SearchHistoryScreen
import org.helllabs.android.xmp.compose.ui.search.SearchScreen
import org.helllabs.android.xmp.compose.ui.search.result.ModuleResultScreenImpl
import org.helllabs.android.xmp.compose.ui.search.result.NavSearchResult
import org.helllabs.android.xmp.compose.ui.search.result.NavSearchTitleResult
import org.helllabs.android.xmp.compose.ui.search.result.ResultViewModel
import org.helllabs.android.xmp.compose.ui.search.result.ResultViewModelFactory
import org.helllabs.android.xmp.compose.ui.search.result.SearchResultViewModel
import org.helllabs.android.xmp.compose.ui.search.result.SearchResultViewModelFactory
import org.helllabs.android.xmp.compose.ui.search.result.TitleResultScreenImpl
import org.helllabs.android.xmp.core.Constants
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.service.PlayerBinder
import org.helllabs.android.xmp.service.PlayerService
import timber.log.Timber

// Compose Stability: https://multithreaded.stitchfix.com/blog/2022/08/05/jetpack-compose-recomposition/
// https://medium.com/androiddevelopers/pew-pew-making-a-game-with-compose-canvas-on-wear-os-9a37fa498d3

class MainActivity : ComponentActivity() {

    private val snackBarHostState = SnackbarHostState()

    private var mAddList: List<Uri> = listOf()
    private var mModPlayer: PlayerService? = null

    // Connection
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mModPlayer = (service as PlayerBinder).getService()

            mModPlayer!!.add(mAddList)
            mAddList = listOf()

            unbindService(this)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mModPlayer = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("onCreate")

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            val scope = rememberCoroutineScope()
            val navController = rememberNavController()

            @Suppress("InlinedApi")
            val permsViewModel = viewModel<PermissionViewModel>(
                factory = PermissionViewModelFactory(
                    listOf(
                        PermissionModel(
                            permission = Manifest.permission.POST_NOTIFICATIONS,
                            rational = "Show notification for media playback"
                        )
                    )
                )
            )
            val permsState by permsViewModel.state.collectAsStateWithLifecycle()
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = permsViewModel::onResult
            )
            LaunchedEffect(permsState.askPermission) {
                if (permsState.askPermission) {
                    permissionLauncher.launch(permsState.permissions.toTypedArray())
                }
            }
            LaunchedEffect(permsState.navigateToSetting) {
                if (permsState.navigateToSetting) {
                    val result = snackBarHostState.showSnackbar(
                        message = "${permsState.permissions.size} permission(s) were not granted",
                        duration = SnackbarDuration.Long,
                        actionLabel = "Show"
                    )
                    when (result) {
                        SnackbarResult.Dismissed -> Unit
                        SnackbarResult.ActionPerformed -> openAppSetting()
                    }
                    permsViewModel.onPermissionRequested()
                }
            }

            XmpTheme {
                NavHost(
                    navController = navController,
                    startDestination = NavigationHome,
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(500)
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(500)
                        )
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(500)
                        )
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(500)
                        )
                    }
                ) {
                    composable<NavigationHome> {
                        val viewModel = viewModel<PlaylistMenuViewModel>()
                        HomeScreenImpl(
                            viewModel = viewModel,
                            snackBarHostState = snackBarHostState,
                            onNavFileList = { navController.navigate(NavFileList) },
                            onNavPlaylist = { navController.navigate(NavPlaylist(it)) },
                            onNavPreferences = { navController.navigate(NavPreferences) },
                            onNavSearch = {
                                if (Constants.APIKEY.isEmpty()) {
                                    scope.launch {
                                        snackBarHostState.showSnackbar(
                                            message = "No API key found to use this feature",
                                            actionLabel = "Dismiss"
                                        )
                                    }
                                } else {
                                    navController.navigate(NavSearch)
                                }
                            },
                        )
                    }
                    composable<NavFileList> {
                        val viewModel = viewModel<FileListViewModel>()
                        val playerResult = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            if (result.resultCode == 1) {
                                result.data?.getStringExtra("error")?.let {
                                    Timber.w("Result with error: $it")
                                    scope.launch {
                                        snackBarHostState.showSnackbar(it)
                                    }
                                }
                            }
                            if (result.resultCode == 2) {
                                viewModel.onRefresh()
                            }
                        }

                        FileListScreenImpl(
                            viewModel = viewModel,
                            snackBarHostState = snackBarHostState,
                            onBackPressedDispatcher = onBackPressedDispatcher,
                            onBack = navController::navigateUp,
                            onPlayAll = { modList, isShuffleMode, isLoopMode ->
                                onPlayAll(
                                    modList = modList,
                                    isShuffleMode = isShuffleMode,
                                    isLoopMode = isLoopMode,
                                    result = playerResult
                                )
                            },
                            onPlayModule = { modList, start, keepFirst, isShuffleMode, isLoopMode ->
                                playModule(
                                    modList = modList,
                                    start = start,
                                    keepFirst = keepFirst,
                                    isShuffleMode = isShuffleMode,
                                    isLoopMode = isLoopMode,
                                    result = playerResult
                                )
                            },
                            onAddQueue = { list, isShuffleMode, isLoopMode ->
                                addToQueue(
                                    list = list,
                                    isShuffleMode = isShuffleMode,
                                    isLoopMode = isLoopMode,
                                    result = playerResult
                                )
                            },
                            onItemClick = { items, position, isShuffleMode, isLoopMode ->
                                onItemClick(
                                    items = items,
                                    position = position,
                                    isShuffleMode = isShuffleMode,
                                    isLoopMode = isLoopMode,
                                    result = playerResult
                                )
                            }
                        )
                    }
                    composable<NavPlaylist> {
                        val args = it.toRoute<NavPlaylist>()
                        val viewModel = viewModel<PlaylistViewModel>()
                        val playerResult = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            if (result.resultCode == RESULT_OK) {
                                val msg = result.data?.getStringExtra("message")
                                if (!msg.isNullOrEmpty()) {
                                    Timber.w("Result with error: $msg")
                                    scope.launch {
                                        snackBarHostState.showSnackbar(msg)
                                    }
                                }
                            }
                            if (result.resultCode == 2) {
                                viewModel.onRefresh(args.playlist)
                            }
                        }
                        PlaylistScreenImpl(
                            viewModel = viewModel,
                            snackBarHostState = snackBarHostState,
                            playlist = args.playlist,
                            onBack = navController::navigateUp,
                            onPlayAll = { modList, isShuffleMode, isLoopMode ->
                                onPlayAll(
                                    modList = modList,
                                    isShuffleMode = isShuffleMode,
                                    isLoopMode = isLoopMode,
                                    result = playerResult
                                )
                            },
                            onAddQueue = { list, isShuffleMode, isLoopMode ->
                                addToQueue(
                                    list = list,
                                    isShuffleMode = isShuffleMode,
                                    isLoopMode = isLoopMode,
                                    result = playerResult
                                )
                            },
                            onPlayModule = { modList, start, keepFirst, isShuffleMode, isLoopMode ->
                                playModule(
                                    modList = modList,
                                    start = start,
                                    keepFirst = keepFirst,
                                    isShuffleMode = isShuffleMode,
                                    isLoopMode = isLoopMode,
                                    result = playerResult
                                )
                            },
                            onItemClick = { items, position, isShuffleMode, isLoopMode ->
                                onItemClick(
                                    items = items,
                                    position = position,
                                    isShuffleMode = isShuffleMode,
                                    isLoopMode = isLoopMode,
                                    result = playerResult
                                )
                            },
                        )
                    }
                    /** Search and Download **/
                    composable<NavSearch> {
                        SearchScreen(
                            onBack = navController::navigateUp,
                            onSearch = { query, selection ->
                                navController.navigate(NavSearchTitleResult(query, selection))
                            },
                            onRandom = { navController.navigate(NavSearchResult(-1)) },
                            onHistory = { navController.navigate(NavSearchHistory) }
                        )
                    }
                    composable<NavSearchHistory> {
                        var historyList = remember { PrefManager.searchHistory }
                        SearchHistoryScreen(
                            historyList = historyList,
                            onBack = navController::navigateUp,
                            onClear = {
                                PrefManager.searchHistory = persistentListOf()
                                historyList = persistentListOf()
                            },
                            onClicked = {
                                navController.navigate(NavSearchResult(it))
                            }
                        )
                    }
                    composable<NavSearchError> {
                        val args = it.toRoute<NavSearchError>()

                        ErrorScreen(
                            message = args.error,
                            onBackPressedCallback = onBackPressedDispatcher,
                            onBack = { navController.popBackStack(NavSearch, false) }
                        )
                    }
                    composable<NavSearchTitleResult> {
                        val args = it.toRoute<NavSearchTitleResult>()
                        val viewModel = viewModel<SearchResultViewModel>(
                            factory = SearchResultViewModelFactory()
                        )
                        TitleResultScreenImpl(
                            viewModel = viewModel,
                            searchSelection = args.searchSelection,
                            searchQuery = args.searchQuery,
                            onBack = navController::navigateUp,
                            onClick = { id -> navController.navigate(NavSearchResult(id)) },
                            onError = { err -> navController.navigate(NavSearchError(err)) }
                        )
                    }
                    composable<NavSearchResult> {
                        val args = it.toRoute<NavSearchResult>()
                        val viewModel = viewModel<ResultViewModel>(
                            factory = ResultViewModelFactory()
                        )
                        ModuleResultScreenImpl(
                            viewModel = viewModel,
                            snackBarHostState = snackBarHostState,
                            moduleID = args.moduleID,
                            onShare = ::shareLink,
                            onBack = navController::navigateUp,
                            onError = { err -> navController.navigate(NavSearchError(err)) }
                        )
                    }
                    /** Preferences **/
                    composable<NavPreferences> {
                        PreferencesScreen(
                            snackBarHostState = snackBarHostState,
                            onBack = navController::navigateUp,
                            onFormats = { navController.navigate(NavPreferenceFormats) },
                            onAbout = { navController.navigate(NavPreferenceAbout) }
                        )
                    }
                    composable<NavPreferenceAbout> {
                        val versionName = remember { BuildConfig.VERSION_NAME }
                        val versionCode = remember { BuildConfig.VERSION_CODE }
                        val xmpVersion = remember { Xmp.getVersion() }
                        AboutScreen(
                            buildVersionName = versionName,
                            buildVersionCode = versionCode,
                            libVersion = xmpVersion,
                            onBack = navController::navigateUp
                        )
                    }
                    composable<NavPreferenceFormats> {
                        val formats = remember { Xmp.formats }
                        FormatsScreen(
                            snackBarHostState = snackBarHostState,
                            formatsList = formats,
                            onBack = navController::navigateUp
                        )
                    }
                }
            }
        }
    }

    private fun showSnack(message: String, actionLabel: String? = null) {
        lifecycleScope.launch {
            snackBarHostState.showSnackbar(message = message, actionLabel = actionLabel)
        }
    }

    /**
     * Play all `playable` modules in the current path we're in.
     */
    private fun onPlayAll(
        modList: List<Uri>,
        isShuffleMode: Boolean,
        isLoopMode: Boolean,
        result: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        lifecycleScope.launch {
            if (modList.isEmpty()) {
                showSnack(getString(R.string.error_snack_no_files_to_play))
                return@launch
            }

            playModule(
                modList = modList,
                isShuffleMode = isShuffleMode,
                isLoopMode = isLoopMode,
                result = result
            )
        }
    }

    private fun onItemClick(
        items: List<Uri>,
        position: Int,
        isShuffleMode: Boolean,
        isLoopMode: Boolean,
        result: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        fun playAllStaringAtPosition() {
            if (position < 0) {
                throw RuntimeException("Play count is negative")
            }
            playModule(
                modList = items,
                start = position,
                keepFirst = true,
                isShuffleMode = isShuffleMode,
                isLoopMode = isLoopMode,
                result = result
            )
        }

        fun playThisFile() {
            val filename = items[position]
            if (filename.path.isNullOrEmpty()) {
                showSnack("Invalid file path")
                return
            }
            if (!Xmp.testFromFd(filename)) {
                showSnack("Unrecognized file format")
                return
            }
            playModule(
                modList = listOf(filename),
                isShuffleMode = isShuffleMode,
                isLoopMode = isLoopMode,
                result = result
            )
        }

        fun addToQueue() {
            val filename = items[position]
            if (filename.path.isNullOrEmpty()) {
                showSnack("Invalid file path")
                return
            }
            if (!Xmp.testFromFd(filename)) {
                showSnack("Unrecognized file format")
                return
            }
            addToQueue(
                list = listOf(filename),
                isShuffleMode = isShuffleMode,
                isLoopMode = isLoopMode,
                result = result
            )
            showSnack("Added to queue")
        }

        /**
         * mode:
         * 1. Start playing at selection
         * 2. Play selected file
         * 3. Enqueue selected file
         */
        Timber.d("Item Clicked: ${PrefManager.playlistMode}")
        when (PrefManager.playlistMode) {
            1 -> playAllStaringAtPosition()
            2 -> playThisFile()
            3 -> addToQueue()
        }
    }

    private fun playModule(
        modList: List<Uri>,
        start: Int = 0,
        keepFirst: Boolean = false,
        isShuffleMode: Boolean,
        isLoopMode: Boolean,
        result: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        if (modList.isEmpty()) {
            showSnack("List is empty to play module(s)")
            return
        }

        XmpApplication.instance!!.fileListUri = modList
        Intent(this, PlayerActivity::class.java).apply {
            putExtra(Constants.PARM_SHUFFLE, isShuffleMode)
            putExtra(Constants.PARM_LOOP, isLoopMode)
            putExtra(Constants.PARM_START, start)
            putExtra(Constants.PARM_KEEPFIRST, keepFirst)
        }.also { intent ->
            Timber.i("Start Player activity")
            result.launch(intent)
        }
    }

    /**
     * Add a list of URI's to the queue.
     *
     * If the service alive, connect to it and append the play queue. @see [connection].
     * Else start the service normally with @see [playModule].
     */
    private fun addToQueue(
        list: List<Uri>,
        isShuffleMode: Boolean,
        isLoopMode: Boolean,
        result: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        if (list.isEmpty()) {
            showSnack("Empty uri list when adding to Queue")
            return
        }

        if (PlayerService.isAlive.value) {
            mAddList = list
            Intent(this, PlayerService::class.java).also {
                bindService(it, connection, 0)
            }
        } else {
            playModule(
                modList = list,
                isShuffleMode = isShuffleMode,
                isLoopMode = isLoopMode,
                result = result
            )
        }
    }

    private fun openAppSetting() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).also(::startActivity)
    }

    private fun shareLink(url: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/html"
        }

        Intent.createChooser(sendIntent, null).also(::startActivity)
    }
}
