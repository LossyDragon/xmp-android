package org.helllabs.android.xmp

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.*
import androidx.lifecycle.lifecycleScope
import com.meticha.permissions_compose.AppPermission
import com.meticha.permissions_compose.PermissionManagerConfig
import com.meticha.permissions_compose.rememberAppPermissionState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.helllabs.android.xmp.compose.RootNavigation
import org.helllabs.android.xmp.compose.components.MessageDialog
import org.helllabs.android.xmp.compose.components.PermissionsRationaleDialog
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.core.Constants
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.core.setEdgeToEdgeConfig
import org.helllabs.android.xmp.service.PlayerConnection
import org.helllabs.android.xmp.service.PlayerService
import org.koin.android.ext.android.inject
import timber.log.Timber

typealias PlayerActivityLauncher = ManagedActivityResultLauncher<Intent, ActivityResult>

class MainActivity : ComponentActivity() {

    private val prefManager by inject<PrefManager>()
    private val storageManager by inject<StorageManager>()
    private val playerConnection by inject<PlayerConnection>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setEdgeToEdgeConfig()
        super.onCreate(savedInstanceState)

        // Permissions
        PermissionManagerConfig.setCustomRationaleUI { permission, onDismiss, onConfirm ->
            PermissionsRationaleDialog(
                description = permission.description,
                onDismiss = onDismiss,
                onConfirm = onConfirm
            )
        }

        setContent {
            val snackBarHostState = retain { SnackbarHostState() }
            val onSnackMessage: (String) -> Unit = {
                lifecycleScope.launch {
                    snackBarHostState.showSnackbar(message = it)
                }
            }

            DisposableEffect(Unit) {
                val job = lifecycleScope.launch {
                    PlayerService.isAlive.collect { isAlive ->
                        if (isAlive) {
                            playerConnection.bindService()
                        } else {
                            playerConnection.unBindService()
                        }
                    }
                }
                onDispose {
                    job.cancel()
                    playerConnection.unBindService()
                }
            }

            // region [REGION] Permissions
            val permissionsList = remember {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    persistentListOf(
                        AppPermission(
                            permission = Manifest.permission.POST_NOTIFICATIONS,
                            description = "Post Notifications access is needed to " +
                                "display the foreground service icon",
                            isRequired = true,
                        ),
                    )
                } else {
                    persistentListOf()
                }
            }
            val permissions = rememberAppPermissionState(permissions = permissionsList)
            LaunchedEffect(Unit) {
                permissions.requestPermission()
            }
            // endregion

            // region [REGION] Initial SAF path, for explorer.
            var hasExplorerPath by remember { mutableStateOf(false) }
            val documentTreeResult = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
                onResult = { uri ->
                    if (uri == null) {
                        Timber.w("uri was null setting explorer default path")
                        return@rememberLauncherForActivityResult
                    }
                    lifecycleScope.launch {
                        storageManager.takePersistablePerms(uri)
                        prefManager.setExplorerRootPath(uri.toString())
                    }
                }
            )
            MessageDialog(
                isShowing = hasExplorerPath,
                title = "Storage Request",
                text = "Xmp Mod Player needs a default directory to browse modules.\n" +
                    "Press OK to choose an initial path. Downloads will be stored in here too.\n" +
                    "This can be changed at any time within settings.",
                confirmText = "OK",
                onConfirm = {
                    documentTreeResult.launch(null)
                    hasExplorerPath = false
                },
                onDismiss = {
                    hasExplorerPath = false
                }
            )
            LaunchedEffect(Unit) {
                if (prefManager.getExplorerRootPath().isBlank()) {
                    hasExplorerPath = true
                }
            }
            // endregion

            // region [REGION] Initial Playlist path, for playlists.
            var hasPlaylistsPath by remember { mutableStateOf(false) }
            val playlistDocumentTreeResult = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree(),
                onResult = { uri ->
                    if (uri == null) {
                        Timber.w("uri was null setting playlist default path")
                        return@rememberLauncherForActivityResult
                    }
                    lifecycleScope.launch {
                        storageManager.takePersistablePerms(uri)
                        prefManager.setPlaylistRootPath(uri.toString())
                    }
                }
            )
            MessageDialog(
                isShowing = hasPlaylistsPath,
                title = "Playlist Storage Request",
                text = "Xmp Mod Player needs a directory to store playlists.\n" +
                    "Press OK to choose a location for your playlists.\n" +
                    "This can be changed at any time within settings.",
                confirmText = "OK",
                onConfirm = {
                    val documentsUri = DocumentsContract.buildDocumentUri(
                        "com.android.externalstorage.documents",
                        "primary:Documents"
                    )
                    playlistDocumentTreeResult.launch(documentsUri)
                    hasPlaylistsPath = false
                },
                onDismiss = {
                    hasPlaylistsPath = false
                }
            )
            LaunchedEffect(Unit) {
                if (prefManager.getPlaylistRootPath().isBlank()) {
                    hasPlaylistsPath = true
                }
            }
            // endregion

            XmpTheme {
                // I'm not impressed with passing these lambas all the way to the Activity,
                //  but here we are.... We're at least 4 composables hoisted :)
                RootNavigation(
                    snackBarHostState = snackBarHostState,
                    onPlayAll = { modList, isShuffle, isLoop, result ->
                        onPlayAll(
                            modList = modList,
                            isShuffleMode = isShuffle,
                            isLoopMode = isLoop,
                            result = result,
                            onSnackMessage = onSnackMessage,
                        )
                    },
                    onAddQueue = { modList, isShuffle, isLoop, result ->
                        onAddToQueue(
                            list = modList,
                            isShuffleMode = isShuffle,
                            isLoopMode = isLoop,
                            result = result,
                            onSnackMessage = onSnackMessage,
                        )
                    },
                    onPlayModule = { modList, start, keepFirst, isShuffle, isLoop, result ->
                        onPlayModule(
                            modList = modList,
                            start = start,
                            keepFirst = keepFirst,
                            isShuffleMode = isShuffle,
                            isLoopMode = isLoop,
                            result = result,
                            onSnackMessage = onSnackMessage,
                        )
                    },
                    onItemClick = { modList, position, isShuffle, isLoop, result ->
                        onItemClick(
                            items = modList,
                            position = position,
                            isShuffleMode = isShuffle,
                            isLoopMode = isLoop,
                            result = result,
                            onSnackMessage = onSnackMessage,
                        )
                    }
                )
            }
        }
    }

    // region [REGION] Player Service stuff

    /**
     * Play all `playable` modules in the current path we're in.
     */
    private fun onPlayAll(
        modList: List<Uri>,
        isShuffleMode: Boolean,
        isLoopMode: Boolean,
        result: PlayerActivityLauncher,
        onSnackMessage: (String) -> Unit
    ) {
        if (modList.isEmpty()) {
            onSnackMessage(getString(R.string.error_snack_no_files_to_play))
            return
        }

        onPlayModule(
            modList = modList,
            isShuffleMode = isShuffleMode,
            isLoopMode = isLoopMode,
            result = result,
            onSnackMessage = onSnackMessage,
        )
    }

    private fun onItemClick(
        items: List<Uri>,
        position: Int,
        isShuffleMode: Boolean,
        isLoopMode: Boolean,
        result: PlayerActivityLauncher,
        onSnackMessage: (String) -> Unit
    ) {
        fun playAllStartingAtPosition() {
            if (position < 0) {
                throw RuntimeException("Play count is negative")
            }
            onPlayModule(
                modList = items,
                start = position,
                keepFirst = true,
                isShuffleMode = isShuffleMode,
                isLoopMode = isLoopMode,
                result = result,
                onSnackMessage = onSnackMessage,
            )
        }

        fun playThisFile() {
            val filename = items[position]
            if (filename.path.isNullOrEmpty()) {
                onSnackMessage("Invalid file path")
                return
            }
            if (!storageManager.testModule(filename)) {
                onSnackMessage("Unrecognized file format")
                return
            }
            onPlayModule(
                modList = listOf(filename),
                isShuffleMode = isShuffleMode,
                isLoopMode = isLoopMode,
                result = result,
                onSnackMessage = onSnackMessage,
            )
        }

        fun addToQueue() {
            val filename = items[position]
            if (filename.path.isNullOrEmpty()) {
                onSnackMessage("Invalid file path")
                return
            }
            if (!storageManager.testModule(filename)) {
                onSnackMessage("Unrecognized file format")
                return
            }
            onAddToQueue(
                list = listOf(filename),
                isShuffleMode = isShuffleMode,
                isLoopMode = isLoopMode,
                result = result,
                onSnackMessage = onSnackMessage,
            )
            onSnackMessage("Added to queue")
        }

        /**
         * mode:
         * 1. Start playing at selection
         * 2. Play selected file
         * 3. Enqueue selected file
         */
        val playlistMode = runBlocking { prefManager.getPlaylistMode() }
        Timber.d("Item Clicked: $playlistMode")
        when (playlistMode) {
            1 -> playAllStartingAtPosition()
            2 -> playThisFile()
            3 -> addToQueue()
        }
    }

    private fun onPlayModule(
        modList: List<Uri>,
        start: Int = 0,
        keepFirst: Boolean = false,
        isShuffleMode: Boolean,
        isLoopMode: Boolean,
        result: PlayerActivityLauncher,
        onSnackMessage: (String) -> Unit
    ) {
        if (modList.isEmpty()) {
            onSnackMessage("List is empty to play module(s)")
            return
        }

        PlayerService.fileListUri.clear()
        PlayerService.fileListUri.addAll(modList)

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
     * Else start the service normally with @see [onPlayModule].
     */
    private fun onAddToQueue(
        list: List<Uri>,
        isShuffleMode: Boolean,
        isLoopMode: Boolean,
        result: PlayerActivityLauncher,
        onSnackMessage: (String) -> Unit
    ) {
        if (list.isEmpty()) {
            onSnackMessage("Empty uri list when adding to Queue")
            return
        }

        if (PlayerService.isAlive.value) {
            playerConnection.addToQueue(list)
        } else {
            onPlayModule(
                modList = list,
                isShuffleMode = isShuffleMode,
                isLoopMode = isLoopMode,
                result = result,
                onSnackMessage = onSnackMessage,
            )
        }
    }
    // endregion
}
