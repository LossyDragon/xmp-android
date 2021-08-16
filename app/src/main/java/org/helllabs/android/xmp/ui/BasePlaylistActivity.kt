package org.helllabs.android.xmp.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.ui.player.PlayerActivity
import org.helllabs.android.xmp.ui.preferences.PrefManager
import org.helllabs.android.xmp.util.logD
import org.helllabs.android.xmp.util.logI
import org.helllabs.android.xmp.util.logW
import org.helllabs.android.xmp.util.toList
import org.helllabs.android.xmp.util.toast

abstract class BasePlaylistActivity : ComponentActivity() {

    private lateinit var mModPlayer: PlayerService
    private var mAddList: MutableList<String>? = null

    protected abstract val isShuffleMode: Boolean
    protected abstract val isLoopMode: Boolean

    private val resultPlay = registerForActivityResult(StartActivityForResult()) {
        logD("Activity Result Play Mod")
    }

    // Connection
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlayerService.PlayerBinder
            mModPlayer = binder.getService()
            mModPlayer.add(mAddList!!.toList())
            unbindService(this)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            logW("Service unexpectedly disconnected")
        }
    }

    open fun onItemClick(
        position: Int,
        filePath: String,
        directoryCount: Int,
        fileList: List<String>
    ) {
        /*
         * Test module again if invalid, in case a new file format is added to the
         * player library and the file was previously unrecognized and cached as invalid.
         */
        if (Xmp.testModule(filePath)) {
            when (PrefManager.playlistMode.toInt()) {
                // Start playing at selection
                1 -> {
                    val count = position - directoryCount
                    if (count >= 0) {
                        playModule(fileList, count, isShuffleMode)
                    }
                }
                // Play selected file
                2 -> playModule(filePath.toList())
                // Enqueue selected file
                3 -> {
                    addToQueue(filePath.toList())
                    toast(R.string.msg_queue_added)
                }
            }
        } else {
            toast(R.string.msg_file_unrecognized)
        }
    }

    protected fun playModule(
        modList: List<String>,
        start: Int = 0,
        keepFirst: Boolean = false,
    ) {
        XmpApplication.fileList = modList
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.PARM_SHUFFLE, isShuffleMode)
            putExtra(PlayerActivity.PARM_LOOP, isLoopMode)
            putExtra(PlayerActivity.PARM_START, start)
            putExtra(PlayerActivity.PARM_KEEPFIRST, keepFirst)
        }

        logI("Start Player activity")
        resultPlay.launch(intent)
    }

    protected fun addToQueue(list: List<String>) {
        val realList = mutableListOf<String>()
        val invalid = mutableListOf<String>()

        if (list.isEmpty()) {
            toast(R.string.msg_queue_empty)
            return
        }

        list.forEach {
            if (Xmp.testModule(it)) {
                realList.add(it)
            } else {
                invalid.add(it)
            }
        }

        if (invalid.isNotEmpty()) {
            toast(R.string.msg_only_valid_files_sent)
        }

        if (realList.isEmpty()) {
            logW("realist is empty when adding to queue")
            return
        }

        if (PlayerService.isPlayerAlive.value == true) {
            val service = Intent(this, PlayerService::class.java)
            mAddList = realList
            bindService(service, connection, BIND_AUTO_CREATE)
        } else {
            playModule(realList)
        }
    }
}
