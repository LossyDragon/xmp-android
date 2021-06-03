package org.helllabs.android.xmp.presentation.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import java.util.*
import org.helllabs.android.xmp.PrefManager
import org.helllabs.android.xmp.R
import org.helllabs.android.xmp.Xmp
import org.helllabs.android.xmp.XmpApplication
import org.helllabs.android.xmp.model.ModInfo
import org.helllabs.android.xmp.presentation.ui.player.PlayerActivity
import org.helllabs.android.xmp.service.PlayerService
import org.helllabs.android.xmp.util.*

abstract class BasePlaylistActivity : ComponentActivity() {

    private lateinit var mModPlayer: PlayerService
    private var mAddList: MutableList<String>? = null

    protected abstract var isShuffleMode: Boolean
    protected abstract var isLoopMode: Boolean
    protected abstract val allFiles: List<String>
    protected open fun update() {}

    private val resultPlay = registerForActivityResult(StartActivityForResult()) {
        logD("Activity Result Play Mod")
        update()
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

    public override fun onResume() {
        super.onResume()
        update()
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
        if (Xmp.testModule(filePath, ModInfo())) {
            when (PrefManager.playlistMode.toInt()) {
                // Start playing at selection
                1 -> {
                    val count = position - directoryCount
                    if (count >= 0) {
                        playModule(fileList, count, isShuffleMode)
                    }
                }
                // Play selected file
                2 -> playModule(filePath)
                // Enqueue selected file
                3 -> {
                    addToQueue(filePath)
                    toast(R.string.msg_queue_added)
                }
            }
        } else {
            toast(R.string.msg_file_unrecognized)
        }
    }

    // Play this module
    protected fun playModule(mod: String) {
        playModule(listOf(mod), 0, false)
    }

    // Play all modules in list and honor default shuffle mode
    protected fun playModule(modList: List<String>) {
        playModule(modList, 0, false)
    }

    protected fun playModule(modList: List<String>, start: Int) {
        playModule(modList, start, false)
    }

    protected fun playModule(modList: List<String>, start: Int, keepFirst: Boolean) {
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

    protected fun addToQueue(filename: String) {
        if (Xmp.testModule(filename, ModInfo())) {
            if (PlayerService.isPlayerAlive.value == true) {
                mAddList = ArrayList()
                mAddList!!.add(filename)
                val service = Intent(this, PlayerService::class.java)
                bindService(service, connection, BIND_AUTO_CREATE)
            } else {
                playModule(filename)
            }
        }
    }

    protected fun addToQueue(list: List<String>) {
        val realList = mutableListOf<String>()
        val invalid = mutableListOf<String>()

        if (list.isEmpty()) {
            toast(R.string.msg_queue_empty)
            logW("Queue list empty")
            return
        }

        list.forEach {
            if (Xmp.testModule(it, ModInfo())) {
                realList.add(it)
            } else {
                invalid.add(it)
            }
        }

        if (invalid.isNotEmpty()) {
            toast(R.string.msg_only_valid_files_sent)
            logW("addToQueue() invalid items: $invalid")
        }

        if (realList.isNotEmpty()) {
            if (PlayerService.isPlayerAlive.value == true) {
                val service = Intent(this, PlayerService::class.java)
                mAddList = realList
                bindService(service, connection, BIND_AUTO_CREATE)
            } else {
                playModule(realList)
            }
        }
    }
}
