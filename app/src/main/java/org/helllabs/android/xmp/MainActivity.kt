package org.helllabs.android.xmp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.retain.retain
import androidx.lifecycle.lifecycleScope
import com.meticha.permissions_compose.PermissionManagerConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.helllabs.android.xmp.compose.RootNavigation
import org.helllabs.android.xmp.compose.components.PermissionsRationaleDialog
import org.helllabs.android.xmp.compose.theme.XmpTheme
import org.helllabs.android.xmp.compose.ui.player.PlayerActivity
import org.helllabs.android.xmp.core.Constants
import org.helllabs.android.xmp.core.PrefManager
import org.helllabs.android.xmp.core.StorageManager
import org.helllabs.android.xmp.core.setEdgeToEdgeConfig
import org.helllabs.android.xmp.service.PlayerBinder
import org.helllabs.android.xmp.service.PlayerService
import org.koin.android.ext.android.inject
import timber.log.Timber

typealias PlayerActivityLauncher = ManagedActivityResultLauncher<Intent, ActivityResult>

class MainActivity : ComponentActivity() {

    private val prefManager by inject<PrefManager>()
    private val storageManager by inject<StorageManager>()

    private var mAddList: List<Uri> = listOf()
    private var mModPlayer: PlayerService? = null
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
        lifecycleScope.launch {
            if (modList.isEmpty()) {
                onSnackMessage(getString(R.string.error_snack_no_files_to_play))
                return@launch
            }

            onPlayModule(
                modList = modList,
                isShuffleMode = isShuffleMode,
                isLoopMode = isLoopMode,
                result = result,
                onSnackMessage = onSnackMessage,
            )
        }
    }

    private fun onItemClick(
        items: List<Uri>,
        position: Int,
        isShuffleMode: Boolean,
        isLoopMode: Boolean,
        result: PlayerActivityLauncher,
        onSnackMessage: (String) -> Unit
    ) {
        fun playAllStaringAtPosition() {
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
            1 -> playAllStaringAtPosition()
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
            mAddList = list
            Intent(this, PlayerService::class.java).also {
                bindService(it, connection, 0)
            }
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
